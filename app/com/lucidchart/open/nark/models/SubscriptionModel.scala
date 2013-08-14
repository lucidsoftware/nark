package com.lucidchart.open.nark.models

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.{Subscription, AlertType}
import java.util.UUID
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object SubscriptionModel extends SubscriptionModel
class SubscriptionModel extends AppModel {

	protected val subscriptionsRowParser = {
		get[UUID]("user_id") ~
		get[UUID]("alert_id") ~
		get[Int]("alert_type") ~
		get[Boolean]("active") map {
			case user_id ~ alert_id ~ alert_type ~ active =>
			new Subscription(user_id, alert_id, AlertType(alert_type), active)
		}
	}

	/**
	 * Create a new subscription in the database
	 * throws an exception on failure
	 *
	 * @param subscription the Subscription to create
	 */
	def createSubscription(subscription: Subscription) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO `alert_subscriptions` (`user_id`, `alert_id`, `alert_type`, `active`)
				VALUES ({user_id}, {alert_id}, {alert_type}, {active})
			""").on(
				"user_id" -> subscription.userId,
				"alert_id" -> subscription.alertId,
				"alert_type" -> subscription.alertType.id,
				"active" -> subscription.active
			).executeUpdate()(connection)
		}
	}

	/**
	 * Get all subscriptions for a certain alert
	 * @param id the id of the Alert for which to find subscriptions
	 */
	 def getSubscriptionsByAlert(id: UUID): List[Subscription] = {
	 	DB.withConnection("main") { connection =>
	 		SQL("""
	 			SELECT *
	 			FROM `alert_subscriptions` 
	 			WHERE `alert_id` = {alert_id}
	 		""").on(
	 			"alert_id" -> id
	 		).as(subscriptionsRowParser *)(connection)
	 	}
	 }

	 /**
	 * Get all subscriptions for a certain user
	 * @param id the id of the User for which to find subscriptions
	 */
	 def getSubscriptionsByUser(id: UUID): List[Subscription] = {
	 	DB.withConnection("main") { connection =>
	 		SQL("""
	 			SELECT *
	 			FROM `alert_subscriptions` 
	 			WHERE `user_id` = {user_id}
	 		""").on(
	 			"user_id" -> id
	 		).as(subscriptionsRowParser *)(connection)
	 	}
	 }
}
