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

class GraphsController extends AppController {

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
				Ok(views.html.graphs.add(dashboard.get, DashboardModel.findAll()))
			}
		}
	}

	def addSubmit(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val dashboard = DashboardModel.findDashboardByID(dashboardId)
			if (dashboard.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not exist."))
			}
			else if (dashboard.get.userId != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Dashboard does not belong to the current user."))
			}
			else {
				val data : Map[String, Seq[String]] = request.body.asFormUrlEncoded.getOrElse(Map())
				val name = data("name").head
				val sort = data("sort").head
				val graphType = GraphTypes.withName(data("type").head.toLowerCase)
				GraphModel.createGraph(new Graph(name, dashboard.get.id, sort.toInt, graphType, user.id, false))
				Redirect(routes.GraphsController.add(dashboardId)).flashing(AppFlash.success(name + " graph added successfully."))
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
				Ok(views.html.graphs.list(graphs, dashboard.get, DashboardModel.findAll()))
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
