package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.utils.GraphiteMetricData

import play.api.libs.json.Json

object graphiteMetricData {
	def apply(obj: GraphiteMetricData) = Json.toJson(
		obj.metrics.map(graphiteMetricItem(_))
	)
}
