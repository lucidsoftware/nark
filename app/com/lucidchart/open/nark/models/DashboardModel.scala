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
	protected val dashboardsRowParser = {
		get[UUID]("id") ~
		get[String]("name") ~
		get[String]("url") ~
		get[Date]("created") ~
		get[UUID]("user_id") ~
		get[Boolean]("deleted") map {
			case id ~ name ~ url ~ created ~ user_id ~ deleted =>
				new Dashboard(id, name, url, created, user_id, deleted)
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
				SELECT *
				FROM `dashboards`
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
				SELECT *
				FROM `dashboards`
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
				SELECT *
				FROM `dashboards`
				WHERE `name` LIKE {name}
				AND `deleted` = false
				LIMIT {limit}
			""").on(
				"name" -> ("%" + name + "%"),
				"limit" -> configuration.getInt("search.limit").get
			).as(dashboardsRowParser *)(connection)
		}
	}

	/**
	 * Get all deleted dashboards for a user
	 * @param user_id
	 * @return dashboards
	 */
	def searchDeletedByName(user_id: UUID, name: String): List[Dashboard] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT *
				FROM `dashboards`
				WHERE `deleted` = true
				AND `user_id` = {user_id}
				AND `name` LIKE {name}
				LIMIT {limit}
			""").on(
				"user_id" -> user_id,
				"name" -> ("%" + name + "%"),
				"limit" -> configuration.getInt("search.limit").get
			).as(dashboardsRowParser *)(connection)
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
				INSERT INTO `dashboards` (`id`, `name`, `url`, `created`, `user_id`, `deleted`)
				VALUES ({id}, {name}, {url}, {created}, {user_id}, {deleted})
			""").on(
				"id"         -> dashboard.id,
				"name"       -> dashboard.name,
				"url"        -> dashboard.url,
				"created"    -> new Date(),
				"user_id"    -> dashboard.userId,
				"deleted"    -> dashboard.deleted
			).executeUpdate()(connection)
		}
	}

	/**
	 * Edit an existing dashboard. not all fields can be updated.
	 * Throws an exception on failure
	 * 
	 * @param dashboard
	 */
	def editDashboard(dashboard: Dashboard) {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE `dashboards` SET `name` = {name}, `url` = {url}, `deleted` = {deleted} WHERE `id` = {id}
			""").on(
				"id"         -> dashboard.id,
				"name"       -> dashboard.name,
				"url"        -> dashboard.url,
				"deleted"    -> dashboard.deleted
			).executeUpdate()(connection)
		}
	}
}

object DashboardModel extends DashboardModel