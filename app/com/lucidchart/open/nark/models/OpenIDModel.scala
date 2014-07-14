package com.lucidchart.open.nark.models

import java.sql.SQLIntegrityConstraintViolationException
import java.util.Date

import com.lucidchart.open.nark.models.records.OpenIDAssociation
import com.lucidchart.open.nark.models.records.OpenIDAssociationType
import com.lucidchart.open.nark.models.records.OpenIDNonce
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._

import play.api.Play.current
import play.api.db.DB

class OpenIDModel extends AppModel {
	protected val associationsRowParser = RowParser { row =>
		OpenIDAssociation(
			row.string("provider"),
			row.string("handle"),
			row.date("created"),
			row.date("expire"),
			OpenIDAssociationType(row.int("type")),
			row.byteArray("secret")
		)
	}
	
	/**
	 * Atomically check to see if a nonce has been seen.
	 * 
	 * @param nonce
	 * @return seen
	 */
	def seenNonce(nonce: OpenIDNonce) = {
		DB.withConnection("main") { implicit connection =>
			try {
				SQL("""
					INSERT INTO `openid_nonces` (`provider`, `nonce`, `created`)
					VALUES ({provider}, {nonce}, {created})
				""").on { implicit query =>
					string("provider", nonce.provider)
					string("nonce", nonce.nonce)
					date("created", nonce.created)
				}.executeUpdate()
				
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
	def cleanNoncesBefore(before: Date) = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				DELETE FROM `openid_nonces` WHERE `created` < {date}
			""").on { implicit query =>
				date("date", before)
			}.executeUpdate()
		}
	}
	
	/**
	 * Save, or resave, an association to the database
	 */
	def saveAssociation(association: OpenIDAssociation) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				INSERT INTO `openid_associations` (`provider`, `handle`, `created`, `expire`, `type`, `secret`)
				VALUES ({provider}, {handle}, {created}, {expire}, {type}, {secret})
				ON DUPLICATE KEY UPDATE
					`created` = {created},
					`expire` = {expire},
					`type` = {type},
					`secret` = {secret}
			""").on { implicit query =>
				string("provider", association.provider)
				string("handle", association.handle)
				date("created", association.created)
				date("expire", association.expires)
				int("type", association.associationType.id)
				byteArray("secret", association.secret)
			}.executeUpdate()
		}
	}
	
	/**
	 * Find a specific association
	 */
	def findAssociation(provider: String, handle: String) = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `openid_associations`
				WHERE `provider` = {provider} AND `handle` = {handle}
				LIMIT 1
			""").on { implicit query =>
				string("provider", provider)
				string("handle", handle)
			}.asSingleOption(associationsRowParser)
		}
	}
	
	/**
	 * Find the association that expires last out of the set
	 */
	def findLastAssociation(provider: String) = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `openid_associations`
				WHERE `provider` = {provider}
				ORDER BY `expire` DESC
				LIMIT 1
			""").on { implicit query =>
				string("provider", provider)
			}.asSingleOption(associationsRowParser)
		}
	}
	
	/**
	 * Remove an existing association
	 */
	def removeAssociation(provider: String, handle: String) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				DELETE FROM `openid_associations`
				WHERE `provider` = {provider} AND `handle` = {handle}
				LIMIT 1
			""").on { implicit query =>
				string("provider", provider)
				string("handle", handle)
			}.executeUpdate()
		}
	}
	
	/**
	 * Cleanup all the associations that expired before the date
	 */
	def cleanAssociationsBefore(before: Date) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				DELETE FROM `openid_associations`
				WHERE `expire` < {date}
				LIMIT 1
			""").on { implicit query =>
				date("date", before)
			}.executeUpdate()
		}
	}
}

object OpenIDModel extends OpenIDModel
