package com.lucidchart.open.nark.models
import java.math.BigDecimal
import anorm._
import anorm.SqlParser._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.AlertState
import com.lucidchart.open.nark.models.records.AlertHistory
import java.util.{Date, UUID}
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object AlertHistoryModel extends AlertHistoryModel
class AlertHistoryModel extends AppModel {
	protected val alertHistoryRowParser = {
		get[UUID]("alert_id") ~ 
		get[String]("target") ~
		get[Date]("date") ~
		get[Int]("state") ~
		get[Int]("messages_sent") ~
		get[Option[BigDecimal]]("alert_value") map {
			case alert_id ~ target ~ date ~ state ~ messages_sent ~ alert_value =>
				new AlertHistory(alert_id, target, date, AlertState(state), messages_sent, alert_value.map(scala.math.BigDecimal(_)))
		}
	}

	/**
	 * Create a new alert using all the details from the alert object
	 * throws an exception on failure
	 *
	 * @param alerts the Alert Histories to create
	 */
	def createAlertHistory(alerts: List[AlertHistory]) {
		if (!alerts.isEmpty) {
			DB.withConnection("main") { connection =>
				RichSQL("""
					INSERT INTO `alert_history` (`alert_id`, `target`, `date`, `state`, `messages_sent`, `alert_value`)
					VALUES ({fields})
				""").multiInsert(alerts.size, Seq(
					"alert_id",
					"target",
					"date",
					"state",
					"messages_sent",
					"alert_value"
				))(
					"alert_id"      -> alerts.map(alert => toParameterValue(alert.alertId)),
					"target"        -> alerts.map(alert => toParameterValue(alert.target)),
					"date"          -> alerts.map(alert => toParameterValue(alert.date)),
					"state"         -> alerts.map(alert => toParameterValue(alert.state.id)),
					"messages_sent" -> alerts.map(alert => toParameterValue(alert.messagesSent)),
					"alert_value" 	-> alerts.map(alert => toParameterValue(alert.alertValue))
				).toSQL.executeUpdate()(connection)
			}
		}
	}

	/**
	 * Get the alert history for a particular alert
	 *
	 * @param alertId the id of the alert to look up
	 * @param page the page of alert histories to get
	 * @return a list of alert histories
	 */
	def getAlertHistory(alertId: UUID, page: Int): (Long, List[AlertHistory]) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `alert_history`
				WHERE `alert_id`={alert_id}
			""").on(
				"alert_id" -> alertId
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT *
				FROM `alert_history`
				WHERE `alert_id`={alert_id}
				ORDER BY `date` DESC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"alert_id" -> alertId,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(alertHistoryRowParser *)(connection)

			(found, matches)
		}
	}
}