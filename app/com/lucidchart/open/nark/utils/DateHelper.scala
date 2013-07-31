package com.lucidchart.open.nark.utils

import java.text.SimpleDateFormat

object DateHelper {
	val rfc8601Formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
	val internetDateFormatter = rfc8601Formatter
}
