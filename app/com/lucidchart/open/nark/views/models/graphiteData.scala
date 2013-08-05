package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.utils.GraphiteData

import play.api.libs.json.Json

object graphiteData {
	def apply(obj: GraphiteData) = Json.toJson(
		obj.targets.map(graphiteTarget(_))
	)
}
