package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
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

	private val createForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.pattern("^[a-zA-Z0-9]*$".r, error = "Only alpha-numberic text allowed")),
			"url" -> text.verifying(Constraints.pattern("^[a-zA-Z0-9]*$".r, error = "Only alpha-numberic text allowed"))
		)(CreateDashboard.apply)(CreateDashboard.unapply)
	)

	def create = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val form = createForm.fill(CreateDashboard("", ""))
			Ok(views.html.dashboards.create(form))
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
				val form = createForm.fill(CreateDashboard(dashboard.get.name, dashboard.get.url))
				Ok(views.html.dashboards.edit(form, dashboard.get))
			}
		}
	}

	def createSubmit = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			createForm.bindFromRequest().fold(
				formWithErrors => {
					Ok(views.html.dashboards.create(formWithErrors))
				},
				data => {
					val name = data.name
					val url = data.url
					DashboardModel.createDashboard(new Dashboard(name, url, user.id, false))
					Redirect(routes.DashboardsController.create()).flashing(AppFlash.success(name + " dashboard added successfully."))
				}
			)
		}
	}

	def editSubmit(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val existingDashboard = DashboardModel.findDashboardByID(dashboardId)
			if (existingDashboard.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not exist."))
			}
			else {
				createForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.dashboards.edit(formWithErrors, existingDashboard.get))
					},
					data => {
						if (existingDashboard.get.userId != user.id) {
							Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not belong to the current user."))
						}
						val name = data.name
						val url = data.url
						DashboardModel.createDashboard(existingDashboard.get.copy(name = name, url = url))
						Redirect(routes.DashboardsController.edit(existingDashboard.get.id)).flashing(AppFlash.success(name + " dashboard has been updated successfully."))
					}
				)
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
				val historyItem = new DashboardHistoryItem(dashboard.get)
				val newHistoryCookie = DashboardHistory.addToHistory(request, historyItem)
				Ok(views.html.dashboards.dashboard(dashboard.get)).withCookies(newHistoryCookie)
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
