package com.lucidchart.open.nark.models

import java.sql.SQLIntegrityConstraintViolationException
import java.util.Date

import com.lucidchart.open.nark.models.records.OpenIDAssociation
import com.lucidchart.open.nark.models.records.OpenIDAssociationType
import com.lucidchart.open.nark.models.records.OpenIDNonce

import AnormImplicits._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB

class OpenIDModel extends AppModel {
	protected val associationsSelectAllFields = "`provider`, `handle`, `created`, `expire`, `secret`, `type`"
	
	protected val associationsRowParser = {
		get[String]("provider") ~
		get[String]("handle") ~
		get[Date]("created") ~
		get[Date]("expire") ~
		get[Int]("type") ~
		get[Array[Byte]]("secret") map {
			case provider ~ handle ~ created ~ expires ~ typeInt ~ secret =>
				new OpenIDAssociation(provider, handle, created, expires, OpenIDAssociationType(typeInt), secret)
		}
	}
	
	/**
	 * Atomically check to see if a nonce has been seen.
	 * 
	 * @param nonce
	 * @return seen
	 */
	def seenNonce(nonce: OpenIDNonce) = {
		DB.withConnection("main") { connection =>
			try {
				SQL("""
					INSERT INTO `openid_nonces` (`provider`, `nonce`, `created`)
					VALUES ({provider}, {nonce}, {created})
				""").on(
					"provider" -> nonce.provider,
					"nonce"    -> nonce.nonce,
					"created"  -> nonce.created
				).executeUpdate()(connection)
				
				false
			}
			catch {
				case e: SQLIntegrityConstraintViolationException => true
			}
		}
	}
	
	/**
	 * Clean all the nonces that were created before the date specified.
	 * 
	 * @param date cutoff
	 * @return count of cleaned up rows
	 */
	def cleanNoncesBefore(date: Date) = {
		DB.withConnection("main") { connection =>
			SQL("""
				DELETE FROM `openid_nonces` WHERE `created` < {date}
			""").on(
				"date" -> date
			).executeUpdate()(connection)
		}
	}
	
	/**
	 * Save, or resave, an association to the database
	 */
	def saveAssociation(association: OpenIDAssociation) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO `openid_associations` (`provider`, `handle`, `created`, `expire`, `type`, `secret`)
				VALUES ({provider}, {handle}, {created}, {expire}, {type}, {secret})
				ON DUPLICATE KEY UPDATE
					`created` = {created},
					`expire` = {expire},
					`type` = {type},
					`secret` = {secret}
			""").on(
				"provider" -> association.provider,
				"handle"   -> association.handle,
				"created"  -> association.created,
				"expire"   -> association.expires,
				"type"     -> association.associationType.id,
				"secret"   -> association.secret
			).executeUpdate()(connection)
		}
	}
	
	/**
	 * Find a specific association
	 */
	def findAssociation(provider: String, handle: String) = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + associationsSelectAllFields + """
				FROM `openid_associations`
				WHERE `provider` = {provider} AND `handle` = {handle}
				LIMIT 1
			""").on(
				"provider" -> provider,
				"handle"   -> handle
			).as(associationsRowParser.singleOpt)(connection)
		}
	}
	
	/**
	 * Find the association that expires last out of the set
	 */
	def findLastAssociation(provider: String) = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + associationsSelectAllFields + """
				FROM `openid_associations`
				WHERE `provider` = {provider}
				ORDER BY `expire` DESC
				LIMIT 1
			""").on(
				"provider" -> provider
			).as(associationsRowParser.singleOpt)(connection)
		}
	}
	
	/**
	 * Remove an existing association
	 */
	def removeAssociation(provider: String, handle: String) {
		DB.withConnection("main") { connection =>
			SQL("""
				DELETE FROM `openid_associations`
				WHERE `provider` = {provider} AND `handle` = {handle}
				LIMIT 1
			""").on(
				"provider" -> provider,
				"handle"   -> handle
			).executeUpdate()(connection)
		}
	}
	
	/**
	 * Cleanup all the associations that expired before the date
	 */
	def cleanAssociationsBefore(date: Date) {
		DB.withConnection("main") { connection =>
			SQL("""
				DELETE FROM `openid_associations`
				WHERE `expire` < {date}
				LIMIT 1
			""").on(
				"date" -> date
			).executeUpdate()(connection)
		}
	}
}

object OpenIDModel extends OpenIDModel
