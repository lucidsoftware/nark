package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.models.UserModel
import com.lucidchart.open.nark.models.records.{Pagination,User}
import com.lucidchart.open.nark.request.{AppAction, AppFlash, AuthAction}
import com.lucidchart.open.nark.views
import java.util.UUID
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

	def manageAdmin(page: Int) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val ids = UserModel.getAdminUserId()
			val realPage = page.max(1)
			if ( ids.length > 0 ) {
				if( ids.contains(user.id) ){
					val (found,matches) = UserModel.getAllUsers( realPage-1, user.id )
					Ok( views.html.users.admin(Pagination[User](realPage, found, UserModel.configuredLimit , matches),false) )
				}	else {
					Redirect(routes.HomeController.index).flashing(AppFlash.error("Please Contact the administrator to gain Admin Privileges."))
				}
			}
			else {
				Ok(views.html.users.admin(Pagination[User](realPage,0,UserModel.configuredLimit,Nil),true))
			}
		}
	}

	def manageAdminSubmit = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			println( UserModel.getAdminUserId().length )
			if ( UserModel.isAdmin(user.id) || (UserModel.getAdminUserId().length == 0) ){
				val formData = request.body.asFormUrlEncoded
				val userIds = formData.get("userIds")
				val action = formData.get("action").head
				val admin = action match {
											case "Revoke" => false
											case "Grant" | "Yes" => true
										}
				userIds.map { id =>
									UserModel.manageAdmin( UUID.fromString(id), admin )
				}
				Redirect(routes.UsersController.manageAdmin()).flashing(AppFlash.success("Administrative Privileges Changed Successfully"))
			}	else {
			Redirect(routes.HomeController.index).flashing(AppFlash.error("Please Contact the administrator to gain Admin Privileges"))
		}
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