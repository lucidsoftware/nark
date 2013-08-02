package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.{TargetModel, GraphTypes, GraphModel, DashboardModel}
import com.lucidchart.open.nark.models.records.{Target, Graph}
import java.util.UUID
import views.html.dashboards.dashboard

class TargetsController extends AppController {

	def add(graphId: String) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val graph = GraphModel.findGraphByID(UUID.fromString(graphId))
			if (graph.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not exist."))
			}
			else if (graph.get.userId != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not belong to the current user."))
			}
			else {
				val dashboard = DashboardModel.findDashboardByID(graph.get.dashboardId)
				Ok(views.html.targets.add(graph.get, dashboard.get, DashboardModel.findAll()))
			}
		}
	}

	def addSubmit(graphId: String) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val graph = GraphModel.findGraphByID(UUID.fromString(graphId))
			if (graph.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not exist."))
			}
			else if (graph.get.userId != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not belong to the current user."))
			}
			else {
				val data : Map[String, Seq[String]] = request.body.asFormUrlEncoded.getOrElse(Map())
				val target = data("target").head
				TargetModel.createTarget(new Target(graph.get.id, target, user.id, false))
				Redirect(routes.TargetsController.add(graphId)).flashing(AppFlash.success("Target added successfully."))
			}
		}
	}

	def list(graphId: String) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val graph = GraphModel.findGraphByID(UUID.fromString(graphId))
			if (graph.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not exist."))
			}
			else {
				val targets = TargetModel.findTargetByGraphId(graph.get.id)
				Ok(views.html.targets.list(targets, graph.get, DashboardModel.findAll()))
			}
		}
	}

	def toggleActivation(uuid: String) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val target = TargetModel.findTargetByID(UUID.fromString(uuid))
			if (target.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Target does not exist."))
			}
			else if (target.get.userId != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Target does not belong to the current user."))
			}
			else {
				val toggleForm = if (target.get.deleted) "activated" else "deactivated"
				TargetModel.toggleActivation(target.get)
				val returnUrl = request.queryString.get("return_url").flatMap(_.headOption)
				if (returnUrl.isDefined) {
					Redirect(returnUrl.get).flashing(AppFlash.success("Target " + toggleForm + "."))
				}
				else {
					Redirect(routes.HomeController.index()).flashing(AppFlash.success("Target " + toggleForm + "."))
				}
			}
		}
	}
}

object TargetsController extends TargetsController