package com.lucidchart.open.nark.models

import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._

import java.util.UUID

import records.Graph
import records.GraphType
import records.GraphAxisLabel

import play.api.Play.current
import play.api.db.DB

class GraphModel extends AppModel {
	protected val graphsRowParser = RowParser { row =>
		Graph(
			row.uuid("id"),
			row.string("name"),
			row.uuid("dashboard_id"),
			row.int("sort"),
			GraphType(row.int("type")),
			GraphAxisLabel(row.int("axis_label")),
			row.bool("deleted")
		)
	}
	
	/**
	 * Find the graph that has the matching id
	 * 
	 * @param id
	 * @return graph
	 */
	def findGraphByID(id: UUID): Option[Graph] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `graphs`
				WHERE `id` = {id}
				LIMIT 1
			""").on { implicit query =>
				uuid("id", id)
			}.asSingleOption(graphsRowParser)
		}
	}

	/**
	 * Find the dashboard that has the matching url
	 *
	 * @param dashboardId
	 * @return List of graphs
	 */
	def findGraphsByDashboardId(dashboardId: UUID): List[Graph] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `graphs`
				WHERE `dashboard_id` = {dashboard_id}
			""").on { implicit query =>
				uuid("dashboard_id", dashboardId)
			}.asList(graphsRowParser)
		}
	}

	/**
	 * Create a new graph using all the details from the graph object.
	 * Throws an exception on failure
	 * 
	 * @param graph
	 */
	def createGraph(graph: Graph) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				INSERT INTO `graphs` (`id`, `name`, `dashboard_id`, `sort`, `type`, `axis_label`, `deleted`)
				VALUES ({id}, {name}, {dashboard_id}, {sort}, {type}, {axis_label}, {deleted})
			""").on { implicit query =>
				uuid("id", graph.id)
				string("name", graph.name)
				uuid("dashboard_id", graph.dashboardId)
				int("sort", graph.sort)
				int("type", graph.typeGraph.id)
				int("axis_label", graph.axisLabel.id)
				bool("deleted", graph.deleted)
			}.executeUpdate()
		}
	}

	/**
	 * Edit an existing graph. Not all fields may be updated.
	 * Throws an exception on failure
	 * 
	 * @param graph
	 */
	def editGraph(graph: Graph) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				UPDATE `graphs` SET
				`name` = {name},
				`sort` = {sort},
				`type` = {type},
				`axis_label` = {axis_label},
				`deleted` = {deleted}
				WHERE `id` = {id}
			""").on { implicit query =>
				uuid("id", graph.id)
				string("name", graph.name)
				int("sort", graph.sort)
				int("type", graph.typeGraph.id)
				int("axis_label", graph.axisLabel.id)
				bool("deleted", graph.deleted)
			}.executeUpdate()
		}
	}
}

object GraphModel extends GraphModel
