package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.{Alert, Tag}
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._
import java.util.UUID
import play.api.db.DB
import play.api.Play.current
import play.api.Play.configuration

object AlertTagModel extends AlertTagModel
trait AlertTagModel extends AppModel {
	protected val table = "alert_tags"

	protected val tagsRowParser = RowParser { row =>
		Tag(
			row.uuid("alert_id"),
			row.string("tag")
		)
	}

	/**
	 * Find all tags similar to the search term
	 * @param tag the search term
	 * @return the list of matched tags
	 */
	def search(tag: String, page: Int) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(distinct(`tag`)) FROM `""" + table + """`
				WHERE `tag` LIKE {tag}
			""").on { implicit query =>
				string("tag", tag)
			}.asScalar[Long]

			val matches = SQL("""
				SELECT * FROM `""" + table + """`
				WHERE `tag` LIKE {tag}
				GROUP BY `tag`
				ORDER BY `tag` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				string("tag", tag)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(tagsRowParser)

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
			DB.withConnection("main") { implicit connection =>
				SQL("""
					SELECT *
					FROM `""" + table + """`
					WHERE `alert_id` IN ({ids})
				""").expand { implicit query =>
					commaSeparated("ids", ids.size)
				}.on { implicit query =>
					uuids("ids", ids)
				}.asList(tagsRowParser)
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
			DB.withConnection("main") { implicit connection =>
				SQL("""
					SELECT *
					FROM `""" + table + """`
					WHERE `tag` in ({tags})
				""").expand { implicit query =>
					commaSeparated("tags", tags.size)
				}.on { implicit query =>
					strings("tags", tags)
				}.asList(tagsRowParser)
			}
		}
	}

	/**
	 * Update the tags associated with an alert
	 * @param id the id of the alert to update
	 * @param tags the new tags to associate with the alert
	 */
	def updateTagsForAlert(id: UUID, tags: List[String]) = {
		DB.withTransaction("main") { implicit connection =>
			if (tags.isEmpty) {
				SQL("""
					DELETE FROM `""" + table + """`
					WHERE `alert_id` = {alert_id}
				""").on { implicit query =>
					uuid("alert_id", id)
				}.executeUpdate()
			}
			else {
				SQL("""
					DELETE FROM `""" + table + """`
					WHERE `alert_id` = {alert_id} AND `tag` NOT IN ({tags})
				""").expand { implicit query =>
					commaSeparated("tags", tags.size)
				}.on { implicit query =>
					strings("tags", tags)
					uuid("alert_id", id)
				}.executeUpdate()

				SQL("""
					INSERT IGNORE INTO `""" + table + """` (`alert_id`, `tag`)
					VALUES {fields}
				""").expand { implicit query =>
					tupled("fields", List("alert_id", "tag"), tags.size)
				}.onTuples("fields", tags) { (tag, query) =>
					query.uuid("alert_id", id)
					query.string("tag", tag)
				}.executeUpdate()
			}
		}
	}
}
