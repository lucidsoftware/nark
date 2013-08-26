package com.lucidchart.open.nark.models

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
		get[Int]("messages_sent") map {
			case alert_id ~ target ~ date ~ state ~ messages_sent =>
				new AlertHistory(alert_id, target, date, AlertState(state), messages_sent)
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
					INSERT INTO `alert_history` (`alert_id`, `target`, `date`, `state`, `messages_sent`)
					VALUES ({fields})
				""").multiInsert(alerts.size, Seq(
					"alert_id",
					"target",
					"date",
					"state",
					"messages_sent"
				))(
					"alert_id"      -> alerts.map(alert => toParameterValue(alert.alertId)),
					"target"        -> alerts.map(alert => toParameterValue(alert.target)),
					"date"          -> alerts.map(alert => toParameterValue(alert.date)),
					"state"         -> alerts.map(alert => toParameterValue(alert.state.id)),
					"messages_sent" -> alerts.map(alert => toParameterValue(alert.messagesSent))
				).toSQL.executeUpdate()(connection)
			}
		}
	}
}