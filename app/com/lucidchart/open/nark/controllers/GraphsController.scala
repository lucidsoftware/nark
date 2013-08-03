package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.{TargetModel, GraphTypes, GraphModel, DashboardModel}
import com.lucidchart.open.nark.models.records.{Graph, Dashboard}
import java.util.UUID
import annotation.target
import scala.annotation
import views.html.dashboards.dashboard
import views.html.helpers.graph
import play.api.data.Form
import play.api.data.Forms._
import com.lucidchart.open.nark.models.records.Graph
import play.api.data.validation.Constraints

class GraphsController extends AppController {

	private case class AddGraph(name: String, graphType: Int) {
		val graphTypeVal = GraphTypes(graphType)
	}

	private val addForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.pattern("^[a-zA-Z0-9]*$".r, error = "Only alpha-numberic text allowed")),
			"type" -> number.verifying(Constraints.min(0), Constraints.max(GraphTypes.maxId))
		)(AddGraph.apply)(AddGraph.unapply)
	)

	def add(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val dashboard = DashboardModel.findDashboardByID(dashboardId)
			if (dashboard.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not exist."))
			}
			else if (dashboard.get.userId != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not belong to the current user."))
			}
			else {
				val form = addForm.fill(AddGraph("", 0))
				Ok(views.html.graphs.add(form, dashboard.get))
			}
		}
	}

	def addSubmit(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val dashboard = DashboardModel.findDashboardByID(dashboardId)
			if (dashboard.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not exist."))
			}
			else {
				addForm.bindFromRequest().fold(
					formWithErrors => {
							Ok(views.html.graphs.add(formWithErrors, dashboard.get))
					},
					data => {
						if (dashboard.get.userId != user.id) {
							Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not belong to the current user."))
						}
						else {
							val name = data.name
							val graphType = data.graphTypeVal
							val graphs = GraphModel.findGraphsByDashboardId(dashboard.get.id)
							val sort = if (!graphs.isEmpty) graphs.map(_.sort).min - 1 else 1000000
							GraphModel.createGraph(new Graph(name, dashboard.get.id, sort, graphType, user.id, false))
							Redirect(routes.GraphsController.add(dashboardId)).flashing(AppFlash.success(name + " graph added successfully."))
						}
					}
				)
			}
		}
	}

	def list(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val dashboard = DashboardModel.findDashboardByID(dashboardId)
			if (dashboard.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not exist."))
			}
			else {
				val graphs = GraphModel.findGraphsByDashboardId(dashboard.get.id)
				Ok(views.html.graphs.list(graphs, dashboard.get))
			}
		}
	}

	def toggleActivation(uuid: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val graph = GraphModel.findGraphByID(uuid)
			if (graph.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not exist."))
			}
			else if (graph.get.userId != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not belong to the current user."))
			}
			else {
				val toggleForm = if (graph.get.deleted) "activated" else "deactivated"
				GraphModel.toggleActivation(graph.get)
				val returnUrl = request.queryString.get("return_url").flatMap(_.headOption)
				if (returnUrl.isDefined) {
					Redirect(returnUrl.get).flashing(AppFlash.success("Graph " + toggleForm + "."))
				}
				else {
					Redirect(routes.HomeController.index()).flashing(AppFlash.success("Graph " + toggleForm + "."))
				}
			}
		}
	}
}

object GraphsController extends GraphsController
