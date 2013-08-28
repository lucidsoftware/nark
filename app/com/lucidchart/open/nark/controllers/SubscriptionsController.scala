package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.models.SubscriptionModel
import com.lucidchart.open.nark.models.AlertTagSubscriptionModel
import com.lucidchart.open.nark.models.records.{AlertType, Pagination, Subscription, SubscriptionRecord}
import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import java.util.UUID
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

object SubscriptionsController extends AppController {
	private case class SubscribeFormSubmission(
		alertType: AlertType.Value
	)

	private case class EditFormSubmission(
		alertType: AlertType.Value,
		active: Boolean
	)

	private case class DeleteFormSubmission(
		ignored: String
	)

	private val subscribeForm = Form(
		mapping(
			"alert_type" -> number.verifying("Invalid alert type", x => AlertType.values.map(_.id).contains(x)).transform[AlertType.Value](AlertType(_), _.id)
		)(SubscribeFormSubmission.apply)(SubscribeFormSubmission.unapply)
	)

	private val editForm = Form(
		mapping(
			"alert_type" -> number.verifying("Invalid alert type", x => AlertType.values.map(_.id).contains(x)).transform[AlertType.Value](AlertType(_), _.id),
			"active" -> boolean
		)(EditFormSubmission.apply)(EditFormSubmission.unapply)
	)

	private val deleteForm = Form(
		mapping(
			"ignored" -> text
		)(DeleteFormSubmission.apply)(DeleteFormSubmission.unapply)
	)

	/**
	 * Subscribe to an alert
	 * @param alertId the id of the alert to subscribe to
	 * @param alertType the type of alert for the subscription, which determines where to be redirected after. Values are "alert" or "dynamic"
	 */
	def subscribe(alertId: UUID, alertType: String) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			subscribeForm.bindFromRequest().fold(
				formWithErrors => {
					if (alertType == "alert") {
						Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.error("Unable to subscribe to this alert."))
					}
					else {
						Redirect(routes.DynamicAlertsController.view(alertId)).flashing(AppFlash.error("Unable to subscribe to this alert."))
					}
				},
				data => {
					val subscription = new Subscription(user.id, alertId, data.alertType)
					SubscriptionModel.createSubscription(subscription)

					if (alertType == "alert") {
						Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.success("Successfully subscribed to this alert."))
					}
					else {
						Redirect(routes.DynamicAlertsController.view(alertId)).flashing(AppFlash.success("Successfully subscribed to this alert."))
					}
				}
			)
		}
	}

	/**
	 * Edit an alert subscription
	 * @param alertId the id of the alert to edit
	 * @param alertType the type of alert for the subscription, which determines where to be redirected after. Values are "alert" or "dynamic"
	 * @param mySubscriptionsPage the page number to show
	 */
	 def edit(alertId: UUID, alertType: String, mySubscriptionsPage: Int) = AuthAction.authenticatedUser { implicit user =>
	 	AppAction { implicit request =>
	 		editForm.bindFromRequest().fold(
	 			formWithErrors => {
	 				if(mySubscriptionsPage < 1) {
	 					if (alertType == "alert") {
	 						Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.error("Unable to edit subscription."))
	 					}
	 					else {
	 						Redirect(routes.DynamicAlertsController.view(alertId)).flashing(AppFlash.error("Unable to edit subscription"))
	 					}
	 				} else {
	 					Redirect(routes.SubscriptionsController.allSubscriptionsForUser(mySubscriptionsPage)).flashing(AppFlash.error("Unable to edit subscription."))
	 				}
	 			},
	 			data => {
	 				val subscription = new Subscription(user.id, alertId, data.alertType, data.active)
	 				SubscriptionModel.editSubscription(alertId, user.id, subscription)
	 				if(mySubscriptionsPage < 1) {
	 					if (alertType == "alert") {
	 						Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.success("Successfully saved changes."))
	 					}
	 					else {
	 						Redirect(routes.DynamicAlertsController.view(alertId)).flashing(AppFlash.success("Successfully saved changes."))
	 					}
	 				} else {
	 					Redirect(routes.SubscriptionsController.allSubscriptionsForUser(mySubscriptionsPage)).flashing(AppFlash.success("Successfully saved changes."))
	 				}
	 			}
	 		)
	 	}
	 }

	 /**
	 * Delete an alert subscription
	 * @param alertId the id of the alert to delete
	 * @param alertType the type of alert for the subscription, which determines where to be redirected after. Values are "alert" or "dynamic"
	 */
	 def delete(alertId: UUID, alertType: String) = AuthAction.authenticatedUser { implicit user =>
	 	AppAction { implicit request =>
	 		deleteForm.bindFromRequest().fold(
	 			formWithErrors => {
	 				if (alertType == "alert") {
	 					Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.error("Unable to delete subscription."))
	 				}
	 				else {
	 					Redirect(routes.DynamicAlertsController.view(alertId)).flashing(AppFlash.error("Unable to delete subscription."))
	 				}
	 			},
	 			data => {
	 				SubscriptionModel.deleteSubscription(alertId, user.id)

	 				if (alertType == "alert") {
	 					Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.success("Successfully deleted subscription."))
	 				}
	 				else {
	 					Redirect(routes.DynamicAlertsController.view(alertId)).flashing(AppFlash.success("Successfully deleted subscription."))
	 				}
	 			}
	 		)
	 	}
	 }

	/**
	 * Get all subscriptions for a user
	 */
	def allSubscriptionsForUser(page: Int) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val realPage = page.max(1)
			val (found, matches) = SubscriptionModel.getSubscriptionsByUser(user, realPage - 1)
			val subscriptions = matches.filter { subscription =>
				subscription.subscription.alertType == AlertType.alert
			}
			Ok(views.html.subscriptions.user(Pagination[SubscriptionRecord](realPage, found, SubscriptionModel.configuredLimit, subscriptions)))
		}
	}

}