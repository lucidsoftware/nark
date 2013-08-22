package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.models.{TagSubscriptionModel, AlertTagModel, AlertModel, AlertTagConverter}
import com.lucidchart.open.nark.models.records.{TagSubscription, TagSubscriptionRecord}
import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import java.util.UUID
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

object TagSubscriptionsController extends AppController {
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
			val subscription = new TagSubscription(user.id, tag)
			TagSubscriptionModel.createSubscription(subscription)

			Redirect(routes.AlertTagsController.tag(tag)).flashing(AppFlash.success("Successfully subscribed to this alert."))
		}
	}

	/**
	 * Edit a tag subscription
	 * @param tag the tag of the subscription to edit
	 */
	 def edit(tag: String, redirectMySubscriptions: Boolean = false) = AuthAction.authenticatedUser { implicit user =>
	 	AppAction { implicit request =>
	 		editForm.bindFromRequest().fold(
	 			formWithErrors => {
	 				if(redirectMySubscriptions) {
	 					Redirect(routes.TagSubscriptionsController.allSubscriptionsForUser(user.id)).flashing(AppFlash.error("Unable to edit subscription."))
	 				} else {
	 					Redirect(routes.AlertTagsController.tag(tag)).flashing(AppFlash.error("Unable to edit subscription."))
	 				}
	 			},
	 			data => {
	 				val subscription = new TagSubscription(user.id, tag, data.active)
	 				TagSubscriptionModel.editSubscription(tag, user.id, subscription)
	 				if(redirectMySubscriptions) {
	 					Redirect(routes.TagSubscriptionsController.allSubscriptionsForUser(user.id)).flashing(AppFlash.success("Successfully saved changes."))
 					} else {
	 					Redirect(routes.AlertTagsController.tag(tag)).flashing(AppFlash.success("Successfully saved changes."))
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
	 				Redirect(routes.AlertTagsController.tag(tag)).flashing(AppFlash.error("Unable to delete subscription."))
	 			},
	 			data => {
	 				TagSubscriptionModel.deleteSubscription(tag, user.id)
	 				Redirect(routes.AlertTagsController.tag(tag)).flashing(AppFlash.success("Successfully deleted subscription."))
	 			}
	 		)
	 	}
	 }

	/**
	 * Get all subscriptions for a user
	 * @param id the id of the user to look up
	 */
	def allSubscriptionsForUser(id: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			if (id != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.error("You do not have access to manage this user's subscriptions."))
			}
			else {
				val tagSubscriptions = TagSubscriptionModel.getSubscriptionsByUser(id)
				val tags = AlertTagModel.findAlertsByTag(tagSubscriptions.map{ts => ts.subscription.tag}.distinct)
				val alerts = AlertModel.getAlerts(tags.map{tag => tag.alertId})
				Ok(views.html.tagsubscriptions.user(tagSubscriptions, AlertTagConverter.toTagMap(tags, alerts))(request, Some(user)))
			}
		}
	}

}