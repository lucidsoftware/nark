package com.lucidchart.open.nark.request

import java.util.UUID

import play.api.Play
import play.api.Play.current
import play.api.Mode
import play.api.libs.iteratee.Done
import play.api.mvc._
import play.api.mvc.Results._

import com.lucidchart.open.nark.models.records.Dashboard
import com.lucidchart.open.nark.models.DashboardModel
import com.lucidchart.open.nark.controllers.routes

/**
 * Provides helpers for creating `DashboardAction` values.
 */
trait DashboardActionBuilder {
	/**
	 * Verify a dashboard exists, and call the block with it
	 */
	def existingDashboard(id: UUID, allowDeleted: Boolean = false)(block: (Dashboard) => EssentialAction): EssentialAction = EssentialAction { requestHeader =>
		DashboardModel.findDashboardByID(id) match {
			case Some(dashboard) if (!dashboard.deleted || allowDeleted) => block(dashboard)(requestHeader)
			case _ => Done(Redirect(routes.HomeController.index()).flashing(AppFlash.error("That dashboard does not exist!")))
		}
	}

	/**
	 * Verify a dashboard exists by url, and call the block with it
	 */
	def existingDashboardByUrl(url: String, allowDeleted: Boolean = false)(block: (Dashboard) => EssentialAction): EssentialAction = EssentialAction { requestHeader =>
		DashboardModel.findDashboardByURL(url) match {
			case Some(dashboard) if (!dashboard.deleted || allowDeleted) => block(dashboard)(requestHeader)
			case _ => Done(Redirect(routes.HomeController.index()).flashing(AppFlash.error("That dashboard does not exist!")))
		}
	}

	/**
	 * Verify the current user has access to the dashboard
	 */
	def dashboardManagementAccess(dashboardId: UUID, userId: UUID, allowDeleted: Boolean = false)(block: (Dashboard) => EssentialAction): EssentialAction = existingDashboard(dashboardId, allowDeleted) { dashboard =>
		EssentialAction { requestHeader =>
			if (dashboard.userId == userId) {
				block(dashboard)(requestHeader)
			}
			else {
				Done(Redirect(routes.HomeController.index()).flashing(AppFlash.error("You do not have access to manage this dashboard.")))
			}
		}
	}
}

/**
 * Helper object to create `DashboardAction` values.
 */
object DashboardAction extends DashboardActionBuilder
