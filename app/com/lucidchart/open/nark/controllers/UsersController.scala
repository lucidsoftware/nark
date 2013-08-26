package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.models.UserModel
import com.lucidchart.open.nark.request.{AppAction, AppFlash, AuthAction}
import com.lucidchart.open.nark.views

import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

object UsersController extends UsersController
class UsersController extends AppController {

	private case class EditFormSubmission(
		errorAddress: String,
		errorEnable: Boolean,
		warnAddress: String,
		warnEnable: Boolean
	)

	private val editAddressesForm = Form(
		mapping(
			"error_address" -> email,
			"error_enable" -> boolean,
			"warn_address" -> email,
			"warn_enable" -> boolean
		)(EditFormSubmission.apply)(EditFormSubmission.unapply)
	)

	/**
	 * Get the page to change a user's alert notification addresses
	 */
	def addresses = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val form = editAddressesForm.fill(EditFormSubmission(user.errorAddress, user.errorEnable, user.warnAddress, user.warnEnable))
			Ok(views.html.users.addresses(form))
		}
	}

	/**
	 * Handle the form submitted by the user and edit the addresses
	 */
	def addressesSubmit = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			editAddressesForm.bindFromRequest().fold(
				formWithErrors => {
					Ok(views.html.users.addresses(formWithErrors))
				},
				data => {
					UserModel.editUser(user.copy(
						errorAddress = data.errorAddress,
						errorEnable = data.errorEnable,
						warnAddress = data.warnAddress,
						warnEnable = data.warnEnable
					))
					Redirect(routes.UsersController.addresses()).flashing(AppFlash.success("Alert addresses saved."))
				}
			)
		}
	}

}