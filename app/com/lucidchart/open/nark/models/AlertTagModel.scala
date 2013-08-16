package com.lucidchart.open.nark.models

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.AlertTag
import java.util.UUID
import play.api.db.DB
import play.api.Play.current
import play.api.Play.configuration

object AlertTagModel extends AlertTagModel
class AlertTagModel extends AppModel {

	protected val tagsRowParser = {
		get[UUID]("alert_id") ~
		get[String]("name") map {
			case alertId ~ tag => new AlertTag(alertId, tag)
		}
	}

	/**
	 * Add tags to an alert, ignores duplicates
	 * @param alertId the id of the alert to add tags to
	 * @param tags a list of tags to add to the alert
	 */
	def addTagsToAlert(alertId: UUID, tags: List[String]) = {
		if (!tags.isEmpty){
			DB.withConnection("main") { connection =>
				RichSQL("""
					INSERT IGNORE INTO `alert_tags` (`alert_id`, `name`)
					VALUES ({fields})
				""").multiInsert(tags.size, Seq("alert_id", "name"))(
					"alert_id" -> tags.map(_ => toParameterValue(alertId)),
					"name" -> tags.map(toParameterValue(_))
				).toSQL.executeUpdate()(connection)
			}
		}
	}

	/**
	 * Find all the tags associated with an alert
	 * @param id the id of the alert to search for
	 */
	def findTagsForAlert(id: UUID): List[AlertTag] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT *
				FROM `alert_tags`
				WHERE `alert_id`={id}
			""").on(
				"id" -> id
			).as(tagsRowParser *)(connection)
		}
	}

	/**
	 * Search for all alerts associated with a tag
	 * @param tag the tag to search for
	 */
	def findAlertsByTag(tag: String): List[AlertTag] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT *
				FROM `alert_tags`
				WHERE `name`={tag}
			""").on(
				"tag" -> tag
			).as(tagsRowParser *)(connection)
		}
	}

	/**
	 * Update the tags associated with an alert
	 * @param id the id of the alert to update
	 * @param tags the new tags to associate with the alert
	 */
	def updateTagsForAlert(id: UUID, tags: List[String]) = {
		removeAllTagsFromAlert(id)
		if (!tags.isEmpty) {
			addTagsToAlert(id, tags)
		}
	}

	/**
	 * Delete all tags for a particular alert
	 * @param id the id of the alert from which to delete tags
	 */
	def removeAllTagsFromAlert(id: UUID) = {
		DB.withConnection("main") { connection =>
			SQL("""
				DELETE FROM `alert_tags`
				WHERE `alert_id`={id}
			""").on(
				"id" -> id
			).executeUpdate()(connection)
		}
	}
}