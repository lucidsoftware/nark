package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.models.SubscriptionModel
import com.lucidchart.open.nark.models.records.{AlertType, Subscription}
import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
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
	 */
	def subscribe(alertId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			subscribeForm.bindFromRequest().fold(
				formWithErrors => {
					Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.error("Unable to subscribe to this alert."))
				},
				data => {
					val subscription = new Subscription(user.id, alertId, data.alertType)
					SubscriptionModel.createSubscription(subscription)

					Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.success("Successfully subscribed to this alert."))
				}
			)
		}
	}

	/**
	 * Edit an alert subscription
	 * @param alertId the id of the alert to edit
	 */
	 def edit(alertId: UUID) = AuthAction.authenticatedUser { implicit user =>
	 	AppAction { implicit request =>
	 		editForm.bindFromRequest().fold(
	 			formWithErrors => {
	 				Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.error("Unable to edit subscription."))
	 			},
	 			data => {
	 				val subscription = new Subscription(user.id, alertId, data.alertType, data.active)
	 				SubscriptionModel.editSubscription(alertId, user.id, subscription)

	 				Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.success("Successfully saved changes."))
	 			}
	 		)
	 	}
	 }

	 /**
	 * Delete an alert subscription
	 * @param alertId the id of the alert to delete
	 */
	 def delete(alertId: UUID) = AuthAction.authenticatedUser { implicit user =>
	 	AppAction { implicit request =>
	 		deleteForm.bindFromRequest().fold(
	 			formWithErrors => {
	 				Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.error("Unable to delete subscription."))
	 			},
	 			data => {
	 				SubscriptionModel.deleteSubscription(alertId, user.id)

	 				Redirect(routes.AlertsController.view(alertId)).flashing(AppFlash.success("Successfully deleted subscription."))
	 			}
	 		)
	 	}
	 }

}