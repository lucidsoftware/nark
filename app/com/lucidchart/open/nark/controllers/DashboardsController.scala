package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.{GraphModel, DashboardModel}
import com.lucidchart.open.nark.models.records.Dashboard
import java.util.UUID

class DashboardsController extends AppController {

	def create = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			Ok(views.html.dashboards.create(DashboardModel.findAll()))
		}
	}

	def edit(uuid: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val dashboard = DashboardModel.findDashboardByID(uuid)
			if (dashboard.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not exist."))
			}
			else if (dashboard.get.userId != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not belong to the current user."))
			}
			else {
				Ok(views.html.dashboards.edit(dashboard.get, DashboardModel.findAll()))
			}
		}
	}

	def createSubmit = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val data : Map[String, Seq[String]] = request.body.asFormUrlEncoded.getOrElse(Map())
			val name = data("name").head
			val url = data("url").head
			if (data.get("uuid").isDefined) {
				val existingDashboard = DashboardModel.findDashboardByID(UUID.fromString(data("uuid").head)).get
				DashboardModel.createDashboard(existingDashboard.copy(name = name, url = url))
				Redirect(routes.DashboardsController.create()).flashing(AppFlash.success(name + " dashboard has been updated successfully."))
			}
			else {
				DashboardModel.createDashboard(new Dashboard(name, url, user.id, false))
				Redirect(routes.DashboardsController.create()).flashing(AppFlash.success(name + " dashboard added successfully."))
			}
		}
	}

	def dashboard(url: String) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction { implicit request =>
			val dashboard = DashboardModel.findDashboardByURL(url)
			if (dashboard.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not exist."))
			}
			else {
				val graphs = GraphModel.findGraphsByDashboardId(dashboard.get.id)
				Ok(views.html.dashboards.dashboard(dashboard.get, graphs, DashboardModel.findAll()))
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
}

object DashboardsController extends DashboardsController
