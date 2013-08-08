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
import play.api.mvc.Cookie
import play.api.mvc.RequestHeader
import play.api.data._
import play.api.data.Forms._
import validation.Constraints

class DashboardsController extends AppController {
	private case class DashboardFormSubmission(name: String, url: String)

	private val createDashboardForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"url" -> text.verifying(Constraints.pattern("^[a-zA-Z0-9\\.\\-_]*$".r, error = "Only alpha-numberic text and periods (.), dashes (-), and underscores (_) allowed"))
			             .verifying("A dashboard with that url already exists. Please choose another url.", !DashboardModel.findDashboardByURL(_).isDefined)
		)(DashboardFormSubmission.apply)(DashboardFormSubmission.unapply)
	)
	private def editDashboardForm(id: UUID) = {
		Form(
			mapping(
				"name" -> text.verifying(Constraints.minLength(1)),
				"url" -> text.verifying(Constraints.pattern("^[a-zA-Z0-9\\.\\-_]*$".r, error = "Only alpha-numberic text and periods (.), dashes (-), and underscores (_) allowed"))
				             .verifying("A dashboard with that url already exists. Please choose another url.", url => {
				                 val dashboard = DashboardModel.findDashboardByURL(url)
				                 !dashboard.isDefined || dashboard.get.id == id
				             })
			)(DashboardFormSubmission.apply)(DashboardFormSubmission.unapply)
		)
	}

	private def addDashboardToHistoryCookie(dashboard: Dashboard)(implicit request: RequestHeader): Cookie = {
		DashboardHistory.addToHistory(request, new DashboardHistoryItem(dashboard))
	}

	private case class SortFormSubmission(order:List[String])
	private val sortForm = Form(mapping("order" -> list(text))(SortFormSubmission.apply)(SortFormSubmission.unapply))

	/**
	 * Create a new dashboard
	 */
	def create = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val form = createDashboardForm.fill(DashboardFormSubmission("", ""))
			Ok(views.html.dashboards.create(form))
		}
	}

	/**
	 * Submit the create form
	 */
	def createSubmit = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			createDashboardForm.bindFromRequest().fold(
				formWithErrors => {
					Ok(views.html.dashboards.create(formWithErrors))
				},
				data => {
					val dashboard = new Dashboard(data.name, data.url, user.id, false)
					DashboardModel.createDashboard(dashboard)

					val newHistoryCookie = addDashboardToHistoryCookie(dashboard)
					Redirect(routes.GraphsController.add(dashboard.id)).flashing(AppFlash.success("Dashboard was created successfully.")).withCookies(newHistoryCookie)
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
				val form = editDashboardForm(dashboardId).fill(DashboardFormSubmission(dashboard.name, dashboard.url))
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
				editDashboardForm(dashboardId).bindFromRequest().fold(
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
				val newHistoryCookie = addDashboardToHistoryCookie(dashboard)
				val graphs = GraphModel.findGraphsByDashboardId(dashboard.id)
				val targets = TargetModel.findTargetByGraphId(graphs.map(_.id))
				Ok(views.html.dashboards.dashboard(dashboard, graphs, targets)).withCookies(newHistoryCookie)
			}
		}
	}

	/**
	 * Managing links for an existing dashboard
	 */
	def manage(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id) { dashboard =>
			AppAction { implicit request =>
				val newHistoryCookie = addDashboardToHistoryCookie(dashboard)
				Ok(views.html.dashboards.manage(dashboard, user)).withCookies(newHistoryCookie)
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
				val newHistoryCookie = addDashboardToHistoryCookie(dashboard)
				Ok(views.html.dashboards.manageGraphsAndTargets(dashboard, graphs, targetsByGraph, user)).withCookies(newHistoryCookie)
			}
		}
	}

	/**
	 * Sort graphs for an existing dashboard
	 */
	def sortGraphs(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id) { dashboard =>
			AppAction { implicit request =>
				val graphs = GraphModel.findGraphsByDashboardId(dashboardId)
				Ok(views.html.dashboards.sortGraphs(dashboard, graphs, user))
			}
		}
	}
	/**
	 *	Save sorted graph order to db
	 */
	def sortGraphsSubmit(dashboardId: UUID) = AuthAction.authenticatedUser {implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id) { dashboard =>
			AppAction { implicit request => 
				sortForm.bindFromRequest().fold (
					formWithErrors => {
						Redirect(routes.DashboardsController.manage(dashboard.id)).flashing(AppFlash.error("Graph order could not be saved."))
					},
					data => {
						val graphs = GraphModel.findGraphsByDashboardId(dashboard.id)
						graphs.foreach{ graph =>
							GraphModel.editGraph(graph.copy(sort=data.order.indexOf(graph.id.toString)))
						}


						Redirect(routes.DashboardsController.manage(dashboard.id)).flashing(AppFlash.success("Graph order was successfully saved."))
					}
				)
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
				Redirect(routes.DashboardsController.manage(dashboardId)).flashing(AppFlash.success("Dashboard was activated successfully."))
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
	/**
	 * Deactivated dashboards
	 */
	def deleted(term: String) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val matches = DashboardModel.searchDeletedByName(user.id, term)
			Ok(views.html.dashboards.deleted(term, matches))
		}
	}
}

object DashboardsController extends DashboardsController
