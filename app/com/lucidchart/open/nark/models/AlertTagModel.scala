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
	 * Find all tags similar to the search term
	 * @param name the search term
	 * @return the list of matched tags
	 */
	def search(name: String, page: Int) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `alert_tags`
				WHERE `name` LIKE {name}
			""").on(
				"name" -> name
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `alert_tags`
				WHERE `name` LIKE {name}
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"name" -> name,
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
	 * Search for all alerts associated with tags
	 * @param tag the tag to search for
	 * @return the AlertTags
	 */
	def findAlertsByTag(tags: List[String]): List[AlertTag] = {
		if(tags.isEmpty)
			return Nil
		DB.withConnection("main") { connection =>
			RichSQL("""
				SELECT *
				FROM `alert_tags`
				WHERE `name` in ({tags})
			""").onList(
				"tags" -> tags
			).toSQL.as(tagsRowParser *)(connection)
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

object AlertTagConverter {
	def toTagMap(tags: List[AlertTag], alerts: List[Alert]): Map[String, List[Alert]] = {
		tags.foldLeft[Map[String, List[Alert]]](Map()) { (ret, tag) =>
			val alert = alerts.find(_.id == tag.alertId)
			if (alert.isDefined) {
				ret + (tag.tag -> (
					if (ret contains tag.tag) {
						alert.get :: ret.get(tag.tag).get
					}
					else {
						List(alert.get)
					})

				)
			}
			else {
				ret
			}
		} 
	}

	def toAlertMap(tags: List[AlertTag]): Map[UUID, List[String]] = {
		tags.foldLeft[Map[UUID, List[String]]](Map()) { (ret, tag) =>
			ret + (tag.alertId -> (
					if (ret contains tag.alertId) {
						tag.tag :: ret.get(tag.alertId).get
					}
					else {
						List(tag.tag)
					}
				)
			)
		}
	}

}
