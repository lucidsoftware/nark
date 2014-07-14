package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.{Tag}
import com.lucidchart.open.nark.models.records.Dashboard
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._

import java.util.UUID
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object DashboardTagsModel extends DashboardTagsModel
class DashboardTagsModel extends AppModel {
	protected val tagsRowParser = RowParser { row =>
		Tag(
			row.uuid("dashboard_id"),
			row.string("tag")
		)
	}

	/**
	 * Gets all tags.
	 * @return tags
	 */
	def search(tag: String, page: Int) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(DISTINCT(`tag`)) FROM `dashboard_tags`
				WHERE `tag` LIKE {tag}
			""").on { implicit query =>
				string("tag", tag)
			}.asScalar[Long]

			val matches = SQL("""
				SELECT * FROM `dashboard_tags`
				WHERE `tag` LIKE {tag}
				GROUP BY `tag`
				ORDER BY `tag` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				string("tag", tag)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(RowParser.string("tag"))

			(found, matches)
		}
	}

	/**
	 * Gets all dashboards with this tag.
	 * @param tag
	 * @return dashboardTags
	 */
	def findDashboardsWithTag(tag: String): List[Tag] = {
		findDashboardsWithTag(List(tag))
	}

	/**
	 * Gets all dashboards with this tag.
	 * @param tag
	 * @return dashboardTags
	 */
	def findDashboardsWithTag(tags: List[String]): List[Tag] = {
		if (tags.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { implicit connection =>
				SQL("""
					SELECT * 
					FROM `dashboard_tags` 
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
	 * Gets all tags for a dashbaord.
	 * @param dashbaordId
	 * @return dashbaordTags
	 */
	def findTagsForDashboard(dashboardId: UUID) : List[Tag] = {
		findTagsForDashboard(List(dashboardId))
	}

	/**
	 * Gets all tags for dashbaords.
	 * @param dashbaordId
	 * @return tags
	 */
	def findTagsForDashboard(dashboardIds: List[UUID]) : List[Tag] = {
		if(dashboardIds.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { implicit connection =>
				SQL("""
					SELECT * 
					FROM `dashboard_tags` 
					WHERE `dashboard_id` IN ({dashboardIds})
					ORDER BY `dashboard_id`
				""").expand { implicit query =>
					commaSeparated("dashboardIds", dashboardIds.size)
				}.on { implicit query =>
					uuids("dashboardIds", dashboardIds)
				}.asList(tagsRowParser)
			}
		}
	}

	/**
	 * Update the tags associated with a dashboard
	 * @param id the id of the dashboard to update
	 * @param tags the new tags to associate with the dashboard
	 */
	def updateTagsForDashboard(id: UUID, tags: List[String]) = {
		DB.withTransaction("main") { implicit connection =>
			if (tags.isEmpty) {
				SQL("""
					DELETE FROM `dashboard_tags`
					WHERE `dashboard_id` = {dashboard_id}
				""").on { implicit query =>
					uuid("dashboard_id", id)
				}.executeUpdate()
			}
			else {
				SQL("""
					DELETE FROM `dashboard_tags`
					WHERE `dashboard_id` = {dashboard_id} AND `tag` NOT IN ({tags})
				""").expand { implicit query =>
					commaSeparated("tags", tags.size)
				}.on { implicit query =>
					strings("tags", tags)
					uuid("dashboard_id", id)
				}.executeUpdate()

				SQL("""
					INSERT IGNORE INTO `dashboard_tags` (`dashboard_id`, `tag`)
					VALUES {fields}
				""").expand { implicit query =>
					tupled("fields", List("dashboard_id", "tag"), tags.size)
				}.onTuples("fields", tags) { (tag, query) =>
					query.uuid("dashboard_id", id)
					query.string("tag", tag)
				}.executeUpdate()
			}
		}
	}
}
