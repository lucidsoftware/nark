package com.lucidchart.open.nark

import com.lucidchart.open.nark.utils.StatsD
import com.lucidchart.open.nark.offline.HostDiscoverer
import com.lucidchart.open.nark.request.KeepFlashFilter
import com.lucidchart.open.nark.request.AppRequest
import com.lucidchart.open.nark.jobs.alerts.AlertMaster
import com.lucidchart.open.nark.jobs.dynamic.AlertPropagator

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.concurrent.Akka
import play.filters.csrf.CSRFFilter
import akka.actor.Props
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
		AlertMaster.schedule()
		AlertPropagator.schedule()
	}
	
	override def onError(request: RequestHeader, e: Throwable) = {
		if (Play.isProd) {
			Future.successful(error500(request, Some(e)))
		}
		else {
			super.onError(request, e)
		}
	}
	
	override def onHandlerNotFound(request: RequestHeader) = {
		if (Play.isProd) {
			Future.successful(error404(request))
		}
		else {
			super.onHandlerNotFound(request)
		}
	}
	
	override def onRequestCompletion(request: RequestHeader) {
		super.onRequestCompletion(request)
		StatsD.increment("requests")
	}

	/**
	 * Generic 500 Error Page
	 */
	def error500(request: RequestHeader, errorOption: Option[Throwable]) = {
		StatsD.increment("global500." + errorStatsKey(request) + "." + request.method.toUpperCase())
		StatsD.increment("global500-total")

		errorOption match {
			case Some(error) => Logger.error("Uncaught error on " + request.method.toUpperCase + " " + request.uri, error)
			case _ => Logger.error("Uncaught, unknown error on " + request.method.toUpperCase + " " + request.uri)
		}

		InternalServerError(views.html.errors.error500(errorOption)(request))
	}

	/**
	 * Generic 404 Error Page
	 */
	def error404(request: RequestHeader) = {
		StatsD.increment("global404." + errorStatsKey(request) + "." + request.method.toUpperCase())
		StatsD.increment("global404-total")
		NotFound(views.html.errors.error404()(request))
	}
}
