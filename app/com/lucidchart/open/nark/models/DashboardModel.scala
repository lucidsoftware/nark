package com.lucidchart.open.nark.models

import java.util.Date
import java.util.UUID

import records.Dashboard

import AnormImplicits._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB
import com.lucidchart.open.nark.views.html.dashboards.dashboard

class DashboardModel extends AppModel {
	protected val tableName = "dashboards"
	protected val fields = "`id`, `name`, `url`, `created`, `user_id`, `deleted`"

	protected val dashboardsRowParser = {
		get[UUID]("id") ~
		get[String]("name") ~
		get[String]("url") ~
		get[Date]("created") ~
		get[UUID]("user_id") ~
		get[Int]("deleted") map {
			case id ~ name ~ url ~ created ~ user_id ~ deleted =>
				new Dashboard(id, name, url, created, user_id, (deleted == 1))
		}
	}
	
	/**
	 * Find the dashboard that has the matching id
	 * 
	 * @param id
	 * @return dashboard
	 */
	def findDashboardByID(id: UUID): Option[Dashboard] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + fields + """
				FROM """ + tableName + """
				WHERE `id` = {id}
				LIMIT 1
			""").on(
				"id" -> id
			).as(dashboardsRowParser.singleOpt)(connection)
		}
	}

	/**
	 * Find the dashboard that has the matching url
	 *
	 * @param url
	 * @return dashboard
	 */
	def findDashboardByURL(url: String): Option[Dashboard] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + fields + """
				FROM """ + tableName + """
				WHERE `url` = {url}
				LIMIT 1
			""").on(
				"url" -> url
			).as(dashboardsRowParser.singleOpt)(connection)
		}
	}

	/**
	 * Search for dashboards by name
	 */
	def searchByName(name: String): List[Dashboard] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + fields + """
				FROM """ + tableName + """
				WHERE `name` LIKE {name}
				LIMIT {limit}
			""").on(
				"name" -> ("%" + name + "%"),
				"limit" -> configuration.getInt("search.limit").get
			).as(dashboardsRowParser *)(connection)
		}
	}

	/**
	 * Find all the dashboards
	 */
	def findAll() : List[Dashboard] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + fields + """
				FROM """ + tableName + """
			""").as(dashboardsRowParser *)(connection)
		}
	}

	/**
	 * toggle the activation status of the dashboard
	 * @param dashboard
	 */
	def toggleActivation(dashboard: Dashboard) {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE """ + tableName + """ SET `deleted` = {deleted}
				WHERE id = {id}
			""").on(
				"id"         -> dashboard.id,
				"deleted"    -> !dashboard.deleted
			).executeUpdate()(connection)
		}
	}

	/**
	 * Create a new dashboard using all the details from the dashboard object.
	 * Throws an exception on failure
	 * 
	 * @param dashboard
	 */
	def createDashboard(dashboard: Dashboard) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO """ + tableName + """ (`id`, `name`, `url`, `created`, `user_id`, `deleted`)
				VALUES ({id}, {name}, {url}, {created}, {user_id}, {deleted})
				ON DUPLICATE KEY UPDATE `name`= {name}, `url`={url}""").on(
				"id"         -> dashboard.id,
				"name"       -> dashboard.name,
				"url"        -> dashboard.url,
				"created"    -> new Date(),
				"user_id"    -> dashboard.userId,
				"deleted"    -> dashboard.deleted
			).executeUpdate()(connection)
		}
	}
}

object DashboardModel extends DashboardModel