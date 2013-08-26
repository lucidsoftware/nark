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
import com.lucidchart.open.nark.Global

/**
 * Helper object to create `AlertAction` values.
 */
object AlertAction extends AlertActionBuilder

/**
 * Provides helpers for creating `AlertAction` values.
*/
trait AlertActionBuilder {
	/**
	 * Verify an alert exists, and call the block with it
	 */
	def existingAlert(id: UUID, allowDeleted: Boolean = false)(block: (Alert) => EssentialAction): EssentialAction = EssentialAction { requestHeader =>
		AlertModel.findAlertByID(id) match {
			case Some(alert) if (!alert.deleted || allowDeleted) => block(alert)(requestHeader)
			case _ => Done(Global.error404(requestHeader))
		}
	}

	/**
	 * Verify the current user has access to the alert
	 */
	def alertManagementAccess(alertId: UUID, userId: UUID, allowDeleted: Boolean = false)(block: (Alert) => EssentialAction): EssentialAction = existingAlert(alertId, allowDeleted) { alert =>
		EssentialAction { requestHeader =>
			if (alert.userId == userId) {
				block(alert)(requestHeader)
			}
			else {
				Done(Forbidden)
			}
		}
	}
}
