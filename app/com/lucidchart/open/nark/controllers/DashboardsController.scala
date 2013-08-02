package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.{GraphModel, DashboardModel}
import com.lucidchart.open.nark.models.records.Dashboard
import java.util.UUID
import play.api.data._
import play.api.data.Forms._
import validation.Constraints

class DashboardsController extends AppController {

	private case class CreateDashboard(name: String, url: String, id: Option[String]) {
		val uuid = id.map(i => UUID.fromString(i))
	}

	private val createForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.pattern("^[a-zA-Z0-9]*$".r, error = "Only alpha-numberic text allowed")),
			"url" -> text.verifying(Constraints.pattern("^[a-zA-Z0-9]*$".r, error = "Only alpha-numberic text allowed")),
			"uuid" -> optional(text.verifying(Constraints.pattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r)))
		)(CreateDashboard.apply)(CreateDashboard.unapply)
	)

	def create = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val form = createForm.fill(CreateDashboard("", "", None))
			Ok(views.html.dashboards.create(form, DashboardModel.findAll()))
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
				routes.DashboardsController.createSubmit()
				Ok(views.html.dashboards.edit(dashboard.get, DashboardModel.findAll()))
			}
		}
	}

	def createSubmit = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
				createForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.dashboards.create(formWithErrors, DashboardModel.findAll()))
					},
					data => {
					val name = data.name
					val url = data.url
					if (data.uuid.isDefined) {
						val existingDashboard = DashboardModel.findDashboardByID(data.uuid.get).get
						DashboardModel.createDashboard(existingDashboard.copy(name = name, url = url))
						Redirect(routes.DashboardsController.create()).flashing(AppFlash.success(name + " dashboard has been updated successfully."))
					}
					else {
						DashboardModel.createDashboard(new Dashboard(name, url, user.id, false))
						Redirect(routes.DashboardsController.create()).flashing(AppFlash.success(name + " dashboard added successfully."))
					}
				}
			)
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
