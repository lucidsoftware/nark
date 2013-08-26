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
			case id ~ name ~ url ~ created ~ userId ~ deleted =>
				new Dashboard(id, name, url, created, userId, deleted)
		}
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
		DB.withConnection("main") { connection =>
			RichSQL("""
				SELECT *
				FROM `dashboards`
				WHERE `id` in ({ids})
			""").onList(
				"ids" -> ids
			).toSQL.as(dashboardsRowParser *)(connection)
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
	def search(name: String, page: Int) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `dashboards`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
			""").on(
				"name" -> name
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `dashboards`
				WHERE `name` LIKE {name} AND `deleted` = FALSE
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"name" -> name,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(dashboardsRowParser *)(connection)

			(found, matches)
		}
	}

	/**
	 * Get all deleted dashboards for a user
	 * @param user_id
	 * @return dashboards
	 */
	def searchDeleted(userId: UUID, name: String, page: Int) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `dashboards`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
			""").on(
				"name" -> name,
				"user_id" -> userId
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `dashboards`
				WHERE `name` LIKE {name} AND `deleted` = TRUE AND `user_id` = {user_id}
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"name" -> name,
				"user_id" -> userId,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(dashboardsRowParser *)(connection)

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