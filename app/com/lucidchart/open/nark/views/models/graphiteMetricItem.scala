package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.utils.GraphiteMetricItem

import play.api.libs.json.Json

object graphiteMetricItem {
	def apply(obj: GraphiteMetricItem) = Json.obj(
		"n" -> obj.name,
		"p" -> obj.path,
		"l" -> obj.leaf
	)
}
