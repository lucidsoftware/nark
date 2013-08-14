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
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT *
				FROM `alerts`
				WHERE `id`={id}
			""").on(
				"id" -> id
			).as(alertsRowParser singleOpt)(connection)
		}
	}

	/**
	 * Search for alert records by a search term
	 * @param name the search term to search by
	 * @return a List of Alert records
	 */
	def searchByName(name: String): List[Alert] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT *
				FROM `alerts`
				WHERE `name` LIKE {name}
				AND `deleted` = false
				ORDER BY `name`
				LIMIT {limit}
			""").on(
				"name" -> ("%" + name + "%"),
				"limit" -> configuration.getInt("search.limit").get
			).as(alertsRowParser *)(connection)
		}
	}
}