package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction, DashboardAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.utils.DashboardHistory
import com.lucidchart.open.nark.utils.DashboardHistoryItem
import com.lucidchart.open.nark.models.DashboardModel
import com.lucidchart.open.nark.models.records.Dashboard
import java.util.UUID
import play.api.data._
import play.api.data.Forms._
import validation.Constraints

class DashboardsController extends AppController {
	private case class CreateDashboard(name: String, url: String)
	private case class EditDashboard(name: String, url: String)

	private val createForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"url" -> text.verifying(Constraints.pattern("^[a-zA-Z0-9\\.\\-_]*$".r, error = "Only alpha-numberic text and periods (.), dashes (-), and underscores (_) allowed"))
		)(CreateDashboard.apply)(CreateDashboard.unapply)
	)
	private val editForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"url" -> text.verifying(Constraints.pattern("^[a-zA-Z0-9\\.\\-_]*$".r, error = "Only alpha-numberic text and periods (.), dashes (-), and underscores (_) allowed"))
		)(EditDashboard.apply)(EditDashboard.unapply)
	)

	/**
	 * Create a new dashboard
	 */
	def create = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val form = createForm.fill(CreateDashboard("", ""))
			Ok(views.html.dashboards.create(form))
		}
	}

	/**
	 * Submit the create form
	 */
	def createSubmit = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			createForm.bindFromRequest().fold(
				formWithErrors => {
					Ok(views.html.dashboards.create(formWithErrors))
				},
				data => {
					DashboardModel.createDashboard(new Dashboard(data.name, data.url, user.id, false))
					Redirect(routes.DashboardsController.dashboard(data.url)).flashing(AppFlash.success("Dashboard was created successfully."))
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
				val form = editForm.fill(EditDashboard(dashboard.name, dashboard.url))
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
				editForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.dashboards.edit(formWithErrors, dashboard))
					},
					data => {
						DashboardModel.editDashboard(dashboard.copy(name = data.name, url = data.url))
						Redirect(routes.DashboardsController.dashboard(data.url)).flashing(AppFlash.success("Dashboard was updated successfully."))
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

	def toggleActivation(uuid: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val dashboard = DashboardModel.findDashboardByID(uuid)
			if (dashboard.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not exist."))
			}
			else if (dashboard.get.userId != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not belong to the current user."))
			}
			else {
				val toggleForm = if (dashboard.get.deleted) "activated" else "deactivated"
				DashboardModel.toggleActivation(dashboard.get)
				val returnUrl = request.queryString.get("return_url").flatMap(_.headOption)
				if (returnUrl.isDefined) {
					Redirect(returnUrl.get).flashing(AppFlash.success("Dashboard " + toggleForm + "."))
				}
				else {
					Redirect(routes.HomeController.index()).flashing(AppFlash.success("Dashboard " + toggleForm + "."))
				}
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
