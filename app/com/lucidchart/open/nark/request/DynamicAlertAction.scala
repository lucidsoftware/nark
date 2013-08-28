package com.lucidchart.open.nark.request

import com.lucidchart.open.nark.Global
import com.lucidchart.open.nark.controllers.routes
import com.lucidchart.open.nark.models.DynamicAlertModel
import com.lucidchart.open.nark.models.records.DynamicAlert

import java.util.UUID
import play.api.libs.iteratee.Done
import play.api.Mode
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Play
import play.api.Play.current

/**
 * Helper objec to create DynamicAlertAction values.
 */
object DynamicAlertAction extends DynamicAlertActionBuilder

/**
 * Provides helpers for creating DynamicAlertAction values.
 */
trait DynamicAlertActionBuilder {
	/**
	 * Verify that a dynamic alert exists, and then call the block with it
	 * @param id the id of the dynamic alert to look for
	 * @param allowDeleted duh
	 */
	def existingAlert(id: UUID, allowDeleted: Boolean = false)(block: (DynamicAlert) => EssentialAction): EssentialAction = EssentialAction { requestHeader =>
		DynamicAlertModel.findDynamicAlertByID(id) match {
			case Some(alert) if (!alert.deleted || allowDeleted) => block(alert)(requestHeader)
			case _ => Done(Global.error404(requestHeader))
		}
	}

	/**
	 * Verify that the current user has access to this dynamic alert, and call the block that was passed in
	 * @param alertId the id of the dynamic alert they're trying to access
	 * @param userId the id of the user trying to access the alert
	 * @param allowDeleted whether to allow deleted alerts in the alerts
	 */
	def alertManagementAccess(alertId: UUID, userId: UUID, allowDeleted: Boolean = false)(block: (DynamicAlert) => EssentialAction): EssentialAction = existingAlert(alertId, allowDeleted) { alert =>
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