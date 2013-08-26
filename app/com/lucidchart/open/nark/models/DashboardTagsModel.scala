package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.DashboardTag
import com.lucidchart.open.nark.models.records.Dashboard

import java.util.UUID
import AnormImplicits._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

object DashboardTagsModel extends DashboardTagsModel
class DashboardTagsModel extends AppModel {
	protected val tagsRowParser =  {
		get[UUID]("dashboard_id") ~
		get[String]("tag") map {
			case dashboardId ~ tag => new DashboardTag(dashboardId, tag)
		}
	}

	/**
	 * Gets all tags.
	 * @returns tags
	 */
	def search(tag: String, page: Int) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(DISTINCT(`tag`)) FROM `dashboard_tags`
				WHERE `tag` LIKE {tag}
			""").on(
				"tag" -> tag
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `dashboard_tags`
				WHERE `tag` LIKE {tag}
				GROUP BY `tag`
				ORDER BY `tag` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"tag" -> tag,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(get[String]("tag") *)(connection)

			(found, matches)
		}
	}

	/**
	 * Gets all dashboards with this tag.
	 * @param tag
	 * @returns dashboardTags
	 */
	def findDashboardsWithTag(tag: String): List[DashboardTag] = {
		findDashboardsWithTag(List(tag))
	}

	/**
	 * Gets all dashboards with this tag.
	 * @param tag
	 * @returns dashboardTags
	 */
	def findDashboardsWithTag(tags: List[String]): List[DashboardTag] = {
		if (tags.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT * 
					FROM `dashboard_tags` 
					WHERE `tag` in ({tags})
				""").onList(
					"tags" -> tags
				).toSQL.as(tagsRowParser *)(connection)
			}
		}
	}


	/**
	 * Gets all tags for a dashbaord.
	 * @param dashbaordId
	 * @returns dashbaordTags
	 */
	def findTagsForDashboard(dashboardId: UUID) : List[DashboardTag] = {
		findTagsForDashboard(List(dashboardId))
	}

	/**
	 * Gets all tags for dashbaords.
	 * @param dashbaordId
	 * @returns tags
	 */
	def findTagsForDashboard(dashboardIds: List[UUID]) : List[DashboardTag] = {
		if(dashboardIds.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") {connection =>
				RichSQL("""
					SELECT * 
					FROM `dashboard_tags` 
					WHERE `dashboard_id` IN ({dashboardIds})
					ORDER BY `dashboard_id`
				""").onList(
					"dashboardIds" -> dashboardIds
				).toSQL.as(tagsRowParser *)(connection)
			}
		}
	}

	/**
	 * Update the tags associated with a dashboard
	 * @param id the id of the dashboard to update
	 * @param tags the new tags to associate with the dashboard
	 */
	def updateTagsForDashboard(id: UUID, tags: List[String]) = {
		DB.withTransaction("main") { connection =>
			if (tags.isEmpty) {
				SQL("""
					DELETE FROM `dashboard_tags`
					WHERE `dashboard_id` = {dashboard_id}
				""").on(
					"dashboard_id" -> id
				).executeUpdate()(connection)
			}
			else {
				RichSQL("""
					DELETE FROM `dashboard_tags`
					WHERE `dashboard_id` = {dashboard_id} AND `tag` NOT IN ({tags})
				""").onList(
					"tags" -> tags
				).toSQL.on(
					"dashboard_id" -> id
				).executeUpdate()(connection)

				RichSQL("""
					INSERT IGNORE INTO `dashboard_tags` (`dashboard_id`, `tag`)
					VALUES ({fields})
				""").multiInsert(tags.size, Seq("dashboard_id", "tag"))(
					"dashboard_id" -> tags.map(_ => toParameterValue(id)),
					"tag" -> tags.map(toParameterValue(_))
				).toSQL.executeUpdate()(connection)
			}
		}
	}
}

object DashboardTagConverter {
	/**
	 * Combine a list of dashboard tags and a list of dashboards into a map
	 * of tag to list of matching dashboard pairs
	 */
	def toTagMap(tags: List[DashboardTag], dashboards: List[Dashboard]): Map[String, List[Dashboard]] = {
		val dashboardsById = dashboards.map { a => (a.id, a) }.toMap
		tags.map { tag =>
			(tag, dashboardsById.get(tag.dashboardId))
		}.collect {
			case (tag, dashboardOption) if (dashboardOption.isDefined) => (tag, dashboardOption.get)
		}.groupBy { case (tag, dashboard) =>
			tag.tag
		}.map { case (tag, tuples) =>
			(tag, tuples.map(_._2))
		}.toMap
	}

	/**
	 * Find all the tags for each dashboard ID
	 */
	def toDashboardMap(tags: List[DashboardTag]): Map[UUID, List[String]] = {
		tags.groupBy { tag =>
			tag.dashboardId
		}.map { case (dashboardId, tags) =>
			(dashboardId, tags.map(_.tag))
		}
	}
}