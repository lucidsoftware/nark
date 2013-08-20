package com.lucidchart.open.nark.request

import com.lucidchart.open.nark.controllers.routes
import com.lucidchart.open.nark.models.{AlertModel, UserModel}
import com.lucidchart.open.nark.models.records.{Alert, User}
import java.util.UUID
import play.api.libs.iteratee.Done
import play.api.Mode
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Play
import play.api.Play.current

/**
 * Helper object to create `AlertAction` values.
 */
object AlertAction extends AlertActionBuilder

/**
 * Provides helpers for creating `AlertAction` values.
*/
trait AlertActionBuilder {
	/**
	 * Verify that the current user can manage the alert
	 * @param alertId the id of the alert the user wants to access
	 * @param userId the user's id
	 */
	def alertManagementAccess(alertId: UUID, userId: UUID)(block: (Alert, User) => EssentialAction): EssentialAction = EssentialAction { requestHeader =>
		val alert = AlertModel.getAlert(alertId)
		val user = UserModel.findUserByID(userId)

		if (alert.isDefined && user.isDefined && alert.get.userId == user.get.id) {
			block(alert.get, user.get)(requestHeader)
		}
		else {
			Done(Forbidden)
		}
	}
}