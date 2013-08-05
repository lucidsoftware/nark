package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.utils.GraphiteDataPoint

import play.api.libs.json.Json

object graphiteDataPoint {
	def apply(obj: GraphiteDataPoint) = Json.obj(
		"d" -> (obj.date.getTime / 1000),
		"v" -> obj.value
	)
}
