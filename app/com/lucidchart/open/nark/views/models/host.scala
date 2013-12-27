package com.lucidchart.open.nark.views.models

import com.lucidchart.open.nark.models.records.Host

import play.api.libs.json.Json

object host {
	def apply(obj: Host) = Json.obj(
		"n" -> obj.name,
		"c" -> obj.lastConfirmed.getTime,
		"s" -> obj.state.toString
	)
}
