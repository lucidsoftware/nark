package com.lucidchart.open.nark.models

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.{Alert, AlertState, AlertStatus, Comparisons}
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

	protected val alertsRowParser = {
		get[UUID]("id") ~ 
		get[String]("name") ~
		get[UUID]("user_id") ~
		get[String]("target") ~
		get[Int]("comparison") ~
		get[Option[UUID]]("dynamic_alert_id") ~
		get[Boolean]("active") ~
		get[Boolean]("deleted") ~
		get[Date]("created") ~
		get[Date]("updated") ~
		get[Option[UUID]]("thread_id") ~
		get[Option[Date]]("thread_start") ~
		get[Date]("last_checked") ~
		get[Date]("next_check") ~
		get[Int]("frequency") ~
		get[BigDecimal]("warn_threshold") ~
		get[BigDecimal]("error_threshold") ~
		get[Int]("worst_state") ~
		get[Int]("consecutive_failures") map {
			case id ~ name ~ user_id ~ target ~ comparison ~ dynamic_alert_id ~ active ~ deleted ~ created ~ updated ~ thread_id ~ thread_start ~ last_checked ~ next_check ~ frequency ~ warn_threshold ~ error_threshold ~ worst_state ~ consecutive_failures =>
				new Alert(id, name, user_id, target, Comparisons(comparison), dynamic_alert_id, active, deleted, created, updated, thread_id, thread_start, last_checked, next_check, frequency, warn_threshold, error_threshold, AlertState(worst_state), consecutive_failures)
		}
	}

	/**
	 * Create a new alert using all the details from the alert object
	 * throws an exception on failure
	 *
	 * @param alert the Alert to create
	 */
	def createAlert(alert: Alert) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO `alerts` (`id`, `name`, `user_id`, `target`, `comparison`, `dynamic_alert_id`, `active`, `deleted`, `created`, `updated`, `thread_id`, `thread_start`, `last_checked`, `next_check`, `frequency`, `warn_threshold`, `error_threshold`, `worst_state`, `consecutive_failures`)
				VALUES ({id}, {name}, {user_id}, {target}, {comparison}, {dynamic_alert_id}, {active}, {deleted}, {created}, {updated}, {thread_id}, {thread_start}, {last_checked}, {next_check}, {frequency}, {warn_threshold}, {error_threshold}, {worst_state}, {consecutive_failures})
			""").on(
				"id"                    -> alert.id,
				"name"                  -> alert.name,
				"user_id"               -> alert.userId,
				"target"                -> alert.target,
				"comparison"            -> alert.comparison.id,
				"dynamic_alert_id"      -> alert.dynamicAlertId,
				"active"                -> alert.active,
				"deleted"               -> alert.deleted,
				"created"               -> alert.created,
				"updated"					-> alert.updated,
				"thread_id"             -> alert.threadId,
				"thread_start"          -> alert.threadStart,
				"last_checked"          -> alert.lastChecked,
				"next_check"            -> alert.nextCheck,
				"frequency"             -> alert.frequency,
				"warn_threshold"        -> alert.warnThreshold,
				"error_threshold"       -> alert.errorThreshold,
				"worst_state"           -> alert.worstState.id,
				"consecutive_failures"  -> alert.consecutiveFailures
			).executeUpdate()(connection)
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
			DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT *
					FROM `alerts`
					WHERE `id` IN ({ids})
				""").onList(
					"ids" -> ids
				).toSQL.as(alertsRowParser *)(connection)
			}
		}
	}

	/**
	 * Get all alerts propagated by a specific dynamic alert
	 * @param dynamicAlertId the id of the dynamic alert to look for
	 * @return the list of alerts
	 */
	def findAlertByDynamicAlert(id: UUID): List[Alert] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT *
				FROM `alerts`
				WHERE `dynamic_alert_id`={id}
			""").on(
				"id" -> id
			).as(alertsRowParser *)(connection)
		}
	}

	/**
	 * Search for alert records by a search term
	 * @param name the search term to search by
	 * @return a List of Alert records
	 */
	def search(name: String, page: Int) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `alerts`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
			""").on(
				"name" -> name
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `alerts`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"name" -> name,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(alertsRowParser *)(connection)

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
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `alerts`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
			""").on(
				"name" -> name,
				"user_id" -> userId
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `alerts`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"name" -> name,
				"user_id" -> userId,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(alertsRowParser *)(connection)

			(found, matches)
		}
	}

	/**
	 * Edit an alert in the database
	 * @param alert the edited values
	 */
	def editAlert(alert: Alert) = {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE `alerts`
				SET name={name}, target={target}, comparison={comparison}, active = {active}, deleted = {deleted}, updated = {updated},
					frequency={frequency}, error_threshold={error_threshold}, warn_threshold={warn_threshold}
				WHERE id={id}
			""").on(
				"id"							-> alert.id,
				"name"						-> alert.name,
				"target"						-> alert.target,
				"comparison"				-> alert.comparison.id,
				"active"						-> alert.active,
				"deleted"					-> alert.deleted,
				"updated"					-> alert.updated,
				"frequency"					-> alert.frequency,
				"warn_threshold"			-> alert.warnThreshold,
				"error_threshold"			-> alert.errorThreshold
			).executeUpdate()(connection)
		}
	}

	private def handleAlertErrors(threadId: UUID, alerts: List[Alert])(implicit connection: Connection) {
		RichSQL("""
			UPDATE `alerts`
			SET
				`consecutive_failures` = LEAST(`consecutive_failures` + 1, {max_failures}),
				`thread_id` = NULL,
				`thread_start` = NULL,
				`last_checked` = NOW(),
				`next_check` = DATE_ADD(NOW(), INTERVAL (`frequency` * `consecutive_failures` * {failure_multiplier}) SECOND)
			WHERE `id` IN ({ids}) AND `thread_id` = {thread_id}
		""").onList(
			"ids" -> alerts.map(_.id)
		).toSQL.on(
			"thread_id"          -> threadId,
			"max_failures"       -> maxConsecutiveFailures,
			"failure_multiplier" -> consecutiveFailuresMultiplier
		).executeUpdate()(connection)
	}

	/**
	 * Used by the alerting job to take the next alert(s) that needs to be checked, check the alert.
	 * @param threadId the thread of the worker checking the alerts
	 * @param limit the max number of alerts to take
	 * @param worker handles the alerts to be checked and returns the status (success or failure)
	 */
	def takeNextAlertsToCheck(threadId: UUID, limit: Integer)(worker: (List[Alert]) => Map[Alert, AlertStatus.Value]) = {
		val start = new Date()
		val alertsForWorker = DB.withTransaction("main") { connection =>
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
			""").on(
				"limit" -> limit
			).as(scalar[UUID] *)(connection)

			if (selectedAlertIds.isEmpty) {
				Nil
			}
			else {
				val updated = RichSQL("""
					UPDATE `alerts`
					SET `thread_id` = {thread_id},
						`thread_start` = {thread_start}
					WHERE `id` IN ({ids})
				""").onList(
					"ids" -> selectedAlertIds
				).toSQL.on(
					"thread_id" -> threadId,
					"thread_start" -> start
				).executeUpdate()(connection)

				RichSQL("""
					SELECT * FROM `alerts`
					WHERE
						`thread_id` = {thread_id}
						AND `thread_start` = {thread_start}
						AND `id` IN ({ids})
				""").onList(
					"ids" -> selectedAlertIds
				).toSQL.on(
					"thread_id" -> threadId,
					"thread_start" -> start
				).as(alertsRowParser *)(connection)
			}
		}

		try {
			val completedAlertStatus = worker(alertsForWorker)
			require(completedAlertStatus.size == alertsForWorker.size)

			if (!alertsForWorker.isEmpty) {
				DB.withConnection("main"){ connection =>
					completedAlertStatus.groupBy { case (alert, status) => status }.map { case (status, records) =>
						val alerts = records.map(_._1).toList
						status match {
							case AlertStatus.success => {
								alerts.groupBy { alert => alert.worstState }.map { case (worstState, sameAlerts) =>
									RichSQL("""
										UPDATE `alerts` 
										SET
											`consecutive_failures` = 0,
											`worst_state` = {worst_state},
											`thread_id` = NULL,
											`thread_start` = NULL,
											`last_checked` = NOW(),
											`next_check` = DATE_ADD(NOW(), INTERVAL `frequency` SECOND)
										WHERE `id` IN ({ids}) AND `thread_id` = {thread_id}
									""").onList(
										"ids" -> sameAlerts.map(_.id)
									).toSQL.on(
										"thread_id"   -> threadId,
										"worst_state" -> worstState.id
									).executeUpdate()(connection)
								}
							}
							case AlertStatus.failure => {
								handleAlertErrors(threadId, alerts)(connection)
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
	def cleanAlertThreadsBefore(date: Date) {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE `alerts` 
				SET `thread_id` = NULL,
					`thread_start` = NULL
				WHERE `thread_id` IS NOT NULL
				AND `thread_start` < {date}
			""").on(
				"date" -> date
			).executeUpdate() (connection)
		}
	}

	/**
	 * Find a particular alert propagated by a dynamic alert
	 * @param id the id of the dynamic alert that propagated the alert
	 * @param target the target of the alert
	 * @return optionally return the alert
	 */
	def findPropagatedAlert(id: UUID, target: String): Option[Alert] = {
		DB.withConnection("main") { connection =>
			val alerts = SQL("""
				SELECT *
				FROM `alerts`
				WHERE `dynamic_alert_id`={dynamic_alert_id} AND `target`={target}
			""").on(
				"dynamic_alert_id" -> id,
				"target" -> target
			).as(alertsRowParser *)(connection)

			alerts.headOption
		}
	}

	/**
	 * Find all alerts propagated by a dynamic alert
	 * @param id the id of the dynamic alert that propagated the alerts
	 * @return the propagated alerts
	 */
	def findPropagatedAlerts(id: UUID): List[Alert] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT *
				FROM `alerts`
				WHERE `dynamic_alert_id`={dynamic_alert_id}
			""").on(
				"dynamic_alert_id" -> id
			).as(alertsRowParser *)(connection)
		}
	}

	/**
	 * Delete a particular alert propagated by a dynamic alert
	 * @param id the id of the dynamic alert that propagated the alert
	 * @param target the target of the alert
	 */
	def deletePropagatedAlert(id: UUID, target: String): Unit = {
		DB.withConnection("main") { connection =>
			SQL("""
				DELETE FROM `alerts`
				WHERE `dynamic_alert_id`={dynamic_alert_id} AND `target`={target}
			""").on(
				"dynamic_alert_id" -> id,
				"target" -> target
			).executeUpdate()(connection)
		}
	}

	/**
	 * Delete all alerts propagated by a dynamic alert that were updated before a particular time
	 * @param id the id of the dynamic alert that propagated the alerts
	 * @param updated the updated time used as the cutoff
	 * @return the deleted alerts
	 */
	def deletePropagatedAlerts(id: UUID, updated: Date): List[Alert] = {
		DB.withConnection("main") { connection =>
			val deleted = SQL("""
				SELECT * FROM `alerts`
				WHERE `dynamic_alert_id`={dynamic_alert_id} AND `updated` < {updated}
			""").on(
				"dynamic_alert_id" -> id,
				"updated" -> updated
			).as(alertsRowParser *)(connection)

			SQL("""
				DELETE FROM `alerts`
				WHERE `dynamic_alert_id`={dynamic_alert_id} AND `updated` < {updated}
			""").on(
				"dynamic_alert_id" -> id,
				"updated" -> updated
			).executeUpdate()(connection)

			deleted
		}
	}
}
