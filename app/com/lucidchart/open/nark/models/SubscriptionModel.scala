package com.lucidchart.open.nark.models

import anorm._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.Subscription
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object SubscriptionModel extends SubscriptionModel
class SubscriptionModel extends AppModel {

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

}
