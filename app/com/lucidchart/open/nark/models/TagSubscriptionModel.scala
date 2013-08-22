package com.lucidchart.open.nark.models

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.{TagSubscription, TagSubscriptionRecord, AlertType, User}
import java.util.UUID
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object TagSubscriptionModel extends TagSubscriptionModel

class TagSubscriptionModel extends AppModel {
	protected val tagSubscriptionRowParser = {
		get[UUID]("user_id") ~
		get[String]("tag") ~
		get[Boolean]("active") map {
			case user_id ~ tag ~ active =>
				new TagSubscription(user_id, tag, active)
		}
	}

		/**
	 * Create a new subscription in the database
	 * throws an exception on failure
	 *
	 * @param subscription the Subscription to create
	 */
	def createSubscription(subscription: TagSubscription) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO `alert_tag_subscriptions` (`user_id`, `tag`, `active`)
				VALUES ({user_id}, {tag}, {active})
			""").on(
				"user_id" 	-> subscription.userId,
				"tag" 		-> subscription.tag,
				"active" 	-> subscription.active
			).executeUpdate()(connection)
		}
	}

	/**
	 * Edit a user's subscription to an alert
	 * @param alertId the id of the alert to edit
	 * @param userId the id of the user for whom to edit it
	 * @param subscription the new subscription values
	 */
	def editSubscription(tag:String, userId: UUID, subscription: TagSubscription) {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE `alert_tag_subscriptions`
				SET `active`= {active}
				WHERE `tag`={tag} AND `user_id`={user_id}
			""").on(
				"tag" -> tag,
				"user_id" -> userId,
				"active" -> subscription.active
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
				DELETE FROM `alert_tag_subscriptions`
				WHERE `tag` = {tag} AND `user_id`={user_id}
			""").on(
				"tag"		-> tag,
				"user_id"	-> userId
			).executeUpdate()(connection)
		}
	}

	/**
	 * Get all subscriptions for a certain tag
	 * @param id the id of the Alert for which to find subscriptions
	 */
	def getSubscriptionsByTag(tag: String): List[TagSubscriptionRecord] = {
		getSubscriptionsByTag(List(tag))
	}
	/**
	 * Get all subscriptions for specified tags
	 * @param id the id of the Alert for which to find subscriptions
	 */
	def getSubscriptionsByTag(tags: List[String]): List[TagSubscriptionRecord] = {
		if(tags.isEmpty) {
			Nil
			
		} else {
			val subscriptions: List[TagSubscription] = DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT *
					FROM `alert_tag_subscriptions`
					WHERE `tag` IN ({tags})
				""").onList(
					"tags" -> tags
				).toSQL.as(tagSubscriptionRowParser *)(connection)
			}

			if (subscriptions.size == 0) {
				Nil
			}
			else {
				val userIds = subscriptions.map( _.userId ).distinct
				val users = UserModel.findUsersByID(userIds).map { user =>
					(user.id, user)
				}.toMap

				subscriptions.map { subscription =>
					TagSubscriptionRecord(subscription, users.get(subscription.userId))
				}
			}
		}
	}

	 /**
	 * Get all subscriptions for a certain user
	 * @param id the id of the User for which to find subscriptions
	 */
	def getSubscriptionsByUser(id: UUID, page: Int) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `alert_tag_subscriptions`
				WHERE `user_id` = {user_id}
			""").on(
				"user_id" -> id
			).as(scalar[Long].single)(connection)

			val subscriptions = SQL("""
				SELECT *
				FROM `alert_tag_subscriptions`
				WHERE `user_id` = {user_id}
				ORDER BY `tag` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"user_id" -> id,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(tagSubscriptionRowParser *)(connection)
			
			if (subscriptions.size == 0) {
				(found, Nil)
			}
			else {
				val user = UserModel.findUserByID(id).get
				(found, (subscriptions.map { subscription =>
					TagSubscriptionRecord(subscription, Some(user))
				}))
			}
		}

	}
}