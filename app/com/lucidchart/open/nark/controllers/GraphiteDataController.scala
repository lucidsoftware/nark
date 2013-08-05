package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.utils.Graphite
import com.lucidchart.open.nark.models.HostModel
import com.lucidchart.open.nark.forms.Forms
import java.util.Date
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.data.validation.Constraints

class GraphiteDataController extends AppController {
	private case class DataPointsFormSubmission(
		targets: List[String],
		secondsOption: Option[Int],
		fromOption: Option[Date],
		toOption: Option[Date]
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
			"from" -> optional(Forms.internetDate),
			"to" -> optional(Forms.internetDate)
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
		dataPointsForm.bindFromRequest().fold(
			formWithErrors => {
				Logger.error("Graphite data request failed with errors: " + formWithErrors.errors.toString)
				BadRequest
			},
			data => {
				val returnedData = data.secondsOption match {
					case Some(seconds) => {
						Graphite.data(data.targets, seconds)
					}
					case None => {
						val from = data.fromOption.get
						val to = data.toOption.get
						Graphite.data(data.targets, from, to)
					}
				}

				val filteredReturnedData = returnedData.filterEmptyTargets()
				Ok(views.models.graphiteData(filteredReturnedData))
			}
		)
	}

	/**
	 * Search for metrics in graphite
	 */
	def metrics = AppAction { implicit request =>
		metricsForm.bindFromRequest().fold(
			formWithErrors => {
				Logger.error("Graphite metric request failed with errors: " + formWithErrors.errors.toString)
				BadRequest
			},
			data => {
				val metrics = Graphite.metrics(data.target)
				Ok(views.models.graphiteMetricData(metrics))
			}
		)
	}

	/**
	 * Search for graphite hosts in the host cache
	 */
	def hosts = AppAction { implicit request =>
		hostForm.bindFromRequest().fold(
			formWithErrors => {
				BadRequest
			},
			data => {
				val hosts = HostModel.findAllByName(data.search)
				Ok(Json.toJson(hosts.map(views.models.host(_))))
			}
		)
	}
}

object GraphiteDataController extends GraphiteDataController
