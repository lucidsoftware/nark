package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.{HasId, Subscription, SubscriptionRecord, AlertType, User}
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._
import java.util.UUID
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object SubscriptionModel extends SubscriptionModel
class SubscriptionModel extends AppModel {
	protected val subscriptionsRowParser = RowParser { row =>
		Subscription(
			row.uuid("user_id"),
			row.uuid("alert_id"),
			AlertType(row.int("alert_type")),
			row.bool("active")
		)
	}

	/**
	 * Create a new subscription in the database
	 * throws an exception on failure
	 *
	 * @param subscription the Subscription to create
	 */
	def createSubscription(subscription: Subscription) {
		createSubscriptions(List(subscription))
	}

	/**
	 * Create multiple new subscriptions in the database
	 * @param subscriptions the subscriptions to create
	 */
	def createSubscriptions(subscriptions: List[Subscription]): Unit = {
		if (subscriptions.size > 0) {
			DB.withConnection("main") { implicit connection =>
				SQL("""
					INSERT IGNORE INTO `alert_subscriptions` (`user_id`, `alert_id`, `alert_type`, `active`)
					VALUES {fields}
				""").expand { implicit query =>
					tupled("fields", List("user_id", "alert_id", "alert_type", "active"), subscriptions.size)
				}.onTuples("fields", subscriptions) { (subscription, query) =>
					query.uuid("user_id", subscription.userId)
					query.uuid("alert_id", subscription.alertId)
					query.int("alert_type", subscription.alertType.id)
					query.bool("active", subscription.active)
				}.executeUpdate()
			}
		}
	}

	/**
	 * Edit a user's subscription to an alert
	 * @param alertId the id of the alert to edit
	 * @param userId the id of the user for whom to edit it
	 * @param subscription the new subscription values
	 */
	def editSubscription(alertId: UUID, userId: UUID, subscription: Subscription) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				UPDATE `alert_subscriptions`
				SET `active`= {active}
				WHERE `alert_id`={alert_id} AND `user_id`={user_id}
			""").on { implicit query =>
				uuid("alert_id", alertId)
				uuid("user_id", userId)
				bool("active", subscription.active)
			}.executeUpdate()(connection)
		}
	}

	/**
	 * Delete a user's subscription to an alert
	 * @param alertId the id of the alert to delete
	 * @param userId the id of the user for whom to delete it
	 */
	def deleteSubscription(alertId: UUID, userId: UUID) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				DELETE FROM `alert_subscriptions`
				WHERE `alert_id`={alert_id} AND `user_id`={user_id}
			""").on { implicit query =>
				uuid("alert_id", alertId)
				uuid("user_id", userId)
			}.executeUpdate()
		}
	}

	/**
	 * Delete all subscriptions to an alert
	 * @param alertId the id of the alert to delete
	 */
	def deleteSubscriptionsByAlert(alertId: UUID) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				DELETE FROM `alert_subscriptions`
				WHERE `alert_id`={alert_id}
			""").on { implicit query =>
				uuid("alert_id", alertId)
			}.executeUpdate()
		}
	}

	/**
	 * Delete all subscriptions to a list of alerts
	 * @param alertIds the ids of the alerts to delete
	 */
	def deleteSubscriptionsByAlert(alertIds: List[UUID]) {
		if (alertIds.size > 0) {
			DB.withConnection("main") { implicit connection =>
				SQL("""
					DELETE FROM `alert_subscriptions`
					WHERE `alert_id` IN ({alert_ids})
				""").expand { implicit query =>
					commaSeparated("alert_ids", alertIds.size)
				}.on { implicit query =>
					uuids("alert_ids", alertIds)
				}.executeUpdate()
			}
		}
	}

	def deleteSubscriptions(alertId: UUID, userIds: List[UUID]) = {
		if (userIds.size > 0) {
			DB.withConnection("main") { implicit connection =>
				SQL("""
					DELETE FROM `alert_subscriptions`
					WHERE `alert_id` = {alert_id} AND `user_id` IN ({user_ids}) 
				""").expand { implicit query =>
					commaSeparated("user_ids", userIds.size)
				}.on { implicit query =>
					uuids("user_ids", userIds)
					uuid("alert_id", alertId)
				}.executeUpdate()
			}
		}
	}

	/**
	 * Get all subscriptions for a certain alert
	 * @param id the id of the Alert for which to find subscriptions
	 */
	def getSubscriptionsByAlert(id: UUID) = {
		getSubscriptionsByAlerts(List(id))
	}
	/**
	 * Get all subscriptions for specified alerts
	 * @param id the id of the Alert for which to find subscriptions
	 */
	def getSubscriptionsByAlerts(ids: List[UUID]) = {
		if(ids.isEmpty) {
			Nil
		}
		else {
			val subscriptions: List[Subscription] = DB.withConnection("main") { implicit connection =>
				SQL("""
					SELECT *
					FROM `alert_subscriptions`
					WHERE `alert_id` IN ({alert_ids})
				""").expand { implicit query =>
					commaSeparated("alert_ids", ids.size)
				}.on { implicit query =>
					uuids("alert_ids", ids)
				}.asList(subscriptionsRowParser)
			}

			if (subscriptions.size == 0) {
				Nil
			}
			else {
				val users = UserModel.findUsersByID(subscriptions.map(_.userId)).map { user => (user.id, user) }.toMap
				val alerts = AlertModel.findAlertByID(ids).map { alert => (alert.id, alert) }.toMap
				subscriptions.map { subscription =>
					SubscriptionRecord(
						subscription,
						alerts.get(subscription.alertId),
						users.get(subscription.userId)
					)
				}
			}
		}
	}

	 /**
	 * Get all subscriptions for a certain user
	 * @param id the id of the User for which to find subscriptions
	 * @param page the page of subscriptions to retur
	 * @param alertType the type of alert subscription to look for
	 */
	def getSubscriptionsByUser[T <: HasId](user: User, page: Int, alertType: AlertType.Value): (Long, List[SubscriptionRecord[T]]) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `alert_subscriptions`
				WHERE `user_id` = {user_id} AND `alert_type`={alert_type}
			""").on { implicit query =>
				uuid("user_id", user.id)
				int("alert_type", alertType.id)
			}.asScalar[Long]

			val subscriptions = SQL("""
				SELECT *
				FROM `alert_subscriptions`
				WHERE `user_id` = {user_id} AND `alert_type`={alert_type}
				ORDER BY `alert_id` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				uuid("user_id", user.id)
				int("alert_type", alertType.id)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(subscriptionsRowParser)

			if (subscriptions.size == 0) {
				(found, Nil)
			}
			else {
				val alertIds = subscriptions.map(_.alertId)
				val alerts = if (alertType == AlertType.alert) {
					AlertModel.findAlertByID(alertIds).map { alert => (alert.id, alert) }.toMap
				}
				else {
					DynamicAlertModel.findDynamicAlertByID(alertIds).map { alert => (alert.id, alert) }.toMap
				}
				val subscriptionRecords = subscriptions.map { subscription =>
					SubscriptionRecord[T](
						subscription,
						alerts.get(subscription.alertId).asInstanceOf[Option[T]],
						Some(user)
					)
				}
				(found, subscriptionRecords)
			}
		}
	}

	/**
	 * Get all of a user's alert subscriptions
	 * @param user the user to look for
	 * @param includeInactive whether to include inactive records
	 * @return a list of all user subscriptions
	 */
	def getAllSubscriptionsByUser[T <: HasId](user: User, includeInactive: Boolean = false): List[Subscription] = {
		DB.withConnection("main") { implicit connection =>
			val includeClause = if (includeInactive) "" else " AND active = TRUE"

			SQL("""
				SELECT *
				FROM `alert_subscriptions`
				WHERE `user_id` = {user_id}
			""" + includeClause).on { implicit query =>
				uuid("user_id", user.id)
			}.asList(subscriptionsRowParser)
		}
	}
}
