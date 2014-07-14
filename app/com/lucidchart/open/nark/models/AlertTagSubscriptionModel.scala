package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.{AlertTagSubscription, AlertTagSubscriptionRecord, AlertType, User}
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._
import java.util.UUID
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object AlertTagSubscriptionModel extends AlertTagSubscriptionModel
trait AlertTagSubscriptionModel extends AppModel {
	protected val table = "alert_tag_subscriptions"

	protected val tagSubscriptionRowParser = RowParser { row =>
		AlertTagSubscription(
			row.uuid("user_id"),
			row.string("tag"),
			row.bool("active")
		)
	}

	/**
	 * Create a new subscription in the database
	 * throws an exception on failure
	 *
	 * @param subscription the Subscription to create
	 */
	def createSubscription(subscription: AlertTagSubscription) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				INSERT INTO """ + table + """ (`user_id`, `tag`, `active`)
				VALUES ({user_id}, {tag}, {active})
			""").on { implicit query =>
				uuid("user_id", subscription.userId)
				string("tag", subscription.tag)
				bool("active", subscription.active)
			}.executeUpdate()
		}
	}

	/**
	 * Edit a user's subscription to an alert
	 * @param subscription the new subscription values
	 */
	def editSubscription(subscription: AlertTagSubscription) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				UPDATE """ + table + """
				SET `active`= {active}
				WHERE `tag`={tag} AND `user_id`={user_id}
			""").on { implicit query =>
				string("tag", subscription.tag)
				uuid("user_id", subscription.userId)
				bool("active", subscription.active)
			}.executeUpdate()
		}
	}

	/**
	 * Delete a user's subscription to an alert
	 * @param alertId the id of the alert to delete
	 * @param userId the id of the user for whom to delete it
	 */
	def deleteSubscription(tag: String, userId: UUID) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				DELETE FROM """ + table + """
				WHERE `tag` = {tag} AND `user_id`={user_id}
			""").on { implicit query =>
				string("tag", tag)
				uuid("user_id", userId)
			}.executeUpdate()
		}
	}

	/**
	 * Get all subscriptions for a certain tag
	 * @param id the id of the Alert for which to find subscriptions
	 */
	def getSubscriptionsByTag(tag: String): List[AlertTagSubscriptionRecord] = {
		getSubscriptionsByTag(List(tag))
	}

	/**
	 * Get all subscriptions for specified tags
	 * @param id the id of the Alert for which to find subscriptions
	 */
	def getSubscriptionsByTag(tags: List[String]): List[AlertTagSubscriptionRecord] = {
		if(tags.isEmpty) {
			Nil
		}
		else {
			val subscriptions = DB.withConnection("main") { implicit connection =>
				SQL("""
					SELECT *
					FROM """ + table + """
					WHERE `tag` IN ({tags})
				""").expand { implicit query =>
					commaSeparated("tags", tags.size)
				}.on { implicit query =>
					strings("tags", tags)
				}.asList(tagSubscriptionRowParser)
			}

			if (subscriptions.size == 0) {
				Nil
			}
			else {
				val userIds = subscriptions.map(_.userId)
				val users = UserModel.findUsersByID(userIds).map { user =>
					(user.id, user)
				}.toMap

				subscriptions.map { subscription =>
					AlertTagSubscriptionRecord(subscription, users.get(subscription.userId))
				}
			}
		}
	}

	 /**
	 * Get a page of subscriptions for a certain user
	 */
	def getSubscriptionsByUser(user: User, page: Int) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM """ + table + """
				WHERE `user_id` = {user_id}
			""").on { implicit query =>
				uuid("user_id", user.id)
			}.asScalar[Long]

			val subscriptions = SQL("""
				SELECT *
				FROM """ + table + """
				WHERE `user_id` = {user_id}
				ORDER BY `tag` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				uuid("user_id", user.id)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(tagSubscriptionRowParser)
			
			if (subscriptions.size == 0) {
				(found, Nil)
			}
			else {
				val subscriptionRecords = subscriptions.map { subscription =>
					AlertTagSubscriptionRecord(subscription, Some(user))
				}
				(found, subscriptionRecords)
			}
		}
	}

	/**
	 * Get all subscriptions for a user
	 * @param user the user to look for
	 * @param includeInactive whether to include inactive subscriptions
	 * @return a list of subscriptions
	 */
	def getAllSubscriptionsByUser(user: User, includeInactive: Boolean = false): List[AlertTagSubscription] = {
		DB.withConnection("main") { implicit connection =>
			val includeClause = if (includeInactive) ""  else " AND active = TRUE"

			SQL("""
				SELECT *
				FROM """ + table + """
				WHERE `user_id` = {user_id}
			""" + includeClause).on { implicit query =>
				uuid("user_id", user.id)
			}.asList(tagSubscriptionRowParser)
		}
	}
}