package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.utils.GraphiteTarget

import play.api.libs.json.Json

object graphiteTarget {
	def apply(obj: GraphiteTarget) = Json.obj(
		"t" -> obj.target,
		"d" -> obj.datapoints.map(graphiteDataPoint(_))
	)
}
