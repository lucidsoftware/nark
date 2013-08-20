package com.lucidchart.open.nark.models

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.{Alert, AlertState, Comparisons}
import java.math.BigDecimal
import java.util.{Date, UUID}
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object AlertModel extends AlertModel
class AlertModel extends AppModel {

	protected val alertsRowParser = {
		get[UUID]("id") ~ 
		get[String]("name") ~
		get[UUID]("user_id") ~
		get[String]("target") ~
		get[Int]("comparison") ~
		get[Boolean]("active") ~
		get[Boolean]("deleted") ~
		get[Date]("created") ~
		get[Option[UUID]]("thread_id") ~
		get[Date]("last_checked") ~
		get[Date]("next_check") ~
		get[Int]("frequency") ~
		get[BigDecimal]("warn_threshold") ~
		get[BigDecimal]("error_threshold") ~
		get[Int]("state") map {
			case id ~ name ~ user_id ~ target ~ comparison ~ active ~ deleted ~ created ~ thread_id ~ last_checked ~ next_check ~ frequency ~ warn_threshold ~ error_threshold ~ state =>
				new Alert(id, name, user_id, target, Comparisons(comparison), active, deleted, created, thread_id, last_checked, next_check, frequency, warn_threshold, error_threshold, AlertState(state))
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
				INSERT INTO `alerts` (`id`, `name`, `user_id`, `target`, `comparison`, `active`, `deleted`, `created`, `thread_id`, `last_checked`, `next_check`, `frequency`, `warn_threshold`, `error_threshold`, `state`)
				VALUES ({id}, {name}, {user_id}, {target}, {comparison}, {active}, {deleted}, {created}, {thread_id}, {last_checked}, {next_check}, {frequency}, {warn_threshold}, {error_threshold}, {state})
			""").on(
				"id"						-> alert.id,
				"name"					-> alert.name,
				"user_id"				-> alert.userId,
				"target"					-> alert.target,
				"comparison"			-> alert.comparison.id,
				"active"					-> alert.active,
				"deleted"				-> alert.deleted,
				"created"				-> alert.created,
				"thread_id"				-> alert.threadId,
				"last_checked"			-> alert.lastChecked,
				"next_check"			-> alert.nextCheck,
				"frequency"				-> alert.frequency,
				"warn_threshold"		-> alert.warnThreshold.underlying,
				"error_threshold"		-> alert.errorThreshold.underlying,
				"state"					-> alert.state.id
			).executeUpdate()(connection)
		}
	}

	/**
	 * Get the alert specified by the uuid
	 * @param id the uuid of the alert to get
	 * @return the requested alert
	 */
	def getAlert(id: UUID): Option[Alert] = {
		getAlerts(List(id)).headOption
	}

	/**
	 * Get the alerts specified by the uuids passed in
	 * @param ids the ids of the alerts to get
	 * @return the requested alerts
	 */
	def getAlerts(ids: List[UUID]): List[Alert] = {
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
	 * Edit an alert in the database
	 * @param alert the edited values
	 */
	def editAlert(alert: Alert) = {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE `alerts`
				SET name={name}, target={target}, error_threshold={error_threshold}, warn_threshold={warn_threshold}, comparison={comparison}, frequency={frequency}, active={active}
				WHERE id={id}
			""").on(
				"name" -> alert.name,
				"target" -> alert.target,
				"error_threshold" -> alert.errorThreshold.underlying,
				"warn_threshold" -> alert.warnThreshold.underlying,
				"comparison" -> alert.comparison.id,
				"frequency" -> alert.frequency,
				"active" -> alert.active,
				"id" -> alert.id
			).executeUpdate()(connection)
		}
	}

	/**
	 * Delete an alert from the database
	 * @param id the id of the alert to delete
	 */
	def deleteAlert(id: UUID) = {
		DB.withConnection("main") { connection =>
			SQL("""
				DELETE FROM `alerts`
				WHERE id={id}
			""").on(
				"id" -> id
			).executeUpdate()(connection)
		}
	}
}