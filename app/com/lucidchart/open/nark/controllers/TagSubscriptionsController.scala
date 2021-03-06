package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.models.{AlertTagSubscriptionModel, AlertTagModel, AlertModel, TagConverter}
import com.lucidchart.open.nark.models.records.{Alert, AlertTagSubscription, AlertTagSubscriptionRecord, Pagination}
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
			val subscription = new AlertTagSubscription(user.id, tag)
			AlertTagSubscriptionModel.createSubscription(subscription)

			Redirect(routes.AlertTagsController.tag(tag)).flashing(AppFlash.success("Successfully subscribed to this tag."))
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
	 					Redirect(routes.AlertTagsController.tag(tag)).flashing(AppFlash.error("Unable to edit subscription."))
	 				} else {
	 					Redirect(routes.TagSubscriptionsController.allSubscriptionsForUser(mySubscriptionsPage)).flashing(AppFlash.error("Unable to edit subscription."))
	 				}
	 			},
	 			data => {
	 				val subscription = new AlertTagSubscription(user.id, tag, data.active)
	 				AlertTagSubscriptionModel.editSubscription(subscription)
	 				if(mySubscriptionsPage < 1) {
	 					Redirect(routes.AlertTagsController.tag(tag)).flashing(AppFlash.success("Successfully saved changes."))
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
	 				Redirect(routes.AlertTagsController.tag(tag)).flashing(AppFlash.error("Unable to delete subscription."))
	 			},
	 			data => {
	 				AlertTagSubscriptionModel.deleteSubscription(tag, user.id)
	 				Redirect(routes.AlertTagsController.tag(tag)).flashing(AppFlash.success("Successfully deleted subscription."))
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
			val (found, tagSubscriptions) = AlertTagSubscriptionModel.getSubscriptionsByUser(user, realPage - 1)
			val tags = AlertTagModel.findAlertsByTag(tagSubscriptions.map{ts => ts.subscription.tag})
			val alerts = AlertModel.findAlertByID(tags.map{tag => tag.recordId}.distinct)
			Ok(views.html.tagsubscriptions.user(Pagination[AlertTagSubscriptionRecord](realPage, found, AlertTagSubscriptionModel.configuredLimit, tagSubscriptions), TagConverter.toTagMap[Alert](tags, alerts)))
		}
	}

}