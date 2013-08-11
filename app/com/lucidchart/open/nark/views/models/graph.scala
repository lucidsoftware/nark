package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.models.records.Graph

import play.api.libs.json.Json

object graph {
	def apply(obj: Graph) = Json.obj(
		"id" -> obj.id.toString,
		"name" -> obj.name,
		"dashboardId" -> obj.dashboardId.toString,
		"sort" -> obj.sort,
		"type" -> obj.typeGraph.toString,
		"axisLabel" -> obj.axisLabel.toString,
		"deleted" -> obj.deleted
	)
}
