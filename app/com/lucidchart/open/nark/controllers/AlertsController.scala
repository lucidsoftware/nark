package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.models.AlertModel
import com.lucidchart.open.nark.models.records.{Alert, Comparisons, AlertState}
import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import validation.Constraints

object AlertsController extends AppController {
	private case class AlertFormSubmission(
		name: String,
		target: String,
		errorThreshold: Double,
		warnThreshold: Double,
		comparison: Comparisons.Value,
		frequency: Int
	)
	private val createAlertForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"target" -> text,
			"error_threshold" -> of[Double],
			"warn_threshold" -> of[Double],
			"comparison" -> number.verifying("Invalid comparison type", x => Comparisons.values.map(_.id).contains(x)).transform[Comparisons.Value](Comparisons(_), _.id),
			"frequency" -> number
		)(AlertFormSubmission.apply)(AlertFormSubmission.unapply)
	)

	/**
	 * Create a new alert
	 */
	def create = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val form = createAlertForm.fill(AlertFormSubmission("", "", 0, 0, Comparisons.<, 0))
			Ok(views.html.alerts.create(form))
		}
	}

	/**
	 * Handle the form submitted by the user and create the new alert
	*/
	def createSubmit = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			createAlertForm.bindFromRequest().fold(
				formWithErrors => {
					Ok(views.html.alerts.create(formWithErrors))
				},
				data => {
					val alert = new Alert(
						data.name,
						user.id,
						data.target,
						data.comparison,
						data.frequency,
						data.warnThreshold,
						data.errorThreshold
					)

					AlertModel.createAlert(alert)
					Redirect(routes.HomeController.index()).flashing(AppFlash.success("Alert was created successfully."))
				}
			)

		}
	}
}
