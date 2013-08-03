package com.lucidchart.open.nark.models

import anorm.SqlParser._
import java.util.UUID
import anorm._
import play.api.db.DB
import anorm.~
import records.Target
import play.api.Play.current
import AnormImplicits._

class TargetModel extends AppModel {
	protected val tableName = "graph_targets"
	protected val fields = "`id`, `graph_id`, `target`, `deleted`"

	protected val targetsRowParser = {
		get[UUID]("id") ~
		get[UUID]("graph_id") ~
		get[String]("target") ~
		get[Int]("deleted") map {
			case id ~ graph_id ~ target ~ deleted =>
				new Target(id, graph_id, target, (deleted == 1))
		}
	}

	/**
	 * Find the target that has the matching id
	 *
	 * @param id
	 * @return target
	 */
	def findTargetByID(id: UUID): Option[Target] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + fields + """
				FROM """ + tableName + """
				WHERE `id` = {id}
				LIMIT 1
			""").on(
				"id" -> id
			).as(targetsRowParser.singleOpt)(connection)
		}
	}

	/**
	 * Find the Targets that has the matching graphId
	 *
	 * @param graphId
	 * @return Targets
	 */
	def findTargetByGraphId(graphId: UUID): List[Target] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + fields + """
				FROM """ + tableName + """
				WHERE `graph_id` = {graph_id}
			""").on(
				"graph_id" -> graphId
			).as(targetsRowParser *)(connection)
		}
	}

	/**
	 * Find all the targets
	 */
	def findAll() : List[Target] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + fields + """
				FROM """ + tableName + """
			""").as(targetsRowParser *)(connection)
		}
	}

	/**
	 * toggle the activation status of the target
	 * @param target
	 */
	def toggleActivation(target: Target) {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE """ + tableName + """ SET `deleted` = {deleted}
				WHERE id = {id}
			""").on(
				"id"         -> target.id,
				"deleted"    -> !target.deleted
			).executeUpdate()(connection)
		}
	}

	/**
	 * Create a new target using all the details from the target object.
	 * Throws an exception on failure
	 *
	 * @param target
	 */
	def createTarget(target: Target) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO """ + tableName + """ (`id`, `graph_id`, `target`, `deleted`)
				VALUES ({id}, {graph_id}, {target}, {deleted})
				ON DUPLICATE KEY UPDATE `target`= {target}""").on(
				"id"         -> target.id,
				"graph_id"   -> target.graphId,
				"target"     -> target.target,
				"deleted"    -> target.deleted
			).executeUpdate()(connection)
		}
	}
}

object TargetModel extends TargetModel