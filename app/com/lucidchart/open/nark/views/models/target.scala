package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.models.records.Target

import play.api.libs.json.Json

object target {
	def apply(obj: Target) = Json.obj(
		"id" -> obj.id.toString,
		"name" -> obj.name,
		"graphId" -> obj.graphId.toString,
		"target" -> obj.target,
		"summarizer" -> obj.summarizer.toString,
		"deleted" -> obj.deleted
	)
}
