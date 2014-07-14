package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.Target
import com.lucidchart.open.nark.models.records.TargetSummarizer
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._
import java.util.UUID
import play.api.db.DB
import play.api.Play.current

class TargetModel extends AppModel {
	protected val targetsRowParser = RowParser { row =>
		Target(
			row.uuid("id"),
			row.uuid("graph_id"),
			row.string("name"),
			row.string("target"),
			TargetSummarizer(row.int("summarizer")),
			row.bool("deleted")
		)
	}

	/**
	 * Find the target that has the matching id
	 *
	 * @param id
	 * @return target
	 */
	def findTargetByID(id: UUID): Option[Target] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `graph_targets`
				WHERE `id` = {id}
				LIMIT 1
			""").on { implicit query =>
				uuid("id", id)
			}.asSingleOption(targetsRowParser)
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
			DB.withConnection("main") { implicit connection =>
				SQL("""
					SELECT *
					FROM `graph_targets`
					WHERE `graph_id` IN ({graph_ids})
				""").expand { implicit query =>
					commaSeparated("graph_ids", graphIds.size)
				}.on { implicit query =>
					uuids("graph_ids", graphIds)
				}.asList(targetsRowParser)
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
		DB.withConnection("main") { implicit connection =>
			SQL("""
				INSERT INTO `graph_targets` (`id`, `graph_id`, `name`, `target`, `deleted`, `summarizer`)
				VALUES ({id}, {graph_id}, {name}, {target}, {deleted}, {summarizer})
			""").on { implicit query =>
				uuid("id", target.id)
				uuid("graph_id", target.graphId)
				string("name", target.name)
				string("target", target.target)
				bool("deleted", target.deleted)
				int("summarizer", target.summarizer.id)
			}.executeUpdate()
		}
	}

	/**
	 * Edit an existing target. Not all values may be edited.
	 * Throws an exception on failure
	 *
	 * @param target
	 */
	def editTarget(target: Target) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				UPDATE `graph_targets` SET `name` = {name}, `target` = {target}, `deleted` = {deleted}, `summarizer` = {summarizer} WHERE `id` = {id}
			""").on { implicit query =>
				uuid("id", target.id)
				string("name", target.name)
				string("target", target.target)
				bool("deleted", target.deleted)
				int("summarizer", target.summarizer.id)
			}.executeUpdate()
		}
	}
}

object TargetModel extends TargetModel
