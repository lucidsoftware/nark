package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.{Alert, AlertState, AlertStatus, Comparisons}
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._
import java.math.BigDecimal
import java.util.{Date, UUID}
import play.api.Logger
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB
import java.sql.Connection

object AlertModel extends AlertModel
class AlertModel extends AppModel {
	private val maxConsecutiveFailures = configuration.getInt("alerts.maxConsecutiveFailures").get
	private val consecutiveFailuresMultiplier = configuration.getInt("alerts.consecutiveFailuresMultiplier").get

	protected val alertsRowParser = RowParser { implicit row =>
		Alert(
			row.uuid("id"),
			row.string("name"),
			row.uuid("user_id"),
			row.string("target"),
			Comparisons(row.int("comparison")),
			row.uuidOption("dynamic_alert_id"),
			row.bool("active"),
			row.bool("deleted"),
			row.date("created"),
			row.date("updated"),
			row.uuidOption("thread_id"),
			row.dateOption("thread_start"),
			row.date("last_checked"),
			row.date("next_check"),
			row.int("frequency"),
			row.bigDecimal("warn_threshold"),
			row.bigDecimal("error_threshold"),
			AlertState(row.int("worst_state")),
			row.int("consecutive_failures"),
			row.int("data_seconds"),
			row.int("drop_null_points"),
			row.bool("drop_null_targets")
		)
	}

	/**
	 * Create a new alert using all the details from the alert object
	 * throws an exception on failure
	 *
	 * @param alert the Alert to create
	 */
	def createAlert(alert: Alert) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				INSERT INTO `alerts` (`id`, `name`, `user_id`, `target`, `comparison`, `dynamic_alert_id`, `active`, `deleted`, `created`, `updated`, `thread_id`, `thread_start`, `last_checked`, `next_check`, `frequency`, `warn_threshold`, `error_threshold`, `worst_state`, `consecutive_failures`, `data_seconds`, `drop_null_points`, `drop_null_targets`)
				VALUES ({id}, {name}, {user_id}, {target}, {comparison}, {dynamic_alert_id}, {active}, {deleted}, {created}, {updated}, {thread_id}, {thread_start}, {last_checked}, {next_check}, {frequency}, {warn_threshold}, {error_threshold}, {worst_state}, {consecutive_failures}, {data_seconds}, {drop_null_points}, {drop_null_targets})
			""").on { implicit query =>
				uuid("id", alert.id)
				string("name", alert.name)
				uuid("user_id", alert.userId)
				string("target", alert.target)
				int("comparison", alert.comparison.id)
				uuidOption("dynamic_alert_id", alert.dynamicAlertId)
				bool("active", alert.active)
				bool("deleted", alert.deleted)
				date("created", alert.created)
				date("updated", alert.updated)
				uuidOption("thread_id", alert.threadId)
				dateOption("thread_start", alert.threadStart)
				date("last_checked", alert.lastChecked)
				date("next_check", alert.nextCheck)
				int("frequency", alert.frequency)
				bigDecimal("warn_threshold", alert.warnThreshold)
				bigDecimal("error_threshold", alert.errorThreshold)
				int("worst_state", alert.worstState.id)
				int("consecutive_failures", alert.consecutiveFailures)
				int("data_seconds", alert.dataSeconds)
				int("drop_null_points", alert.dropNullPoints)
				bool("drop_null_targets", alert.dropNullTargets)
			}.executeUpdate()
		}
	}

	/**
	 * Get the alert specified by the uuid
	 * @param id the uuid of the alert to get
	 * @return the requested alert
	 */
	def findAlertByID(id: UUID): Option[Alert] = {
		findAlertByID(List(id)).headOption
	}

	/**
	 * Get the alerts specified by the uuids passed in
	 * @param ids the ids of the alerts to get
	 * @return the requested alerts
	 */
	def findAlertByID(ids: List[UUID]): List[Alert] = {
		if (ids.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { implicit connection =>
				SQL("""
					SELECT *
					FROM `alerts`
					WHERE `id` IN ({ids})
				""").expand { implicit query =>
					commaSeparated("ids", ids.size)
				}.on { implicit query =>
					uuids("ids", ids)
				}.asList(alertsRowParser)
			}
		}
	}

	/**
	 * Get all alerts propagated by a specific dynamic alert
	 * @param dynamicAlertId the id of the dynamic alert to look for
	 * @return the list of alerts
	 */
	def findAlertByDynamicAlert(id: UUID): List[Alert] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `alerts`
				WHERE `dynamic_alert_id`={id}
			""").on { implicit query =>
				uuid("id", id)
			}.asList(alertsRowParser)
		}
	}

	/**
	 * Search for alert records by a search term
	 * @param name the search term to search by
	 * @return a List of Alert records
	 */
	def search(name: String, page: Int) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `alerts`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
			""").on { implicit query =>
				string("name", name)
			}.asScalar[Long]

			val matches = SQL("""
				SELECT * FROM `alerts`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				string("name", name)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(alertsRowParser)

			(found, matches)
		}
	}

	/**
	 * Get all deleted alerts for a user
	 * @param userId the id of the user
	 * @param name the name of the alert to search for
	 * @param page the page of deleted alerts to return
	 * @return the number of matching alerts and a list of matching alerts
	 */
	def searchDeleted(userId: UUID, name: String, page: Int): (Long, List[Alert]) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `alerts`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
			""").on { implicit query =>
				string("name", name)
				uuid("user_id", userId)
			}.asScalar[Long]

			val matches = SQL("""
				SELECT * FROM `alerts`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				string("name", name)
				uuid("user_id", userId)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(alertsRowParser)

			(found, matches)
		}
	}

	/**
	 * Edit an alert in the database
	 * @param alert the edited values
	 */
	def editAlert(alert: Alert) = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				UPDATE `alerts`
				SET name={name}, target={target}, comparison={comparison}, active = {active}, deleted = {deleted}, updated = {updated},
					frequency={frequency}, error_threshold={error_threshold}, warn_threshold={warn_threshold}, data_seconds={data_seconds},
					drop_null_points={drop_null_points}, drop_null_targets={drop_null_targets}
				WHERE id={id}
			""").on { implicit query =>
				uuid("id", alert.id)
				string("name", alert.name)
				string("target", alert.target)
				int("comparison", alert.comparison.id)
				bool("active", alert.active)
				bool("deleted", alert.deleted)
				date("updated", alert.updated)
				int("frequency", alert.frequency)
				bigDecimal("warn_threshold", alert.warnThreshold)
				bigDecimal("error_threshold", alert.errorThreshold)
				int("data_seconds", alert.dataSeconds)
				int("drop_null_points", alert.dropNullPoints)
				bool("drop_null_targets", alert.dropNullTargets)
			}.executeUpdate()
		}
	}

	private def handleAlertErrors(threadId: UUID, alerts: List[Alert])(implicit connection: Connection) {
		SQL("""
			UPDATE `alerts`
			SET
				`consecutive_failures` = LEAST(`consecutive_failures` + 1, {max_failures}),
				`thread_id` = NULL,
				`thread_start` = NULL,
				`last_checked` = NOW(),
				`next_check` = DATE_ADD(NOW(), INTERVAL (`frequency` * `consecutive_failures` * {failure_multiplier}) SECOND)
			WHERE `id` IN ({ids}) AND `thread_id` = {thread_id}
		""").expand { implicit query =>
			commaSeparated("ids", alerts.size)
		}.on {implicit query =>
			uuids("ids", alerts.map(_.id))
			uuid("thread_id", threadId)
			int("max_failures", maxConsecutiveFailures)
			int("failure_multiplier", consecutiveFailuresMultiplier)
		}.executeUpdate()
	}

	/**
	 * Used by the alerting job to take the next alert(s) that needs to be checked, check the alert.
	 * @param threadId the thread of the worker checking the alerts
	 * @param limit the max number of alerts to take
	 * @param worker handles the alerts to be checked and returns the status (success or failure)
	 */
	def takeNextAlertsToCheck(threadId: UUID, limit: Integer)(worker: (List[Alert]) => Map[Alert, AlertStatus.Value]) = {
		val start = new Date()
		val alertsForWorker = DB.withTransaction("main") { implicit connection =>
			val selectedAlertIds = SQL("""
				SELECT `id` FROM `alerts`
				WHERE
					`active` = 1
					AND `deleted` = 0
					AND `thread_id` IS NULL
					AND `next_check` <= NOW()
				ORDER BY
					`next_check` ASC,
					`id` ASC
				LIMIT {limit}
				FOR UPDATE
			""").on { implicit query =>
				int("limit", limit)
			}.asList(RowParser { row => row.uuid("id") })

			if (selectedAlertIds.isEmpty) {
				Nil
			}
			else {
				val updated = SQL("""
					UPDATE `alerts`
					SET `thread_id` = {thread_id},
						`thread_start` = {thread_start}
					WHERE `id` IN ({ids})
				""").expand { implicit query =>
					commaSeparated("ids", selectedAlertIds.size)
				}.on { implicit query =>
					uuids("ids", selectedAlertIds)
					uuid("thread_id", threadId)
					date("thread_start", start)
				}.executeUpdate()

				SQL("""
					SELECT * FROM `alerts`
					WHERE
						`thread_id` = {thread_id}
						AND `id` IN ({ids})
				""").expand { implicit query =>
					commaSeparated("ids", selectedAlertIds.size)
				}.on { implicit query =>
					uuids("ids", selectedAlertIds)
					uuid("thread_id", threadId)
				}.asList(alertsRowParser)
			}
		}

		try {
			val completedAlertStatus = worker(alertsForWorker)
			require(completedAlertStatus.size == alertsForWorker.size)

			if (!alertsForWorker.isEmpty) {
				DB.withConnection("main"){ implicit connection =>
					completedAlertStatus.groupBy { case (alert, status) => status }.map { case (status, records) =>
						val alerts = records.map(_._1).toList
						status match {
							case AlertStatus.success => {
								alerts.groupBy { alert => alert.worstState }.map { case (worstState, sameAlerts) =>
									val sameAlertIds = sameAlerts.map(_.id)

									SQL("""
										UPDATE `alerts` 
										SET
											`consecutive_failures` = 0,
											`worst_state` = {worst_state},
											`thread_id` = NULL,
											`thread_start` = NULL,
											`last_checked` = NOW(),
											`next_check` = DATE_ADD(NOW(), INTERVAL `frequency` SECOND)
										WHERE `id` IN ({ids}) AND `thread_id` = {thread_id}
									""").expand { implicit query =>
										commaSeparated("ids", sameAlertIds.size)
									}.on { implicit query =>
										uuids("ids", sameAlertIds)
										uuid("thread_id", threadId)
										int("worst_state", worstState.id)
									}.executeUpdate()
								}
							}
							case AlertStatus.failure => {
								handleAlertErrors(threadId, alerts)
							}
						}
					}
				}
			}

			completedAlertStatus
		}
		catch {
			case e: Exception => {
				Logger.error("Error occurred while processing alerts.", e)

				if (!alertsForWorker.isEmpty) {
					DB.withConnection("main"){ connection =>
						handleAlertErrors(threadId, alertsForWorker)(connection)
					}
				}

				throw e
			}
		}
	}

	/**
	 * Clean up after broken alert threads
	 */
	def cleanAlertThreadsBefore(before: Date) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				UPDATE `alerts` 
				SET `thread_id` = NULL,
					`thread_start` = NULL
				WHERE `thread_id` IS NOT NULL
				AND `thread_start` < {date}
			""").on { implicit query =>
				date("date", before)
			}.executeUpdate()
		}
	}

	/**
	 * Find a particular alert propagated by a dynamic alert
	 * @param id the id of the dynamic alert that propagated the alert
	 * @param target the target of the alert
	 * @return optionally return the alert
	 */
	def findPropagatedAlert(id: UUID, target: String): Option[Alert] = {
		DB.withConnection("main") { implicit connection =>
			val alerts = SQL("""
				SELECT *
				FROM `alerts`
				WHERE `dynamic_alert_id`={dynamic_alert_id} AND `target`={target}
			""").on { implicit query =>
				uuid("dynamic_alert_id", id)
				string("target", target)
			}.asList(alertsRowParser)

			alerts.headOption
		}
	}

	/**
	 * Find all alerts propagated by a dynamic alert
	 * @param id the id of the dynamic alert that propagated the alerts
	 * @return the propagated alerts
	 */
	def findPropagatedAlerts(id: UUID): List[Alert] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `alerts`
				WHERE `dynamic_alert_id`={dynamic_alert_id}
			""").on { implicit query =>
				uuid("dynamic_alert_id", id)
			}.asList(alertsRowParser)
		}
	}

	/**
	 * Delete a particular alert propagated by a dynamic alert
	 * @param id the id of the dynamic alert that propagated the alert
	 * @param target the target of the alert
	 */
	def deletePropagatedAlert(id: UUID, target: String): Unit = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				DELETE FROM `alerts`
				WHERE `dynamic_alert_id`={dynamic_alert_id} AND `target`={target}
			""").on { implicit query =>
				uuid("dynamic_alert_id", id)
				string("target", target)
			}.executeUpdate()
		}
	}

	/**
	 * Delete all alerts propagated by a dynamic alert that were updated before a particular time
	 * @param id the id of the dynamic alert that propagated the alerts
	 * @param updated the updated time used as the cutoff
	 * @return the deleted alerts
	 */
	def deletePropagatedAlerts(id: UUID, updated: Date): List[Alert] = {
		DB.withConnection("main") { implicit connection =>
			val deleted = SQL("""
				SELECT * FROM `alerts`
				WHERE `dynamic_alert_id`={dynamic_alert_id} AND `updated` < {updated}
			""").on { implicit query =>
				uuid("dynamic_alert_id", id)
				date("updated", updated)
			}.asList(alertsRowParser)

			SQL("""
				DELETE FROM `alerts`
				WHERE `dynamic_alert_id`={dynamic_alert_id} AND `updated` < {updated}
			""").on { implicit query =>
				uuid("dynamic_alert_id", id)
				date("updated", updated)
			}.executeUpdate()

			deleted
		}
	}
}
