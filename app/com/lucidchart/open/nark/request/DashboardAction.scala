package com.lucidchart.open.nark.request

import java.util.UUID

import play.api.Play
import play.api.Play.current
import play.api.Mode
import play.api.libs.iteratee.Done
import play.api.mvc._
import play.api.mvc.Results._

import com.lucidchart.open.nark.models.records.Dashboard
import com.lucidchart.open.nark.models.records.Graph
import com.lucidchart.open.nark.models.records.Target
import com.lucidchart.open.nark.models.DashboardModel
import com.lucidchart.open.nark.models.GraphModel
import com.lucidchart.open.nark.models.TargetModel
import com.lucidchart.open.nark.controllers.routes
import com.lucidchart.open.nark.utils.DashboardHistory

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
			case _ => {
				val historyCookie = DashboardHistory.removeFromHistory(requestHeader, id)
				Done(
					Redirect(routes.HomeController.index()).flashing(AppFlash.error("That dashboard does not exist!")).withCookies(historyCookie)
				)
			}
		}
	}

	/**
	 * Verify a dashboard exists by url, and call the block with it
	 */
	def existingDashboardByUrl(url: String, allowDeleted: Boolean = false)(block: (Dashboard) => EssentialAction): EssentialAction = EssentialAction { requestHeader =>
		DashboardModel.findDashboardByURL(url) match {
			case Some(dashboard) if (!dashboard.deleted || allowDeleted) => block(dashboard)(requestHeader)
			case _ => {
				val historyCookie = DashboardHistory.removeFromHistory(requestHeader, url)
				Done(
					Redirect(routes.HomeController.index()).flashing(AppFlash.error("That dashboard does not exist!")).withCookies(historyCookie)
				)
			}
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

	/**
	 * Verify the current user has access to the graph
	 */
	def graphManagementAccess(graphId: UUID, userId: UUID, allowDeletedDashboard: Boolean = false, allowDeletedGraph: Boolean = false)(block: (Dashboard, Graph) => EssentialAction): EssentialAction = EssentialAction { requestHeader =>
		GraphModel.findGraphByID(graphId) match {
			case Some(graph) if (!graph.deleted || allowDeletedGraph) => {
				dashboardManagementAccess(graph.dashboardId, userId, allowDeletedDashboard) { dashboard => block(dashboard, graph) }(requestHeader)
			}
			case _ => {
				Done(Redirect(routes.HomeController.index()).flashing(AppFlash.error("You do not have access to manage this graph.")))
			}
		}
	}

	/**
	 * Verify the current user has access to the target
	 */
	def targetManagementAccess(targetId: UUID, userId: UUID, allowDeletedDashboard: Boolean = false, allowDeletedGraph: Boolean = false, allowDeletedTarget: Boolean = false)(block: (Dashboard, Graph, Target) => EssentialAction): EssentialAction = EssentialAction { requestHeader =>
		TargetModel.findTargetByID(targetId) match {
			case Some(target) if (!target.deleted || allowDeletedTarget) => {
				graphManagementAccess(target.graphId, userId, allowDeletedDashboard, allowDeletedGraph) { (dashboard, graph) => block(dashboard, graph, target) }(requestHeader)
			}
			case _ => {
				Done(Redirect(routes.HomeController.index()).flashing(AppFlash.error("You do not have access to manage this target.")))
			}
		}
	}
}

/**
 * Helper object to create `DashboardAction` values.
 */
object DashboardAction extends DashboardActionBuilder
