package com.lucidchart.open.nark.models

import anorm._
import anorm.SqlParser._
import AnormImplicits._
import com.lucidchart.open.nark.models.records.{Alert, Tag}
import java.util.UUID
import play.api.db.DB
import play.api.Play.current
import play.api.Play.configuration

object AlertTagModel extends AlertTagModel
trait AlertTagModel extends AppModel {
	protected val table = "alert_tags"

	protected val tagsRowParser = {
		get[UUID]("alert_id") ~
		get[String]("tag") map {
			case alertId ~ tag => new Tag(alertId, tag)
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
				SELECT COUNT(distinct(`tag`)) FROM `""" + table + """`
				WHERE `tag` LIKE {tag}
			""").on(
				"tag" -> tag
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `""" + table + """`
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
	def findTagsForAlert(id: UUID): List[Tag] = {
		findTagsForAlert(List(id))
	}
	
	/**
	 * Find all tags for a list of alerts
	 * @param ids the ids of the alerts to look for
	 * @return a list of matching AlertTags
	 */
	def findTagsForAlert(ids: List[UUID]) : List[Tag] = {
		if (ids.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT *
					FROM `""" + table + """`
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
	def findAlertsByTag(tag: String): List[Tag] = {
		findAlertsByTag(List(tag))
	}

	/**
	 * Search for all alerts associated with tags
	 * @param tags the tags to search for
	 * @return the AlertTags
	 */
	def findAlertsByTag(tags: List[String]): List[Tag] = {
		if(tags.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT *
					FROM `""" + table + """`
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
					DELETE FROM `""" + table + """`
					WHERE `alert_id` = {alert_id}
				""").on(
					"alert_id" -> id
				).executeUpdate()(connection)
			}
			else {
				RichSQL("""
					DELETE FROM `""" + table + """`
					WHERE `alert_id` = {alert_id} AND `tag` NOT IN ({tags})
				""").onList(
					"tags" -> tags
				).toSQL.on(
					"alert_id" -> id
				).executeUpdate()(connection)

				RichSQL("""
					INSERT IGNORE INTO `""" + table + """` (`alert_id`, `tag`)
					VALUES ({fields})
				""").multiInsert(tags.size, Seq("alert_id", "tag"))(
					"alert_id" -> tags.map(_ => toParameterValue(id)),
					"tag" -> tags.map(toParameterValue(_))
				).toSQL.executeUpdate()(connection)
			}
		}
	}
}
