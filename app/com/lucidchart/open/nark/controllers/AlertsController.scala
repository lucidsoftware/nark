package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.models.{AlertModel, SubscriptionModel, UserModel}
import com.lucidchart.open.nark.models.records.{Alert, Comparisons, AlertState}
import com.lucidchart.open.nark.request.{AlertAction, AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import java.util.UUID
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import scala.math.BigDecimal
import validation.Constraints

object AlertsController extends AppController {
	private case class AlertFormSubmission(
		name: String,
		target: String,
		errorThreshold: BigDecimal,
		warnThreshold: BigDecimal,
		comparison: Comparisons.Value,
		frequency: Int
	)

	private case class EditFormSubmission(
		name: String,
		target: String,
		errorThreshold: BigDecimal,
		warnThreshold: BigDecimal,
		comparison: Comparisons.Value,
		frequency: Int,
		active: Boolean
	)

	private val createAlertForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"target" -> text,
			"error_threshold" -> bigDecimal,
			"warn_threshold" -> bigDecimal,
			"comparison" -> number.verifying("Invalid comparison type", x => Comparisons.values.map(_.id).contains(x)).transform[Comparisons.Value](Comparisons(_), _.id),
			"frequency" -> number
		)(AlertFormSubmission.apply)(AlertFormSubmission.unapply)
	)

	private val editAlertForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"target" -> text,
			"error_threshold" -> bigDecimal,
			"warn_threshold" -> bigDecimal,
			"comparison" -> number.verifying("Invalid comparison type", x => Comparisons.values.map(_.id).contains(x)).transform[Comparisons.Value](Comparisons(_), _.id),
			"frequency" -> number,
			"active" -> boolean
		)(EditFormSubmission.apply)(EditFormSubmission.unapply)
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
					Redirect(routes.AlertsController.search("")).flashing(AppFlash.success("Alert was created successfully."))
				}
			)
		}
	}

	/**
	 * Search for a particular alert
	 * @param term the search term to use when looking for alerts
	 */
	def search(term: String) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val matches = AlertModel.searchByName(term).filter(_.active)
			Ok(views.html.alerts.search(term, matches))
		}
	}

	/**
	 * View a particular alert
	 * @param the id of the alert to view
	 */
	def view(id: UUID) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val alert = AlertModel.getAlert(id)

			if (alert.isDefined) {
				val creator = UserModel.findUserByID(alert.get.userId)
				val subscriptions = SubscriptionModel.getSubscriptionsByAlert(id)
				Ok(views.html.alerts.view(alert.get, creator.get, subscriptions))
			}
			else {
				Redirect(routes.HomeController.index()).flashing(AppFlash.error("Alert not found"))
			}
		}
	}

	/**
	 * Edit a particular alert
	 * @param the id of the alert to edit
	 */
	def edit(id: UUID) = AuthAction.authenticatedUser { implicit user =>
		AlertAction.alertManagementAccess(id, user.id) { (alert, ignored) =>
			AppAction { implicit request =>
				val form = editAlertForm.fill(EditFormSubmission(alert.name, alert.target, alert.errorThreshold, alert.warnThreshold, alert.comparison, alert.frequency, alert.active))
				Ok(views.html.alerts.edit(form, id))
			}
		}
	}

	/**
	 * Edit a particular alert
	 * @param the id of the alert to edit
	 */
	def editSubmit(id: UUID) = AuthAction.authenticatedUser { implicit user =>
		AlertAction.alertManagementAccess(id, user.id) { (alert, ignored) =>
			AppAction { implicit request =>
				editAlertForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.alerts.edit(formWithErrors, id))
					},
					data => {
						val editedAlert = alert.copy(
							name = data.name,
							target = data.target,
							errorThreshold = data.errorThreshold,
							warnThreshold = data.warnThreshold,
							comparison = data.comparison,
							frequency = data.frequency,
							active = data.active
						)

						AlertModel.editAlert(editedAlert)
						Redirect(routes.AlertsController.view(id)).flashing(AppFlash.success("Changes saved."))
					}
				)
			}
		}
	}
}