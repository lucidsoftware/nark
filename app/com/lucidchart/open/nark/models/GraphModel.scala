package com.lucidchart.open.nark.models

import java.util.UUID

import records.Graph
import records.GraphType

import AnormImplicits._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB

class GraphModel extends AppModel {
	protected val graphsRowParser = {
		get[UUID]("id") ~
		get[String]("name") ~
		get[UUID]("dashboard_id") ~
		get[Int]("sort") ~
		get[Int]("type") ~
		get[Boolean]("deleted") map {
			case id ~ name ~ dashboard_id ~ sort ~ type_graph ~ deleted =>
				new Graph(id, name, dashboard_id, sort, GraphType(type_graph), deleted)
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
				SELECT *
				FROM `graphs`
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
				SELECT *
				FROM `graphs`
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
				SELECT *
				FROM `graphs`
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
				UPDATE `graphs` SET `deleted` = {deleted}
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
				INSERT INTO `graphs` (`id`, `name`, `dashboard_id`, `sort`, `type`, `deleted`)
				VALUES ({id}, {name}, {dashboard_id}, {sort}, {type}, {deleted})
				ON DUPLICATE KEY UPDATE `name`= {name}, `type`={type}""").on(
				"id"         -> graph.id,
				"name"       -> graph.name,
				"dashboard_id" -> graph.dashboardId,
				"sort"       -> graph.sort,
				"type"       -> graph.typeGraph.id,
				"deleted"    -> graph.deleted
			).executeUpdate()(connection)
		}
	}
}

object GraphModel extends GraphModel