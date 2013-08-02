package com.lucidchart.open.nark

import com.lucidchart.open.nark.utils.StatsD
import com.lucidchart.open.nark.offline.HostDiscoverer
import com.lucidchart.open.nark.request.KeepFlashFilter

import play.api.Application
import play.api.GlobalSettings
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.mvc.Results._
import play.filters.csrf.CSRFFilter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import play.api.Play.current

object Global extends WithFilters(CSRFFilter(), KeepFlashFilter()) with GlobalSettings {
	protected def errorStatsKey(request: RequestHeader) = request.path.replaceAll("""[\/]""", ".") + "." + request.method.toLowerCase

	override def onStart(application: Application) {
		Akka.system.scheduler.scheduleOnce(1.seconds) {
			registerBackgroundJobs()
		}
	}

	private def registerBackgroundJobs() {
		Logger.info("Registering background jobs")
		HostDiscoverer.schedule()
	}
	
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
