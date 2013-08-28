package com.lucidchart.open.nark.models

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.{AlertTagSubscription, AlertTagSubscriptionRecord, AlertType, User}
import java.util.UUID
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object AlertTagSubscriptionModel extends AlertTagSubscriptionModel
trait AlertTagSubscriptionModel extends AppModel {
	protected val table = "alert_tag_subscriptions"

	protected val tagSubscriptionRowParser = {
		get[UUID]("user_id") ~
		get[String]("tag") ~
		get[Boolean]("active") map {
			case user_id ~ tag ~ active => new AlertTagSubscription(user_id, tag, active)
		}
	}

	/**
	 * Create a new subscription in the database
	 * throws an exception on failure
	 *
	 * @param subscription the Subscription to create
	 */
	def createSubscription(subscription: AlertTagSubscription) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO """ + table + """ (`user_id`, `tag`, `active`)
				VALUES ({user_id}, {tag}, {active})
			""").on(
				"user_id"   -> subscription.userId,
				"tag"       -> subscription.tag,
				"active"    -> subscription.active
			).executeUpdate()(connection)
		}
	}

	/**
	 * Edit a user's subscription to an alert
	 * @param subscription the new subscription values
	 */
	def editSubscription(subscription: AlertTagSubscription) {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE """ + table + """
				SET `active`= {active}
				WHERE `tag`={tag} AND `user_id`={user_id}
			""").on(
				"tag"     -> subscription.tag,
				"user_id" -> subscription.userId,
				"active"  -> subscription.active
			).executeUpdate()(connection)
		}
	}

	/**
	 * Delete a user's subscription to an alert
	 * @param alertId the id of the alert to delete
	 * @param userId the id of the user for whom to delete it
	 */
	def deleteSubscription(tag: String, userId: UUID) {
		DB.withConnection("main") { connection =>
			SQL("""
				DELETE FROM """ + table + """
				WHERE `tag` = {tag} AND `user_id`={user_id}
			""").on(
				"tag"      -> tag,
				"user_id"  -> userId
			).executeUpdate()(connection)
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
			val subscriptions = DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT *
					FROM """ + table + """
					WHERE `tag` IN ({tags})
				""").onList(
					"tags" -> tags
				).toSQL.as(tagSubscriptionRowParser *)(connection)
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
	 * Get all subscriptions for a certain user
	 */
	def getSubscriptionsByUser(user: User, page: Int) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM """ + table + """
				WHERE `user_id` = {user_id}
			""").on(
				"user_id" -> user.id
			).as(scalar[Long].single)(connection)

			val subscriptions = SQL("""
				SELECT *
				FROM """ + table + """
				WHERE `user_id` = {user_id}
				ORDER BY `tag` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"user_id" -> user.id,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(tagSubscriptionRowParser *)(connection)
			
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
}