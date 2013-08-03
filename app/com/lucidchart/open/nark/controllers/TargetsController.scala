package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.{TargetModel, GraphModel}
import java.util.UUID
import play.api.data.Form
import play.api.data.Forms._
import com.lucidchart.open.nark.models.records.Target

class TargetsController extends AppController {

	private case class AddTarget(target: String)

	private val addForm = Form(
		mapping(
			"target" -> text
	)(AddTarget.apply)(AddTarget.unapply)
	)


	def add(graphId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val graph = GraphModel.findGraphByID(graphId)
			if (graph.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not exist."))
			}
			else if (graph.get.userId != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not belong to the current user."))
			}
			else {
				val form = addForm.fill(AddTarget(""))
				Ok(views.html.targets.add(form, graph.get))
			}
		}
	}

	def edit(targetId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val target = TargetModel.findTargetByID(targetId)
			if (target.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Target does not exist."))
			}
			else if (target.get.userId != user.id) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Target does not belong to the current user."))
			}
			else {
				val target = TargetModel.findTargetByID(targetId).get
				val form = addForm.fill(AddTarget(target.target))
				Ok(views.html.targets.edit(form, target))
			}
		}
	}

	def addSubmit(graphId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val graph = GraphModel.findGraphByID(graphId)
			if (graph.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not exist."))
			}
			addForm.bindFromRequest().fold(
				formWithErrors => {
					Ok(views.html.targets.add(formWithErrors, graph.get))
				},
				data => {
					if (graph.get.userId != user.id) {
						Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not belong to the current user."))
					}
					else {
						val target = data.target
						TargetModel.createTarget(new Target(graph.get.id, target, false))
						Redirect(routes.TargetsController.add(graphId)).flashing(AppFlash.success("Target added successfully."))
					}
				}
			)
		}
	}

	def editSubmit(targetId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val target = TargetModel.findTargetByID(targetId)
			if (target.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Target does not exist."))
			}
			addForm.bindFromRequest().fold(
				formWithErrors => {
					Ok(views.html.targets.edit(formWithErrors, target.get))
				},
				data => {
					if (target.get.userId != user.id) {
						Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Target does not belong to the current user."))
					}
					else {
						val targetValue = data.target
						TargetModel.createTarget(target.get.copy(target = targetValue))
						Redirect(routes.TargetsController.edit(target.get.id)).flashing(AppFlash.success("Target updated successfully."))
					}
				}
			)
		}
	}

	def list(graphId: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val graph = GraphModel.findGraphByID(graphId)
			if (graph.isEmpty) {
				Redirect(routes.HomeController.index()).flashing(AppFlash.warning("Graph does not exist."))
			}
			else {
				Ok(views.html.targets.list(graph.get))
			}
		}
	}

	def toggleActivation(uuid: UUID) = AuthAction.authenticatedUser { implicit user =>
		AppAction { implicit request =>
			val target = TargetModel.findTargetByID(uuid)
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