package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.Global
import com.lucidchart.open.nark.models.{AlertModel, DynamicAlertModel, DynamicAlertTagModel, SubscriptionModel, TagConverter, UserModel}
import com.lucidchart.open.nark.models.records.{Comparisons, DynamicAlert, Pagination}
import com.lucidchart.open.nark.request.{AppAction, AppFlash, AuthAction, DynamicAlertAction}
import com.lucidchart.open.nark.views

import java.util.UUID
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import scala.math.BigDecimal
import validation.Constraints

object DynamicAlertsController extends DynamicAlertsController
class DynamicAlertsController extends AppController {

	private case class CreateFormSubmission(
		name: String,
		tags: List[String],
		searchTarget: String,
		matchExpr: String,
		buildTarget: String,
		errorThreshold: BigDecimal,
		warnThreshold: BigDecimal,
		comparison: Comparisons.Value,
		frequency: Int
	)

	private case class EditFormSubmission(
		name: String,
		tags: List[String],
		searchTarget: String,
		matchExpr: String,
		buildTarget: String,
		errorThreshold: BigDecimal,
		warnThreshold: BigDecimal,
		comparison: Comparisons.Value,
		frequency: Int,
		active: Boolean
	)

	private val createAlertForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"tags" -> text.verifying("Max 25 characters per tag.", tags => tags.length == 0 || tags.split(",").filter(_.length > 25).isEmpty)
			              .verifying(Constraints.pattern("^[a-zA-Z0-9\\.\\-_,]*$".r, error = "Only alpha-numberic text and periods (.), dashes (-), and underscores (_) allowed"))
			              .transform[List[String]](str => if(str.length == 0) List[String]() else str.split(",").map(_.trim.toLowerCase).toList, _.mkString(",")),
			"search_target" -> text,
			"match_expr" -> text,
			"build_target" -> text,
			"error_threshold" -> bigDecimal,
			"warn_threshold" -> bigDecimal,
			"comparison" -> number.verifying("Invalid comparison type", x => Comparisons.values.map(_.id).contains(x)).transform[Comparisons.Value](Comparisons(_), _.id),
			"frequency" -> number.verifying(Constraints.min(1))
		)(CreateFormSubmission.apply)(CreateFormSubmission.unapply)
	)

	private val editAlertForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"tags" -> text.verifying("Max 25 characters per tag.", tags => tags.length == 0 || tags.split(",").filter(_.length > 25).isEmpty)
			              .verifying(Constraints.pattern("^[a-zA-Z0-9\\.\\-_,]*$".r, error = "Only alpha-numberic text and periods (.), dashes (-), and underscores (_) allowed"))
			              .transform[List[String]](str => if(str.length == 0) List[String]() else str.split(",").map(_.trim.toLowerCase).toList, _.mkString(",")),
			"search_target" -> text,
			"match_expr" -> text,
			"build_target" -> text,
			"error_threshold" -> bigDecimal,
			"warn_threshold" -> bigDecimal,
			"comparison" -> number.verifying("Invalid comparison type", x => Comparisons.values.map(_.id).contains(x)).transform[Comparisons.Value](Comparisons(_), _.id),
			"frequency" -> number.verifying(Constraints.min(1)),
			"active" -> boolean
		)(EditFormSubmission.apply)(EditFormSubmission.unapply)
	)

	/**
	 * Show the form for creating a dynamic alert
	 */
	def create() = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val form = createAlertForm.fill(CreateFormSubmission("", Nil, "", "", "", 0, 0, Comparisons.<, 60))
			Ok(views.html.dynamicalerts.create(form))
		}
	}

	/**
	 * Create the dynamic alert in the database
	 */
	def createSubmit() = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			createAlertForm.bindFromRequest().fold(
				formWithErrors => {
					Ok(views.html.dynamicalerts.create(formWithErrors))
				},
				data => {
					val alert = new DynamicAlert(
						data.name,
						user.id,
						data.searchTarget,
						data.matchExpr,
						data.buildTarget,
						data.comparison,
						data.frequency,
						data.warnThreshold,
						data.errorThreshold
					)
					DynamicAlertModel.createDynamicAlert(alert)
					DynamicAlertTagModel.updateTagsForAlert(alert.id, data.tags)

					Redirect(routes.DynamicAlertsController.view(alert.id)).flashing(AppFlash.success("Dynamic Alert created successfully."))
				}
			)
		}
	}

	/**
	 * Search for a particular dynamic alert
	 * @param term the search term to use when looking for alerts
	 * @param page the page number of results to show
	 */
	def search(term: String, page: Int) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val realPage = page.max(1)
			val (found, matches) = DynamicAlertModel.search(term, realPage - 1)
			val tags = TagConverter.toTagMap(DynamicAlertTagModel.findTagsForAlert(matches.map{_.id}))
			Ok(views.html.dynamicalerts.search(term, Pagination[DynamicAlert](realPage, found, DynamicAlertModel.configuredLimit, matches), tags))
		}
	}

	/**
	 * View a dynamic alert
	 * @param id the id of the dynamic alert to view
	 */
	def view(id: UUID) = AuthAction.maybeAuthenticatedUser { implicit user =>
		DynamicAlertAction.existingAlert(id) { alert =>
			AppAction { implicit request =>
				val creator = UserModel.findUserByID(alert.userId)
				val tags = DynamicAlertTagModel.findTagsForAlert(alert.id)
				val subscriptions = SubscriptionModel.getSubscriptionsByAlert(alert.id)
				val propagatedAlerts = AlertModel.findAlertByDynamicAlert(alert.id)
				Ok(views.html.dynamicalerts.view(alert, propagatedAlerts, creator, tags, subscriptions))
			}
		}
	}

	/**
	 * Edit a dynamic alert
	 * @param id the id of the dynamic alert to edit
	 */
	def edit(id: UUID) = AuthAction.authenticatedUser { implicit user =>
		DynamicAlertAction.alertManagementAccess(id, user.id) { alert =>
			AppAction { implicit request =>
				val form = editAlertForm.fill(EditFormSubmission(
					alert.name,
					DynamicAlertTagModel.findTagsForAlert(alert.id).map(_.tag),
					alert.searchTarget,
					alert.matchExpr,
					alert.buildTarget,
					alert.errorThreshold,
					alert.warnThreshold,
					alert.comparison,
					alert.frequency,
					alert.active
				))
				Ok(views.html.dynamicalerts.edit(form, id))
			}
		}
	}

	/**
	 * Edit a particular dynamic alert
	 * @param id the id of the alert to edit
	 */
	def editSubmit(alertId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DynamicAlertAction.alertManagementAccess(alertId, user.id) { alert =>
			AppAction { implicit request =>
				editAlertForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.dynamicalerts.edit(formWithErrors, alertId))
					},
					data => {
						val editedAlert = alert.copy(
							name = data.name,
							searchTarget = data.searchTarget,
							matchExpr = data.matchExpr,
							buildTarget = data.buildTarget,
							errorThreshold = data.errorThreshold,
							warnThreshold = data.warnThreshold,
							comparison = data.comparison,
							frequency = data.frequency,
							active = data.active
						)
						
						DynamicAlertModel.editDynamicAlert(editedAlert)
						DynamicAlertTagModel.updateTagsForAlert(editedAlert.id, data.tags)
						Redirect(routes.DynamicAlertsController.view(alertId)).flashing(AppFlash.success("Changes saved."))
					}
				)
			}
		}
	}

	/**
	 * Recover a dynamic alert
	 */
	def recover(alertId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DynamicAlertAction.alertManagementAccess(alertId, user.id, allowDeleted = true) { alert =>
			AppAction { implicit request =>
				DynamicAlertModel.editDynamicAlert(alert.copy(deleted = false))
				Redirect(routes.DynamicAlertsController.view(alertId)).flashing(AppFlash.success("Dynamic alert was recovered successfully."))
			}
		}
	}

	/**
	 * Delete a dynamic alert
	 */
	def delete(alertId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DynamicAlertAction.alertManagementAccess(alertId, user.id) { alert =>
			AppAction { implicit request =>
				DynamicAlertModel.editDynamicAlert(alert.copy(deleted = true))
				Redirect(routes.HomeController.index()).flashing(AppFlash.success("Dynamic alert was deleted successfully."))
			}
		}
	}
}
