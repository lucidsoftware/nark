package com.lucidchart.open.nark

import com.lucidchart.open.nark.utils.StatsD
import com.lucidchart.open.nark.request.KeepFlashFilter

import play.api.GlobalSettings
import play.api.mvc._
import play.api.mvc.Results._
import play.filters.csrf.CSRFFilter

object Global extends WithFilters(CSRFFilter(), KeepFlashFilter()) with GlobalSettings {
	protected def errorStatsKey(request: RequestHeader) = request.path.replaceAll("""[\/]""", ".") + "." + request.method.toLowerCase
	
	override def onError(request: RequestHeader, e: Throwable) = {
		val result = super.onError(request, e)
		StatsD.increment("global500." + errorStatsKey(request) + "." + request.method.toUpperCase())
		StatsD.increment("global500-total")
		result
	}
	
	override def onHandlerNotFound(request: RequestHeader): Result = {
		val result = super.onHandlerNotFound(request)
		StatsD.increment("global404." + errorStatsKey(request) + "." + request.method.toUpperCase())
		StatsD.increment("global404-total")
		result
	}
	
	override def onRequestCompletion(request: RequestHeader) {
		super.onRequestCompletion(request)
		StatsD.increment("requests")
	}
}
