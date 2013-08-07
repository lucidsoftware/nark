package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction, DashboardAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.{TargetModel, GraphModel}
import java.util.UUID
import play.api.data.Form
import play.api.data.Forms._
import com.lucidchart.open.nark.models.records.Target

class TargetsController extends AppController {
	private case class EditTargetSubmission(target: String)

	private val editTargetForm = Form(
		mapping(
			"target" -> text
		)(EditTargetSubmission.apply)(EditTargetSubmission.unapply)
	)

	/**
	 * Add a new target to an existing graph
	 */
	def add(graphId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.graphManagementAccess(graphId, user.id) { (dashboard, graph) =>
			AppAction { implicit request =>
				val form = editTargetForm.fill(EditTargetSubmission(""))
				Ok(views.html.targets.add(form, graph))
			}
		}
	}

	/**
	 * Submit the add form
	 */
	def addSubmit(graphId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.graphManagementAccess(graphId, user.id) { (dashboard, graph) =>
			AppAction { implicit request =>
				editTargetForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.targets.add(formWithErrors, graph))
					},
					data => {
						val target = new Target(graph.id, data.target, false)
						TargetModel.createTarget(target)
						Redirect(routes.DashboardsController.manageGraphsAndTargets(dashboard.id)).flashing(AppFlash.success("Target was added successfully."))
					}
				)
			}
		}
	}

	/**
	 * Edit a target
	 */
	def edit(targetId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.targetManagementAccess(targetId, user.id) { (dashboard, graph, target) =>
			AppAction { implicit request =>
				val form = editTargetForm.fill(EditTargetSubmission(target.target))
				Ok(views.html.targets.edit(form, target))
			}
		}
	}

	/**
	 * Submit the edit form
	 */
	def editSubmit(targetId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.targetManagementAccess(targetId, user.id) { (dashboard, graph, target) =>
			AppAction { implicit request =>
				editTargetForm.bindFromRequest().fold(
					formWithErrors => {
						Ok(views.html.targets.edit(formWithErrors, target))
					},
					data => {
						TargetModel.editTarget(target.copy(target = data.target))
						Redirect(routes.DashboardsController.manageGraphsAndTargets(dashboard.id)).flashing(AppFlash.success("Target was updated successfully."))
					}
				)
			}
		}
	}

	/**
	 * Activate a target
	 */
	def activate(targetId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.targetManagementAccess(targetId, user.id, allowDeletedGraph = true, allowDeletedTarget = true) { (dashboard, graph, target) =>
			AppAction { implicit request =>
				TargetModel.editTarget(target.copy(deleted = false))
				Redirect(routes.DashboardsController.manageGraphsAndTargets(dashboard.id)).flashing(AppFlash.success("Target was activated successfully."))
			}
		}
	}

	/**
	 * Deactivate a target
	 */
	def deactivate(targetId: UUID) = AuthAction.authenticatedUser { implicit user =>
		DashboardAction.targetManagementAccess(targetId, user.id) { (dashboard, graph, target) =>
			AppAction { implicit request =>
				TargetModel.editTarget(target.copy(deleted = true))
				Redirect(routes.DashboardsController.manageGraphsAndTargets(dashboard.id)).flashing(AppFlash.success("Target was activated successfully."))
			}
		}
	}
}

object TargetsController extends TargetsController