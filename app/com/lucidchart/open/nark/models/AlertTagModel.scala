package com.lucidchart.open.nark.models

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.{Alert, AlertTag}
import java.util.UUID
import play.api.db.DB
import play.api.Play.current
import play.api.Play.configuration

object AlertTagModel extends AlertTagModel
class AlertTagModel extends AppModel {
	protected val tagsRowParser = {
		get[UUID]("alert_id") ~
		get[String]("tag") map {
			case alertId ~ tag => new AlertTag(alertId, tag)
		}
	}

	/**
	 * Find all tags similar to the search term
	 * @param tag the search term
	 * @return the list of matched tags
	 */
	def search(tag: String, page: Int) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(distinct(`tag`)) FROM `alert_tags`
				WHERE `tag` LIKE {tag}
			""").on(
				"tag" -> tag
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `alert_tags`
				WHERE `tag` LIKE {tag}
				GROUP BY `tag`
				ORDER BY `tag` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"tag" -> tag,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(tagsRowParser *)(connection)

			(found, matches)
		}
	}

	/**
	 * Find all the tags associated with an alert
	 * @param id the id of the alert to search for
	 */
	def findTagsForAlert(id: UUID): List[AlertTag] = {
		findTagsForAlert(List(id))
	}
	
	/**
	 * Find all tags for a list of alerts
	 * @param ids the ids of the alerts to look for
	 * @return a list of matching AlertTags
	 */
	def findTagsForAlert(ids: List[UUID]) : List[AlertTag] = {
		if (ids.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT *
					FROM `alert_tags`
					WHERE `alert_id` IN ({ids})
				""").onList(
					"ids" -> ids
				).toSQL.as(tagsRowParser *)(connection)
			}
		}
	}

	/**
	 * Search for all alerts associated with a tag
	 * @param tag the tag to search for
	 * @return the AlertTags
	 */
	def findAlertsByTag(tag: String): List[AlertTag] = {
		findAlertsByTag(List(tag))
	}

	/**
	 * Search for all alerts associated with tags
	 * @param tags the tags to search for
	 * @return the AlertTags
	 */
	def findAlertsByTag(tags: List[String]): List[AlertTag] = {
		if(tags.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT *
					FROM `alert_tags`
					WHERE `tag` in ({tags})
				""").onList(
					"tags" -> tags
				).toSQL.as(tagsRowParser *)(connection)
			}
		}
	}

	/**
	 * Update the tags associated with an alert
	 * @param id the id of the alert to update
	 * @param tags the new tags to associate with the alert
	 */
	def updateTagsForAlert(id: UUID, tags: List[String]) = {
		DB.withTransaction("main") { connection =>
			if (tags.isEmpty) {
				SQL("""
					DELETE FROM `alert_tags`
					WHERE `alert_id` = {alert_id}
				""").on(
					"alert_id" -> id
				).executeUpdate()(connection)
			}
			else {
				RichSQL("""
					DELETE FROM `alert_tags`
					WHERE `alert_id` = {alert_id} AND `tag` NOT IN ({tags})
				""").onList(
					"tags" -> tags
				).toSQL.on(
					"alert_id" -> id
				).executeUpdate()(connection)

				RichSQL("""
					INSERT IGNORE INTO `alert_tags` (`alert_id`, `tag`)
					VALUES ({fields})
				""").multiInsert(tags.size, Seq("alert_id", "tag"))(
					"alert_id" -> tags.map(_ => toParameterValue(id)),
					"tag" -> tags.map(toParameterValue(_))
				).toSQL.executeUpdate()(connection)
			}
		}
	}
}

object AlertTagConverter {
	/**
	 * Combine a list of alert tags and a list of alerts into a map
	 * of tag to list of matching alert pairs
	 */
	def toTagMap(tags: List[AlertTag], alerts: List[Alert]): Map[String, List[Alert]] = {
		val alertsById = alerts.map { a => (a.id, a) }.toMap
		tags.map { tag =>
			(tag, alertsById.get(tag.alertId))
		}.collect {
			case (tag, alertOption) if (alertOption.isDefined) => (tag, alertOption.get)
		}.groupBy { case (tag, alert) =>
			tag.tag
		}.map { case (tag, tuples) =>
			(tag, tuples.map(_._2))
		}.toMap
	}

	/**
	 * Find all the tags for each alert ID
	 */
	def toAlertMap(tags: List[AlertTag]): Map[UUID, List[String]] = {
		tags.groupBy { tag =>
			tag.alertId
		}.map { case (alertId, tags) =>
			(alertId, tags.map(_.tag))
		}
	}
}
