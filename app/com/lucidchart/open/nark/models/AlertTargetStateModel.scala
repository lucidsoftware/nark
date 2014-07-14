package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.Alert
import com.lucidchart.open.nark.models.records.AlertState
import com.lucidchart.open.nark.models.records.AlertTargetState
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._
import java.util.{Date, UUID}
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object AlertTargetStateModel extends AlertTargetStateModel
class AlertTargetStateModel extends AppModel {
	protected val AlertTargetStateRowParser = RowParser { row =>
		AlertTargetState(
			row.uuid("alert_id"),
			row.string("target"),
			AlertState(row.int("state")),
			row.date("last_updated")
		)
	}

	/**
	 * updates ALL existing target states for the alert
	 */
	def setAlertTargetStates(alert: Alert, states: List[AlertTargetState]) {
		DB.withTransaction("main") { implicit connection =>
			if(!states.isEmpty) {
				states.groupBy(_.state).map { case (state, records) =>
					SQL("""
						INSERT INTO `alert_target_state` (`alert_id`, `target`,`state`,`last_updated`) 
						VALUES {fields} ON DUPLICATE KEY UPDATE `state` = {update_state}, `last_updated` = VALUES(`last_updated`)
					""").expand { implicit query =>
						tupled("fields", List("alert_id", "target", "state", "last_updated"), records.size)
					}.onTuples("fields", records) { (record, query) =>
						query.uuid("alert_id", record.alertId)
						query.string("target", record.target)
						query.int("state", record.state.id)
						query.date("last_updated", record.lastUpdated)
					}.on { implicit query =>
						int("update_state", state.id)
					}.executeUpdate()
				}
			}

			//delete all old targets of this alert that have not been updated in 100 intervals
			val now = new Date
			val hundredIntervals = new Date(now.getTime - (100000 * alert.dataSeconds))
			SQL("""
				DELETE FROM `alert_target_state`
				WHERE `alert_id` = {alert_id} AND `last_updated` < {limit}
			""").on { implicit query =>
				uuid("alert_id", alert.id)
				date("limit", hundredIntervals)
			}.executeUpdate()
		}
	}

	/**
	 * Get all target states for the alert
	 * @param alertId the alert to get the target states for.
	 */
	def getTargetStatesByAlertID(alertId: UUID): List[AlertTargetState] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT * 
				FROM `alert_target_state`
				WHERE `alert_id` = {alert_id}
			""").on { implicit query =>
				uuid("alert_id", alertId)
			}.asList(AlertTargetStateRowParser)
		}
	}

	/**
	 * Get paginated results of sick targets
	 * @param alertIds the alerts for which to get sick targets
	 * @param page the page to get
	 * @return a page of sick targets
	 */
	def getSickTargets(page: Int): List[AlertTargetState] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `alert_target_state`
				WHERE `state` IN ({error}, {warn})
				ORDER BY `alert_id` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				bigDecimal("error", AlertState.error.id)
				bigDecimal("warn", AlertState.warn.id)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(AlertTargetStateRowParser)
		}
	}

	/**
	 * Get all sick targets for the alerts
	 * @param alertIds the alerts for which to get sick targets
	 * @return a list of sick targets
	 */
	def getSickTargets(alertIds: List[UUID]): List[AlertTargetState] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `alert_target_state`
				WHERE `alert_id` IN ({ids}) AND `state` IN ({error}, {warn})
			""").expand { implicit query =>
				commaSeparated("ids", alertIds.size)
			}.on { implicit query =>
				uuids("ids", alertIds)
				int("error", AlertState.error.id)
				int("warn", AlertState.warn.id)
			}.asList(AlertTargetStateRowParser)
		}
	}
}
