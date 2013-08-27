package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.{DynamicAlert, Comparisons}

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import java.math.BigDecimal
import java.util.{Date, UUID}
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object DynamicAlertModel extends DynamicAlertModel
class DynamicAlertModel extends AppModel {

	/**
	 * Create a new dynamic alert using the details in the DynamicAlert object
	 * throws an exception on failure
	 * @param alert the dynamic alert to create
	 */
	def createDynamicAlert(alert: DynamicAlert): Unit = {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO `dynamic_alerts` (`id`, `name`, `user_id`, `created`, `search_target`, `match`, `build_target`, `error_threshold`, `warn_threshold`, `comparison`, `active`, `deleted`, `frequency`)
				VALUES ({id}, {name}, {user_id}, {created}, {search_target}, {match}, {build_target}, {error_threshold}, {warn_threshold}, {comparison}, {active}, {deleted}, {frequency})
			""").on(
				"id" -> alert.id,
				"name" -> alert.name,
				"user_id" -> alert.userId,
				"created" -> alert.created,
				"search_target" -> alert.searchTarget,
				"match" -> alert.matchExpr,
				"build_target" -> alert.buildTarget,
				"error_threshold" -> alert.errorThreshold,
				"warn_threshold" -> alert.warnThreshold,
				"comparison" -> alert.comparison.id,
				"active" -> alert.active,
				"deleted" -> alert.deleted,
				"frequency" -> alert.frequency
			).executeUpdate()(connection)
		}
	}
}
