package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.Target
import com.lucidchart.open.nark.models.records.TargetSummarizer
import anorm.SqlParser._
import java.util.UUID
import anorm._
import play.api.db.DB
import play.api.Play.current
import AnormImplicits._

class TargetModel extends AppModel {
	protected val targetsRowParser = {
		get[UUID]("id") ~
		get[UUID]("graph_id") ~
		get[String]("name") ~
		get[String]("target") ~
		get[Boolean]("deleted") ~
		get[Int]("summarizer") map {
			case id ~ graphId ~ name ~ target ~ deleted ~ summarizer =>
				new Target(id, graphId, name, target, TargetSummarizer(summarizer), deleted)
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
				SELECT *
				FROM `graph_targets`
				WHERE `id` = {id}
				LIMIT 1
			""").on(
				"id" -> id
			).as(targetsRowParser.singleOpt)(connection)
		}
	}

	/**
	 * Find the Targets that have the matching graphId
	 *
	 * @param graphId
	 * @return Targets
	 */
	def findTargetByGraphId(graphId: UUID): List[Target] = findTargetByGraphId(List(graphId))

	/**
	 * Find the Targets that have the matching graphId
	 *
	 * @param graphIds
	 * @return Targets
	 */
	def findTargetByGraphId(graphIds: List[UUID]): List[Target] = {
		if (graphIds.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT *
					FROM `graph_targets`
					WHERE `graph_id` IN ({graph_ids})
				""").onList(
					"graph_ids" -> graphIds
				).toSQL.as(targetsRowParser *)(connection)
			}
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
				INSERT INTO `graph_targets` (`id`, `graph_id`, `name`, `target`, `deleted`, `summarizer`)
				VALUES ({id}, {graph_id}, {name}, {target}, {deleted}, {summarizer})
			""").on(
				"id"         -> target.id,
				"graph_id"   -> target.graphId,
				"name"       -> target.name,
				"target"     -> target.target,
				"deleted"    -> target.deleted,
				"summarizer" -> target.summarizer.id
			).executeUpdate()(connection)
		}
	}

	/**
	 * Edit an existing target. Not all values may be edited.
	 * Throws an exception on failure
	 *
	 * @param target
	 */
	def editTarget(target: Target) {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE `graph_targets` SET `name` = {name}, `target` = {target}, `deleted` = {deleted}, `summarizer` = {summarizer} WHERE `id` = {id}
			""").on(
				"id"         -> target.id,
				"name"       -> target.name,
				"target"     -> target.target,
				"deleted"    -> target.deleted,
				"summarizer" -> target.summarizer.id
			).executeUpdate()(connection)
		}
	}
}

object TargetModel extends TargetModel