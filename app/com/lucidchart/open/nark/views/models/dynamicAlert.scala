package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.models.records.DynamicAlert

import play.api.libs.json.Json

object dynamicAlert {
	def apply(obj: DynamicAlert) = Json.obj(
		"id" -> obj.id.toString,
		"name" -> obj.name,
		"userId" -> obj.userId.toString,
		"searchTarget" -> obj.searchTarget,
		"matchExpr" -> obj.matchExpr,
		"buildTarget" -> obj.buildTarget,
		"comparison" -> obj.comparison.toString,
		"active" -> obj.active,
		"deleted" -> obj.deleted,
		"created" -> obj.created.getTime,
		"frequency" -> obj.frequency,
		"warnThreshold" -> obj.warnThreshold,
		"errorThreshold" -> obj.errorThreshold
	)
}
