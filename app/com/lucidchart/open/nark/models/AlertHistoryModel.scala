package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.AlertState
import com.lucidchart.open.nark.models.records.AlertHistory
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._
import java.util.{Date, UUID}
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object AlertHistoryModel extends AlertHistoryModel
class AlertHistoryModel extends AppModel {
	protected val alertHistoryRowParser = RowParser { implicit row =>
		AlertHistory(
			row.uuid("alert_id"), 
			row.string("target"),
			row.date("date"),
			AlertState(row.int("state")),
			row.int("messages_sent")
		)
	}

	/**
	 * Create a new alert using all the details from the alert object
	 * throws an exception on failure
	 *
	 * @param alerts the Alert Histories to create
	 */
	def createAlertHistory(alerts: List[AlertHistory]) {
		if (!alerts.isEmpty) {
			DB.withConnection("main") { implicit connection =>
				SQL("""
					INSERT INTO `alert_history` (`alert_id`, `target`, `date`, `state`, `messages_sent`)
					VALUES {fields}
				""").expand { implicit query =>
					tupled("fields", List("alert_id", "target", "date", "state", "messages_sent"), alerts.size)
				}.onTuples("fields", alerts) { (alert, query) =>
					query.uuid("alert_id", alert.alertId)
					query.string("target", alert.target)
					query.date("date", alert.date)
					query.int("state", alert.state.id)
					query.int("messages_sent", alert.messagesSent)
				}.executeUpdate()
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
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `alert_history`
				WHERE `alert_id`={alert_id}
			""").on { implicit query =>
				uuid("alert_id", alertId)
			}.asScalar[Long]

			val matches = SQL("""
				SELECT *
				FROM `alert_history`
				WHERE `alert_id`={alert_id}
				ORDER BY `date` DESC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				uuid("alert_id", alertId)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(alertHistoryRowParser)

			(found, matches)
		}
	}
}