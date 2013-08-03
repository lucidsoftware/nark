package com.lucidchart.open.nark.models

import java.util.UUID

import records.Graph

import AnormImplicits._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB

object GraphTypes extends Enumeration {
	val NORMAL = Value(0,"Normal")
	val STACKED = Value(1,"Stacked")
}

class GraphModel extends AppModel {
	protected val tableName = "graphs"
	protected val fields = "`id`, `name`, `dashboard_id`, `sort`, `type`, `user_id`, `deleted`"

	protected val graphsRowParser = {
		get[UUID]("id") ~
		get[String]("name") ~
		get[UUID]("dashboard_id") ~
		get[Int]("sort") ~
		get[Int]("type") ~
		get[UUID]("user_id") ~
		get[Int]("deleted") map {
			case id ~ name ~ dashboard_id ~ sort ~ type_graph ~ user_id ~ deleted =>
				new Graph(id, name, dashboard_id, sort, GraphTypes(type_graph), user_id, (deleted == 1))
		}
	}
	
	/**
	 * Find the graph that has the matching id
	 * 
	 * @param id
	 * @return graph
	 */
	def findGraphByID(id: UUID): Option[Graph] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + fields + """
				FROM """ + tableName + """
				WHERE `id` = {id}
				LIMIT 1
			""").on(
				"id" -> id
			).as(graphsRowParser.singleOpt)(connection)
		}
	}

	/**
	 * Find the dashboard that has the matching url
	 *
	 * @param dashboardId
	 * @return List of graphs
	 */
	def findGraphsByDashboardId(dashboardId: UUID): List[Graph] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + fields + """
				FROM """ + tableName + """
				WHERE `dashboard_id` = {dashboard_id}
			""").on(
				"dashboard_id" -> dashboardId
			).as(graphsRowParser *)(connection)
		}
	}

	/**
	 * Find all the graphs
	 */
	def findAll() : List[Graph] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + fields + """
				FROM """ + tableName + """
			""").as(graphsRowParser *)(connection)
		}
	}

	/**
	 * toggle the activation status of the graph
	 * @param graph
	 */
	def toggleActivation(graph: Graph) {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE """ + tableName + """ SET `deleted` = {deleted}
				WHERE id = {id}
			""").on(
				"id"         -> graph.id,
				"deleted"    -> !graph.deleted
			).executeUpdate()(connection)
		}
	}

	/**
	 * Create a new graph using all the details from the graph object.
	 * Throws an exception on failure
	 * 
	 * @param graph
	 */
	def createGraph(graph: Graph) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO """ + tableName + """ (`id`, `name`, `dashboard_id`, `sort`, `type`, `user_id`, `deleted`)
				VALUES ({id}, {name}, {dashboard_id}, {sort}, {type}, {user_id}, {deleted})
				ON DUPLICATE KEY UPDATE `name`= {name}, `type`={type}""").on(
				"id"         -> graph.id,
				"name"       -> graph.name,
				"dashboard_id" -> graph.dashboardId,
				"sort"       -> graph.sort,
				"type"       -> graph.typeGraph.id,
				"user_id"    -> graph.userId,
				"deleted"    -> graph.deleted
			).executeUpdate()(connection)
		}
	}
}

object GraphModel extends GraphModel