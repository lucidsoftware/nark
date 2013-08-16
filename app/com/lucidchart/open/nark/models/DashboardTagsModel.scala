package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.DashboardTag
import com.lucidchart.open.nark.models.records.Dashboard

import java.util.UUID
import AnormImplicits._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.Play.configuration
import play.api.db.DB

class DashboardTagsModel extends AppModel {
	protected val tagsRowParser =  {
		get[UUID]("dashboard_id") ~
		get[String]("tag") map {
			case dashboardId ~ tag =>
				new DashboardTag(dashboardId, tag)
		}

	}

	/**
	 * Gets all tags.
	 * @returns tags
	 */
	def search(query:String) :List[String] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT `tag` 
				FROM `dashboard_tags` 
				WHERE `tag` LIKE {query}
				GROUP BY `tag`
				LIMIT {limit}
			""").on(
				"query" -> ("%" + query + "%"),
				"limit" -> configuration.getInt("search.limit")
			).as(get[String]("tag") *)(connection)
		}
	}

	/**
	 * Search all tags.
	 * @param searchTerm
	 * @returns dashboardTags
	 */
	// def search(query:String) :List[DashboardTag] = {
	// 	DB.withConnection("main") { connection =>
	// 		SQL("""
	// 			SELECT *
	// 			FROM `dashboard_tags`
	// 			WHERE `tag` LIKE {query}
	// 			ORDER BY `tag`
	// 			LIMIT {limit}
	// 		""").on(
	// 			"query" -> ("%" + query + "%"),
	// 			"limit" -> configuration.getInt("search.limit")
	// 		).as(tagsRowParser *)(connection)
	// 	}
	// }

	/**
	 * Gets all dashboards with this tag.
	 * @param tag
	 * @returns dashboardTags
	 */
	def findDashboardsWithTag(tag: String): List[DashboardTag] = {
		findDashboardsWithTag(List(tag))
	}

	/**
	 * Gets all dashboards with this tag.
	 * @param tag
	 * @returns dashboardTags
	 */
	def findDashboardsWithTag(tags: List[String]): List[DashboardTag] = {
		DB.withConnection("main") { connection =>
			RichSQL("""
				SELECT * 
				FROM `dashboard_tags` 
				WHERE `tag` in ({tags})
			""")
			.onList("tags" -> tags)
			.toSQL.on("limit" -> configuration.getInt("search.limit"))
			.as(tagsRowParser *)(connection)
		}	
	}


	/**
	 * Gets all tags for a dashbaord.
	 * @param dashbaordId
	 * @returns dashbaordTags
	 */
	def findTagsForDashboard(dashboardId: UUID) : List[DashboardTag] = {
		findTagsForDashboard(List(dashboardId))
	}

	/**
	 * Gets all tags for dashbaords.
	 * @param dashbaordId
	 * @returns tags
	 */
	def findTagsForDashboard(dashboardIds: List[UUID]) : List[DashboardTag] = {
		if(dashboardIds.isEmpty) {
			return List[DashboardTag]();
		}

		DB.withConnection("main") {connection =>
			RichSQL("""
				SELECT * 
				FROM `dashboard_tags` 
				WHERE `dashboard_id` IN ({dashboardIds})
				ORDER BY `dashboard_id`
				""")
			.onList("dashboardIds" -> dashboardIds)
			.toSQL.as(tagsRowParser *)(connection)
		}
	}

	/**
	 * @param dashbaordID
	 * @param tags
	 * Updates tags for a dashbaord. Removes any tags associate with the dashbaord 
	 * that are not in the list list. Adds tags in list to the dashboard, ignoring duplicates.
	 */
	def updateTagsForDashboard(dashboardId: UUID, tags:List[String]) {
		if(tags.isEmpty) {
			removeAllTagsForDashboard(dashboardId);
		} else {
			removeTagsForDashboard(dashboardId, tags, false)
			addTagsForDashboard(dashboardId, tags)
		}
	}

	/**
	 * @param dashboardId
	 * @param tags
	 * Adds these tags for a dashboard, ignoring duplicates.
	 */
	def addTagsForDashboard(dashboardId:UUID, tags:List[String]) {
		if(!tags.isEmpty) {
			DB.withConnection("main") { connection =>
				RichSQL("""

			 		INSERT IGNORE INTO `dashboard_tags` (`dashboard_id`, `tag`) 
			 		VALUES ({fields})

			 	""")
			 	.multiInsert(tags.size, Seq("dashboard_id", "tag"))(
			      "dashboard_id" -> tags.map(_ => toParameterValue(dashboardId)),
			      "tag" -> tags.map(x => toParameterValue(x)))
			 	.toSQL.executeUpdate()(connection)
			}
		}
	}

	/**
	 * Removes these tags from a dashboard.
	 * @param dashbaordID
	 * @param tags
	 * @param inList wheter to remove the tags in the list, or to remove the tags not in the list. By default removes tags in the list.
	 */
	def removeTagsForDashboard(dashboardId:UUID, tags:List[String], inList:Boolean=true) {
		DB.withConnection("main") { connection =>
			RichSQL("""
					DELETE
					FROM `dashboard_tags`
					WHERE `dashboard_id` = {dashboard_id} 
					AND `tag` """ + (if (inList)  "" else "NOT") + """ IN ({tags})
				""")
			.onList("tags" -> tags)
			.toSQL.on("dashboard_id" -> dashboardId)
			.executeUpdate()(connection)
		}
	}

	/**
	 * Removes all tags from a dashboard.
	 * @param dashbaordID
	 */
	def removeAllTagsForDashboard(dashboardId:UUID) {
		DB.withConnection("main") { connection =>
			SQL("""
					DELETE
					FROM `dashboard_tags`
					WHERE `dashboard_id` = {dashboard_id} 
				""")
			.on("dashboard_id" -> dashboardId)
			.executeUpdate()(connection)
		}
	}
}


object DashboardTagsModel extends DashboardTagsModel

object DashboardTagsConverter {
		def convertToDashboardMap(tags : List[DashboardTag]) : Map[UUID, List[String]] = {
		tags.foldLeft[Map[UUID,List[String]]](Map())((ret,dt) =>
	    	ret + (dt.dashboardId -> 
	    				(if(ret contains dt.dashboardId)
	    					(dt.tag :: ret.get(dt.dashboardId).get)
	    				else 
	    					List(dt.tag)
	    				))
		)
	}

	def convertToTagsMap(tags : List[DashboardTag], dashboards: List[Dashboard]) : Map[String, List[Dashboard]] = {
		tags.foldLeft[Map[String,List[Dashboard]]](Map()){(ret,dt) =>
			val dashboard = dashboards.find(_.id == dt.dashboardId)
			if(dashboard.isDefined) {
				ret + (dt.tag -> 
	    				(if(ret contains dt.tag)
	    					(dashboard.get :: ret.get(dt.tag).get)
	    				else 
	    					List(dashboard.get)
	    				))
			} else
				ret
			
		}
	}	
}