package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.models.records.Alert

import play.api.libs.json.Json

object alert {
	def apply(obj: Alert) = Json.obj(
		"id" -> obj.id.toString,
		"name" -> obj.name,
		"userId" -> obj.userId.toString,
		"target" -> obj.target,
		"comparison" -> obj.comparison.toString,
		"dynamicAlertId" -> obj.dynamicAlertId.map(_.toString),
		"active" -> obj.active,
		"deleted" -> obj.deleted,
		"created" -> obj.created.getTime,
		"updated" -> obj.updated.getTime,
		"lastChecked" -> obj.lastChecked.getTime,
		"nextCheck" -> obj.nextCheck.getTime,
		"frequency" -> obj.frequency,
		"warnThreshold" -> obj.warnThreshold,
		"errorThreshold" -> obj.errorThreshold,
		"worstState" -> obj.worstState.toString,
		"consecutiveFailures" -> obj.consecutiveFailures
	)
}
