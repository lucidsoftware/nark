package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppAction, AuthAction}
import com.lucidchart.open.nark.models.{AlertModel, AlertTagModel}
import com.lucidchart.open.nark.views


object AlertTagsController extends AlertTagsController
class AlertTagsController extends AppController {
	
	/*
	 *	Get tag and all the dashboards it is assocaited with.
	 * @param tag the tag to look for
	 */
	def tag(tag: String) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val alertIds = AlertTagModel.findAlertsByTag(tag).map(_.alertId)
			val alerts = AlertModel.getAlerts(alertIds)
			Ok(views.html.alerttags.tag(tag, alerts))
		}
	}
}