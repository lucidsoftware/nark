package com.lucidchart.open.nark.models

import java.util.Date
import java.util.UUID

import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._
import records.Dashboard
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB
import com.lucidchart.open.nark.views.html.dashboards.dashboard

class DashboardModel extends AppModel {
	protected val dashboardsRowParser = RowParser { row =>
		Dashboard(
			row.uuid("id"),
			row.string("name"),
			row.string("url"),
			row.date("created"),
			row.uuid("user_id"),
			row.bool("deleted")
		)
	}
	
	/**
	 * Find the dashboard that has the matching id
	 * 
	 * @param id
	 * @return dashboard
	 */
	def findDashboardByID(id: UUID): Option[Dashboard] = {
		findDashboardByID(List(id)).headOption
	}
	
	/**
	 * Find the dashboards that have matching ids
	 * 
	 * @param ids
	 * @return dashboards
	 */
	def findDashboardByID(ids: List[UUID]): List[Dashboard] = {
		if(ids.isEmpty)
			return Nil
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `dashboards`
				WHERE `id` in ({ids})
			""").expand { implicit query =>
				commaSeparated("ids", ids.size)
			}.on { implicit query =>
				uuids("ids", ids)
			}.asList(dashboardsRowParser)
		}
	}	

	/**
	 * Find the dashboard that has the matching url
	 *
	 * @param url
	 * @return dashboard
	 */
	def findDashboardByURL(url: String): Option[Dashboard] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `dashboards`
				WHERE `url` = {url}
				LIMIT 1
			""").on { implicit query =>
				string("url", url)
			}.asSingleOption(dashboardsRowParser)
		}
	}

	/**
	 * Search for dashboards by name
	 */
	def search(name: String, page: Int) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `dashboards`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
			""").on { implicit query =>
				string("name", name)
			}.asScalar[Long]

			val matches = SQL("""
				SELECT * FROM `dashboards`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				string("name", name)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(dashboardsRowParser)

			(found, matches)
		}
	}

	/**
	 * Get all deleted dashboards for a user
	 * @param user_id
	 * @return dashboards
	 */
	def searchDeleted(userId: UUID, name: String, page: Int) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `dashboards`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
			""").on { implicit query =>
				string("name", name)
				uuid("user_id", userId)
			}.asScalar[Long]

			val matches = SQL("""
				SELECT * FROM `dashboards`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				string("name", name)
				uuid("user_id", userId)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(dashboardsRowParser)

			(found, matches)
		}
	}

	/**
	 * Create a new dashboard using all the details from the dashboard object.
	 * Throws an exception on failure
	 * 
	 * @param dashboard
	 */
	def createDashboard(dashboard: Dashboard) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				INSERT INTO `dashboards` (`id`, `name`, `url`, `created`, `user_id`, `deleted`)
				VALUES ({id}, {name}, {url}, {created}, {user_id}, {deleted})
			""").on { implicit query =>
				uuid("id", dashboard.id)
				string("name", dashboard.name)
				string("url", dashboard.url)
				date("created", new Date())
				uuid("user_id", dashboard.userId)
				bool("deleted", dashboard.deleted)
			}.executeUpdate()
		}
	}

	/**
	 * Edit an existing dashboard. not all fields can be updated.
	 * Throws an exception on failure
	 * 
	 * @param dashboard
	 */
	def editDashboard(dashboard: Dashboard) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				UPDATE `dashboards` SET `name` = {name}, `url` = {url}, `deleted` = {deleted} WHERE `id` = {id}
			""").on { implicit query =>
				uuid("id", dashboard.id)
				string("name", dashboard.name)
				string("url", dashboard.url)
				bool("deleted", dashboard.deleted)
			}.executeUpdate()
		}
	}
}

object DashboardModel extends DashboardModel
