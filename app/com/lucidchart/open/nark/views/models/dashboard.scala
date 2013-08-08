package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.models.records.Dashboard

import play.api.libs.json.Json

object dashboard {
	def apply(obj: Dashboard) = Json.obj(
		"id" -> obj.id.toString,
		"name" -> obj.name,
		"url" -> obj.url,
		"userId" -> obj.userId.toString,
		"deleted" -> obj.deleted
	)
}
