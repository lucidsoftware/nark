package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.Alert
import AnormImplicits._
import anorm._
import play.api.Play.current
import play.api.db.DB

object AlertModel extends AlertModel
class AlertModel extends AppModel {

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
				"warn_threshold"		-> alert.warnThreshold,
				"error_threshold"		-> alert.errorThreshold,
				"state"					-> alert.state.id
			).executeUpdate()(connection)
		}
	}
}