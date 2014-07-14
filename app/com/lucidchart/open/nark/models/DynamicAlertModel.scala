package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.{DynamicAlert, Comparisons}
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._

import java.math.BigDecimal
import java.util.{Date, UUID}
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object DynamicAlertModel extends DynamicAlertModel
class DynamicAlertModel extends AppModel {

	protected val dynamicAlertsRowParser = RowParser { row =>
		DynamicAlert(
			row.uuid("id"),
			row.string("name"),
			row.uuid("user_id"),
			row.string("search_target"),
			row.string("match"),
			row.string("build_target"),
			Comparisons(row.int("comparison")),
			row.bool("active"),
			row.bool("deleted"),
			row.date("created"),
			row.int("frequency"),
			row.bigDecimal("error_threshold"),
			row.bigDecimal("warn_threshold"),
			row.int("data_seconds"),
			row.int("drop_null_points"),
			row.bool("drop_null_targets")
		)
	}

	/**
	 * Create a new dynamic alert using the details in the DynamicAlert object
	 * throws an exception on failure
	 * @param alert the dynamic alert to create
	 */
	def createDynamicAlert(alert: DynamicAlert): Unit = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				INSERT INTO `dynamic_alerts` (`id`, `name`, `user_id`, `created`, `search_target`, `match`, `build_target`, `error_threshold`, `warn_threshold`, `comparison`, `active`, `deleted`, `frequency`, `data_seconds`, `drop_null_points`, `drop_null_targets`)
				VALUES ({id}, {name}, {user_id}, {created}, {search_target}, {match}, {build_target}, {error_threshold}, {warn_threshold}, {comparison}, {active}, {deleted}, {frequency}, {data_seconds}, {drop_null_points}, {drop_null_targets})
			""").on { implicit query =>
				uuid("id", alert.id)
				string("name", alert.name)
				uuid("user_id", alert.userId)
				date("created", alert.created)
				string("search_target", alert.searchTarget)
				string("match", alert.matchExpr)
				string("build_target", alert.buildTarget)
				bigDecimal("error_threshold", alert.errorThreshold)
				bigDecimal("warn_threshold", alert.warnThreshold)
				int("comparison", alert.comparison.id)
				bool("active", alert.active)
				bool("deleted", alert.deleted)
				int("frequency", alert.frequency)
				int("data_seconds", alert.dataSeconds)
				int("drop_null_points", alert.dropNullPoints)
				bool("drop_null_targets", alert.dropNullTargets)
			}.executeUpdate()
		}
	}

	/**
	 * Search for dynamic alert records by a search term
	 * @param name the search term to search by
	 * @param page the page of search results to return
	 * @return a List of DynamicAlert records
	 */
	def search(name: String, page: Int) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `dynamic_alerts`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
			""").on { implicit query =>
				string("name", name)
			}.asScalar[Long]

			val matches = SQL("""
				SELECT * FROM `dynamic_alerts`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				string("name", name)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(dynamicAlertsRowParser)

			(found, matches)
		}
	}

	/**
	 * Get all deleted dynamic alerts for a user
	 * @param userId the id of the user
	 * @param name the name of the dynamic alert to search for
	 * @param page the page of deleted dynamic alerts to return
	 * @return the number of matching dynamic alerts and a list of matching dynamic alerts
	 */
	def searchDeleted(userId: UUID, name: String, page: Int): (Long, List[DynamicAlert]) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `dynamic_alerts`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
			""").on { implicit query =>
				string("name", name)
				uuid("user_id", userId)
			}.asScalar[Long]

			val matches = SQL("""
				SELECT * FROM `dynamic_alerts`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				string("name", name)
				uuid("user_id", userId)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(dynamicAlertsRowParser)

			(found, matches)
		}
	}

	/**
	 * Get the dynamic alert specified by the uuid
	 * @param id the uuid of the dynamic alert to get
	 * @return the requested dynamic alert
	 */
	def findDynamicAlertByID(id: UUID): Option[DynamicAlert] = {
		findDynamicAlertByID(List(id)).headOption
	}

	/**
	 * Get the dynamic alerts specified by the uuids passed in
	 * @param ids the ids of the dynamic alerts to get
	 * @return the requested dynamic alerts
	 */
	def findDynamicAlertByID(ids: List[UUID]): List[DynamicAlert] = {
		if (ids.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { implicit connection =>
				SQL("""
					SELECT *
					FROM `dynamic_alerts`
					WHERE `id` IN ({ids})
				""").expand { implicit query =>
					commaSeparated("ids", ids.size)
				}.on { implicit query =>
					uuids("ids", ids)
				}.asList(dynamicAlertsRowParser)
			}
		}
	}

	/**
	 * Get all active non deleted dynamic alerts
	 * @return the list of active non deleted dynamic alerts
	 */
	def findActiveDynamicAlerts(): List[DynamicAlert] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `dynamic_alerts`
				WHERE `active` = TRUE AND `deleted` = FALSE
			""").asList(dynamicAlertsRowParser)
		}
	}

	/**
	 * Edit a particular dynamic alert
	 * @param alert the edited alert
	 */
	def editDynamicAlert(alert: DynamicAlert) = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				UPDATE `dynamic_alerts`
				SET `name`={name}, `search_target`={search_target}, `match`={match_expr}, `build_target`={build_target}, `error_threshold`={error_threshold}, `warn_threshold`={warn_threshold}, `comparison`={comparison}, `active`={active}, `deleted`={deleted}, `frequency`={frequency}, `data_seconds`={data_seconds}, `drop_null_points`={drop_null_points}, `drop_null_targets`={drop_null_targets}
				WHERE `id`={id}
			""").on { implicit query =>
				string("name", alert.name)
				string("search_target", alert.searchTarget)
				string("match_expr", alert.matchExpr)
				string("build_target", alert.buildTarget)
				bigDecimal("error_threshold", alert.errorThreshold)
				bigDecimal("warn_threshold", alert.warnThreshold)
				int("comparison", alert.comparison.id)
				bool("active", alert.active)
				bool("deleted", alert.deleted)
				int("frequency", alert.frequency)
				int("data_seconds", alert.dataSeconds)
				int("drop_null_points", alert.dropNullPoints)
				bool("drop_null_targets", alert.dropNullTargets)
				uuid("id", alert.id)
			}.executeUpdate()
		}
	}
}
