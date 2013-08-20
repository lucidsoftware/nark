package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.utils.Graphite
import com.lucidchart.open.nark.forms.Forms
import com.lucidchart.open.nark.utils.StatsD
import java.util.Date
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.data.validation.Constraints
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class GraphiteDataController extends AppController {
	private case class DataPointsFormSubmission(
		targets: List[String],
		secondsOption: Option[Int],
		fromOption: Option[Int],
		toOption: Option[Int]
	)

	private case class MetricsFormSubmission(
		target: String
	)

	private case class HostFormSubmission(
		search: String
	)

	private val dataPointsForm = Form(
		mapping(
			"target" -> list(text).verifying { l => l.size > 0 },
			"seconds" -> optional(number.verifying(Constraints.min(1))),
			"from" -> optional(number),
			"to" -> optional(number)
		)(DataPointsFormSubmission.apply)(DataPointsFormSubmission.unapply).verifying { f =>
			f.secondsOption.isDefined || (f.fromOption.isDefined && f.toOption.isDefined)
		}
	)

	private val metricsForm = Form(
		mapping(
			"target" -> text
		)(MetricsFormSubmission.apply)(MetricsFormSubmission.unapply)
	)

	private val hostForm = Form(
		mapping(
			"search" -> text
		)(HostFormSubmission.apply)(HostFormSubmission.unapply)
	)

	/**
	 * Get data from graphite and return it
	 */
	def dataPoints = AppAction { implicit request =>
		StatsD.increment("graphite.datapoints")
		dataPointsForm.bindFromRequest().fold(
			formWithErrors => {
				Logger.error("Graphite data request failed with errors: " + formWithErrors.errors.toString)
				BadRequest
			},
			data => {
				Async {
					val returnedDataFuture = data.secondsOption match {
						case Some(seconds) => {
							Graphite.data(data.targets, seconds)
						}
						case None => {
							val from = data.fromOption.get
							val to = data.toOption.get
							Graphite.data(data.targets, new Date(from * 1000L), new Date(to * 1000L))
						}
					}

					returnedDataFuture.map { returnedData =>
						val filteredReturnedData = returnedData.filterEmptyTargets()
						Ok(views.models.graphiteData(filteredReturnedData))
					}
				}
			}
		)
	}

	/**
	 * Search for metrics in graphite
	 */
	def metrics = AppAction { implicit request =>
		StatsD.increment("graphite.metrics")
		metricsForm.bindFromRequest().fold(
			formWithErrors => {
				Logger.error("Graphite metric request failed with errors: " + formWithErrors.errors.toString)
				BadRequest
			},
			data => {
				Async {
					Graphite.metrics(data.target).map { metrics =>
						Ok(views.models.graphiteMetricData(metrics))
					}
				}
			}
		)
	}
}

object GraphiteDataController extends GraphiteDataController
