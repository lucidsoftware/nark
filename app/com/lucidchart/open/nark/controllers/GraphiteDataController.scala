package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.utils.Graphite
import com.lucidchart.open.nark.forms.Forms
import java.util.Date
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints

class GraphiteDataController extends AppController {
	private case class RetrieveFormSubmission(
		targets: List[String],
		secondsOption: Option[Int],
		fromOption: Option[Date],
		toOption: Option[Date]
	)

	private val retrieveForm = Form(
		mapping(
			"target" -> list(text).verifying { l => l.size > 0 },
			"seconds" -> optional(number.verifying(Constraints.min(1))),
			"from" -> optional(Forms.internetDate),
			"to" -> optional(Forms.internetDate)
		)(RetrieveFormSubmission.apply)(RetrieveFormSubmission.unapply).verifying { f =>
			f.secondsOption.isDefined || (f.fromOption.isDefined && f.toOption.isDefined)
		}
	)

	/**
	 * Get data from graphite and return it
	 */
	def retrieve = AppAction { implicit request =>
		retrieveForm.bindFromRequest().fold(
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
}

object GraphiteDataController extends GraphiteDataController
