package com.lucidchart.open.nark.models

import play.api.Play.current
import play.api.Play.configuration

trait AppModel {
	val configuredLimit = configuration.getInt("search.limit").get
}
