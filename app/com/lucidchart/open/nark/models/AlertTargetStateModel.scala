package com.lucidchart.open.nark.models

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.Alert
import com.lucidchart.open.nark.models.records.AlertState
import com.lucidchart.open.nark.models.records.AlertTargetState
import java.util.{Date, UUID}
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object AlertTargetStateModel extends AlertTargetStateModel
class AlertTargetStateModel extends AppModel {
	protected val AlertTargetStateRowParser = {
		get[UUID]("alert_id") ~ 
		get[String]("target") ~
		get[Int]("state") ~
		get[Date]("last_updated") map {
			case alert_id ~ target ~ state ~ last_updated =>
				new AlertTargetState(alert_id, target, AlertState(state), last_updated)
		}
	}

	/**
	 * updates ALL existing target states for the alert
	 */
	def setAlertTargetStates(alert: Alert, states: List[AlertTargetState]) {
		DB.withTransaction("main") { connection =>
			if(!states.isEmpty) {
				states.groupBy(_.state).map { case (state, records) =>
					RichSQL("""
						INSERT INTO `alert_target_state` (`alert_id`, `target`,`state`,`last_updated`) 
						VALUES ({fields}) ON DUPLICATE KEY UPDATE `state` = {update_state}, `last_updated` = VALUES(`last_updated`)
					""").multiInsert(records.size, Seq("alert_id", "target", "state", "last_updated"))(
						"alert_id"      -> records.map(s => toParameterValue(s.alertId)),
						"target"        -> records.map(s => toParameterValue(s.target)),
						"state"         -> records.map(s => toParameterValue(s.state.id)),
						"last_updated"  -> records.map(s => toParameterValue(s.lastUpdated)))
					.toSQL.on(
						"update_state" -> state.id
					).executeUpdate()(connection)
				}
			}

			//delete all old targets of this alert that have not been updated in 100 intervals
			val now = new Date
			val hundredIntervals = new Date(now.getTime - (100000 * alert.dataSeconds))
			SQL("""
				DELETE FROM `alert_target_state`
				WHERE `alert_id` = {alert_id} AND `last_updated` < {limit}
			""").on(
				"alert_id" -> alert.id,
				"limit" -> hundredIntervals
			).executeUpdate()(connection)
		}
	}

	/**
	 * Get all target states for the alert
	 * @param alertId the alert to get the target states for.
	 */
	def getTargetStatesByAlertID(alertId: UUID): List[AlertTargetState] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT * 
				FROM `alert_target_state`
				WHERE `alert_id` = {alert_id}
			""").on(
				"alert_id" -> alertId
			).as(AlertTargetStateRowParser * )(connection)
		}
	}
}
