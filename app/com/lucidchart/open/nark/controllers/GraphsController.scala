package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction, DashboardAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.{GraphModel, DashboardModel}
import com.lucidchart.open.nark.models.records.GraphType
import com.lucidchart.open.nark.models.records.GraphAxisLabel
import java.util.UUID
import play.api.data.Form
import play.api.data.Forms._
import com.lucidchart.open.nark.models.records.Graph
import play.api.data.validation.Constraints

class GraphsController extends AppController {
	private case class EditGraphSubmission(name: String, graphType: GraphType.Value, axisLabel: GraphAxisLabel.Value)

	private val editGraphForm = Form(
		mapping(
			"name" -> text.verifying(Constraints.minLength(1)),
			"type" -> number.verifying("Invalid graph type", x => GraphType.values.map(_.id).contains(x)).transform[GraphType.Value](GraphType(_), _.id),
			"axis" -> number.verifying("Invalid axis label", x => GraphAxisLabel.values.map(_.id).contains(x)).transform[GraphAxisLabel.Value](GraphAxisLabel(_), _.id)
		)(EditGraphSubmission.apply)(EditGraphSubmission.unapply)
	)

	/**
	 * Add a graph to a dashboard
	 */
	def add(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id) { dashboard =>
			AppAction { implicit request =>
				val form = editGraphForm.fill(EditGraphSubmission("", GraphType.normal, GraphAxisLabel.auto))
				Ok(views.html.graphs.add(form, dashboard))
			}
		}
	}

	/**
	 * Submit the add form
	 */
	def addSubmit(dashboardId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.dashboardManagementAccess(dashboardId, user.id) { dashboard =>
			AppAction { implicit request =>
				editGraphForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.graphs.add(formWithErrors, dashboard))
					},
					data => {
						val oldGraphs = GraphModel.findGraphsByDashboardId(dashboard.id)
						val sort = if (oldGraphs.isEmpty) 0 else (oldGraphs.maxBy(_.sort).sort + 1)
						val graph = new Graph(data.name, dashboard.id, sort, data.graphType, data.axisLabel)
						GraphModel.createGraph(graph)
						Redirect(routes.TargetsController.add(graph.id)).flashing(AppFlash.success("Graph was added successfully."))
					}
				)
			}
		}
	}

	/**
	 * Edit a graph
	 */
	def edit(graphId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.graphManagementAccess(graphId, user.id) { (dashboard, graph) =>
			AppAction { implicit request =>
				val form = editGraphForm.fill(EditGraphSubmission(graph.name, graph.typeGraph, graph.axisLabel))
				Ok(views.html.graphs.edit(form, graph))
			}
		}
	}

	/**
	 * Submit the edit form
	 */
	def editSubmit(graphId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.graphManagementAccess(graphId, user.id) { (dashboard, graph) =>
			AppAction { implicit request =>
				editGraphForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.graphs.edit(formWithErrors, graph))
					},
					data => {
						GraphModel.editGraph(graph.copy(name = data.name, typeGraph = data.graphType, axisLabel = data.axisLabel))
						Redirect(routes.DashboardsController.manageGraphsAndTargets(dashboard.id)).flashing(AppFlash.success("Graph was updated successfully."))
					}
				)
			}
		}
	}

	/**
	 * Activate a graph
	 */
	def activate(graphId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.graphManagementAccess(graphId, user.id, allowDeletedGraph = true) { (dashboard, graph) =>
			AppAction { implicit request =>
				GraphModel.editGraph(graph.copy(deleted = false))
				Redirect(routes.DashboardsController.manageGraphsAndTargets(dashboard.id)).flashing(AppFlash.success("Graph was activated successfully."))
			}
		}
	}

	/**
	 * Deactivate a graph
	 */
	def deactivate(graphId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.graphManagementAccess(graphId, user.id) { (dashboard, graph) =>
			AppAction { implicit request =>
				GraphModel.editGraph(graph.copy(deleted = true))
				Redirect(routes.DashboardsController.manageGraphsAndTargets(dashboard.id)).flashing(AppFlash.success("Graph was activated successfully."))
			}
		}
	}
}

object GraphsController extends GraphsController
