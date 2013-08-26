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
	 * @param alert the Alert to create
	 */
	def createAlertHistory(alert: AlertHistory) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO `alert_history` (`alert_id`, `target`, `date`, `state`, `messages_sent`)
				VALUES ({alert_id}, {target}, {date}, {state}, {messages_sent})
			""").on(
				"alert_id"      -> alert.alertId,
				"target"        -> alert.target,
				"date"          -> alert.date,
				"state"         -> alert.state.id,
				"messages_sent" -> alert.messagesSent
			).executeUpdate()(connection)
		}
	}
}