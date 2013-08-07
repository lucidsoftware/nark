package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction, DashboardAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.utils.DashboardHistory
import com.lucidchart.open.nark.utils.DashboardHistoryItem
import com.lucidchart.open.nark.models.DashboardModel
import com.lucidchart.open.nark.models.GraphModel
import com.lucidchart.open.nark.models.TargetModel
import com.lucidchart.open.nark.models.records.Dashboard
import java.util.UUID
import play.api.data._
import play.api.data.Forms._
import validation.Constraints

class DashboardsController extends AppController {
	private case class DashboardFormSubmission(name: String, url: String)

	private val editDashboardForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"url" -> text.verifying(Constraints.pattern("^[a-zA-Z0-9\\.\\-_]*$".r, error = "Only alpha-numberic text and periods (.), dashes (-), and underscores (_) allowed"))
		)(DashboardFormSubmission.apply)(DashboardFormSubmission.unapply)
	)

	/**
	 * Create a new dashboard
	 */
	def create = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val form = editDashboardForm.fill(DashboardFormSubmission("", ""))
			Ok(views.html.dashboards.create(form))
		}
	}

	/**
	 * Submit the create form
	 */
	def createSubmit = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			editDashboardForm.bindFromRequest().fold(
				formWithErrors => {
					Ok(views.html.dashboards.create(formWithErrors))
				},
				data => {
					val dashboard = new Dashboard(data.name, data.url, user.id, false)
					DashboardModel.createDashboard(dashboard)
					Redirect(routes.DashboardsController.manageGraphsAndTargets(dashboard.id)).flashing(AppFlash.success("Dashboard was created successfully."))
				}
			)
		}
	}

	/**
	 * Edit an existing dashboard
	 */
	def edit(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id) { dashboard =>
			AppAction { implicit request =>
				val form = editDashboardForm.fill(DashboardFormSubmission(dashboard.name, dashboard.url))
				Ok(views.html.dashboards.edit(form, dashboard))
			}
		}
	}

	/**
	 * Submit the edit form
	 */
	def editSubmit(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id) { dashboard =>
			AppAction { implicit request =>
				editDashboardForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.dashboards.edit(formWithErrors, dashboard))
					},
					data => {
						DashboardModel.editDashboard(dashboard.copy(name = data.name, url = data.url))
						Redirect(routes.DashboardsController.manage(dashboard.id)).flashing(AppFlash.success("Dashboard was updated successfully."))
					}
				)
			}
		}
	}

	/**
	 * Show the dashboard with its graphs, targets, and filters.
	 */
	def dashboard(url: String) = DashboardAction.existingDashboardByUrl(url) { dashboard =>
		AuthAction.maybeAuthenticatedUser { implicit userOption =>
			AppAction { implicit request =>
				val historyItem = new DashboardHistoryItem(dashboard)
				val newHistoryCookie = DashboardHistory.addToHistory(request, historyItem)
				Ok(views.html.dashboards.dashboard(dashboard)).withCookies(newHistoryCookie)
			}
		}
	}

	/**
	 * Managing links for an existing dashboard
	 */
	def manage(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id) { dashboard =>
			AppAction { implicit request =>
				Ok(views.html.dashboards.manage(dashboard, user))
			}
		}
	}

	/**
	 * Manage graphs and targets for an existing dashboard
	 */
	def manageGraphsAndTargets(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id) { dashboard =>
			AppAction { implicit request =>
				val graphs = GraphModel.findGraphsByDashboardId(dashboardId)
				val targets = TargetModel.findTargetByGraphId(graphs.map(_.id))
				val targetsByGraph = targets.groupBy(_.graphId)
				Ok(views.html.dashboards.manageGraphsAndTargets(dashboard, graphs, targetsByGraph, user))
			}
		}
	}

	/**
	 * Activate a dashboard
	 */
	def activate(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id, allowDeleted = true) { dashboard =>
			AppAction { implicit request =>
				DashboardModel.editDashboard(dashboard.copy(deleted = false))
				Redirect(routes.HomeController.index()).flashing(AppFlash.success("Dashboard was activated successfully."))
			}
		}
	}

	/**
	 * Deactivate a dashboard
	 */
	def deactivate(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id) { dashboard =>
			AppAction { implicit request =>
				DashboardModel.editDashboard(dashboard.copy(deleted = true))
				Redirect(routes.HomeController.index()).flashing(AppFlash.success("Dashboard was deactivated successfully."))
			}
		}
	}

	/**
	 * Dashboard searching utility
	 */
	def search(term: String) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction { implicit request =>
			val matches = DashboardModel.searchByName(term).filter(!_.deleted)
			Ok(views.html.dashboards.search(term, matches))
		}
	}
}

object DashboardsController extends DashboardsController
