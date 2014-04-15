package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.models.{AlertModel, AlertHistoryModel, AlertTagModel, SubscriptionModel, TagConverter, UserModel}
import com.lucidchart.open.nark.models.records.{Alert, AlertHistory, Comparisons, AlertState, Pagination, TagMap}
import com.lucidchart.open.nark.request.{AlertAction, AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.Global
import java.util.UUID
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.Play.configuration
import play.api.Play.current
import scala.math.BigDecimal
import validation.Constraints

object AlertsController extends AppController {
	private case class AlertFormSubmission(
		name: String,
		tags: List[String],
		target: String,
		errorThreshold: BigDecimal,
		warnThreshold: BigDecimal,
		comparison: Comparisons.Value,
		frequency: Int,
		dataSeconds: Int,
		dropNullPoints: Int,
		dropNullTargets: Boolean
	)

	private case class EditFormSubmission(
		name: String,
		tags: List[String],
		target: String,
		errorThreshold: BigDecimal,
		warnThreshold: BigDecimal,
		comparison: Comparisons.Value,
		frequency: Int,
		active: Boolean,
		dataSeconds: Int,
		dropNullPoints: Int,
		dropNullTargets: Boolean
	)

	private val createAlertForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"tags" -> text.verifying("Max 25 characters per tag.", tags => tags.length == 0 || tags.split(",").filter(_.length > 25).isEmpty)
			              .verifying(Constraints.pattern("^[a-zA-Z0-9\\.\\-_,]*$".r, error = "Only alpha-numberic text and periods (.), dashes (-), and underscores (_) allowed"))
			              .transform[List[String]](str => if(str.length == 0) List[String]() else str.split(",").map(_.trim.toLowerCase).toList, _.mkString(",")),
			"target" -> text,
			"error_threshold" -> bigDecimal,
			"warn_threshold" -> bigDecimal,
			"comparison" -> number.verifying("Invalid comparison type", x => Comparisons.values.map(_.id).contains(x)).transform[Comparisons.Value](Comparisons(_), _.id),
			"frequency" -> number.verifying(Constraints.min(1)),
			"data_seconds" -> number.verifying("Must be positive", x => x > 0),
			"drop_null_points" -> number.verifying("Must be positive", x => x >= 0),
			"drop_null_targets" -> boolean
		)(AlertFormSubmission.apply)(AlertFormSubmission.unapply)
	)

	private val editAlertForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"tags" -> text.verifying("Max 25 characters per tag.", tags => tags.length == 0 || tags.split(",").filter(_.length > 25).isEmpty)
			              .verifying(Constraints.pattern("^[a-zA-Z0-9\\.\\-_,]*$".r, error = "Only alpha-numberic text and periods (.), dashes (-), and underscores (_) allowed"))
			              .transform[List[String]](str => if(str.length == 0) List[String]() else str.split(",").map(_.trim.toLowerCase).toList, _.mkString(",")),
			"target" -> text,
			"error_threshold" -> bigDecimal,
			"warn_threshold" -> bigDecimal,
			"comparison" -> number.verifying("Invalid comparison type", x => Comparisons.values.map(_.id).contains(x)).transform[Comparisons.Value](Comparisons(_), _.id),
			"frequency" -> number.verifying(Constraints.min(1)),
			"active" -> boolean,
			"data_seconds" -> number.verifying("Must be positive", x => x > 0),
			"drop_null_points" -> number.verifying("Must be positive", x => x >= 0),
			"drop_null_targets" -> boolean
		)(EditFormSubmission.apply)(EditFormSubmission.unapply)
	)

	/**
	 * Create a new alert
	 */
	def create = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val form = createAlertForm.fill(AlertFormSubmission("", Nil, "", 0, 0, Comparisons.<, 60, configuration.getInt("alerts.secondsToCheckData").get, 1, true))
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
						data.errorThreshold,
						data.dataSeconds,
						data.dropNullPoints,
						data.dropNullTargets
					)

					AlertModel.createAlert(alert)
					AlertTagModel.updateTagsForAlert(alert.id, data.tags)
					Redirect(routes.AlertsController.view(alert.id)).flashing(AppFlash.success("Alert was successfully created."))
				}
			)
		}
	}

	/**
	 * Search for a particular alert
	 * @param term the search term to use when looking for alerts
	 */
	def search(term: String, page: Int) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val realPage = page.max(1)
			val (found, matches) = AlertModel.search(term, realPage - 1)
			val tags = TagConverter.toTagMap(AlertTagModel.findTagsForAlert(matches.map{_.id}))
			Ok(views.html.alerts.search(term, Pagination[Alert](realPage, found, AlertModel.configuredLimit, matches), tags))
		}
	}

	/**
	 * View a particular alert
	 * @param id the id of the alert to view
	 * @param page the page of alert histories to view
	 */
	def view(alertId: UUID, page: Int) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AlertAction.existingAlert(alertId) { alert =>
			AppAction { implicit request =>
				val creator = UserModel.findUserByID(alert.userId)
				val tags = AlertTagModel.findTagsForAlert(alert.id)
				val subscriptions = SubscriptionModel.getSubscriptionsByAlert(alertId)
				val realPage = page.max(1)
				val (found, histories) = AlertHistoryModel.getAlertHistory(alertId, realPage - 1)
				Ok(views.html.alerts.view(alert, tags, creator, subscriptions, Pagination[AlertHistory](realPage, found, AlertHistoryModel.configuredLimit, histories)))
			}
		}
	}

	/**
	 * Edit a particular alert
	 * @param id the id of the alert to edit
	 */
	def edit(alertId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AlertAction.alertManagementAccess(alertId, user.id) { alert =>
			AppAction { implicit request =>
				//disallow management of propagated alerts
				if (alert.dynamicAlertId.isDefined) {
					Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.error("Alerts propagated by dynamic alerts may not be managed."))
				}
				else {
					val form = editAlertForm.fill(EditFormSubmission(
						alert.name,
						AlertTagModel.findTagsForAlert(alert.id).map(_.tag),
						alert.target,
						alert.errorThreshold,
						alert.warnThreshold,
						alert.comparison,
						alert.frequency,
						alert.active,
						alert.dataSeconds,
						alert.dropNullPoints,
						alert.dropNullTargets
					))
					Ok(views.html.alerts.edit(form, alertId))
				}
			}
		}
	}

	/**
	 * Edit a particular alert
	 * @param id the id of the alert to edit
	 */
	def editSubmit(alertId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AlertAction.alertManagementAccess(alertId, user.id) { alert =>
			AppAction { implicit request =>
				editAlertForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.alerts.edit(formWithErrors, alertId))
					},
					data => {
						val editedAlert = alert.copy(
							name = data.name,
							target = data.target,
							errorThreshold = data.errorThreshold,
							warnThreshold = data.warnThreshold,
							comparison = data.comparison,
							frequency = data.frequency,
							active = data.active,
							dataSeconds = data.dataSeconds,
							dropNullPoints = data.dropNullPoints,
							dropNullTargets = data.dropNullTargets
						)
						
						AlertModel.editAlert(editedAlert)
						AlertTagModel.updateTagsForAlert(editedAlert.id, data.tags)
						Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.success("Alert was saved successfully."))
					}
				)
			}
		}
	}

	/**
	 * Deleted alerts
	 */
	def deleted(term: String, page: Int) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val realPage = page.max(1)
			val (found, matches) = AlertModel.searchDeleted(user.id, term, realPage - 1)
			val tags = TagConverter.toTagMap(AlertTagModel.findTagsForAlert(matches.map(_.id)))
			Ok(views.html.alerts.deleted(term, Pagination[Alert](realPage, found, AlertModel.configuredLimit, matches), tags))
		}
	}

	/**
	 * Recover an alert
	 */
	def recover(alertId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AlertAction.alertManagementAccess(alertId, user.id, allowDeleted = true) { alert =>
			AppAction { implicit request =>
				AlertModel.editAlert(alert.copy(deleted = false))
				Redirect(routes.AlertsController.edit(alertId)).flashing(AppFlash.success("Alert was recovered successfully."))
			}
		}
	}

	/**
	 * Delete an alert
	 */
	def delete(alertId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AlertAction.alertManagementAccess(alertId, user.id) { alert =>
			AppAction { implicit request =>
				AlertModel.editAlert(alert.copy(deleted = true))
				Redirect(routes.HomeController.index()).flashing(AppFlash.success("Alert was deleted successfully."))
			}
		}
	}
}
