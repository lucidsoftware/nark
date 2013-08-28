package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.models.{DynamicAlertTagSubscriptionModel}
import com.lucidchart.open.nark.models.records.{AlertTagSubscription}
import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}

import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

object DynamicAlertTagSubscriptionsController extends DynamicAlertTagSubscriptionsController
class DynamicAlertTagSubscriptionsController extends AppController {
	private case class EditFormSubmission(
		active: Boolean
	)

	private val editForm = Form(
		mapping(
			"active" -> boolean
		)(EditFormSubmission.apply)(EditFormSubmission.unapply)
	)

	private case class DeleteFormSubmission(
		ignored: String
	)

	private val deleteForm = Form(
		mapping(
			"ignored" -> text
		)(DeleteFormSubmission.apply)(DeleteFormSubmission.unapply)
	)

	/**
	 * Subscribe to an alert tag
	 * @param tag the alert tag to subscribe to
	 */
	def subscribe(tag: String) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val subscription = new AlertTagSubscription(user.id, tag)
			DynamicAlertTagSubscriptionModel.createSubscription(subscription)

			Redirect(routes.DynamicAlertTagsController.tag(tag)).flashing(AppFlash.success("Successfully subscribed to this tag."))
		}
	}

	/**
	 * Edit a tag subscription
	 * @param tag the tag of the subscription to edit
	 */
	def edit(tag: String, mySubscriptionsPage: Int) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			editForm.bindFromRequest().fold(
				formWithErrors => {

					if(mySubscriptionsPage < 1) {
						Redirect(routes.DynamicAlertTagsController.tag(tag)).flashing(AppFlash.error("Unable to edit subscription."))
					} else {
						Redirect(routes.TagSubscriptionsController.allSubscriptionsForUser(mySubscriptionsPage)).flashing(AppFlash.error("Unable to edit subscription."))
					}
				},
				data => {
					val subscription = new AlertTagSubscription(user.id, tag, data.active)
					DynamicAlertTagSubscriptionModel.editSubscription(subscription)
					if(mySubscriptionsPage < 1) {
						Redirect(routes.DynamicAlertTagsController.tag(tag)).flashing(AppFlash.success("Successfully saved changes."))
 					} else {
						Redirect(routes.TagSubscriptionsController.allSubscriptionsForUser(mySubscriptionsPage)).flashing(AppFlash.success("Successfully saved changes."))
					}
	 			}
	 		)
	 	}
	}

	/**
	 * Delete tag subscription
	 * @param tag the tag of the subscription to delete
	 */
	def delete(tag: String) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			deleteForm.bindFromRequest().fold(
				formWithErrors => {
					Redirect(routes.DynamicAlertTagsController.tag(tag)).flashing(AppFlash.error("Unable to delete subscription."))
				},
				data => {
					DynamicAlertTagSubscriptionModel.deleteSubscription(tag, user.id)
					Redirect(routes.DynamicAlertTagsController.tag(tag)).flashing(AppFlash.success("Successfully deleted subscription."))
				}
			)
		}
	}
}