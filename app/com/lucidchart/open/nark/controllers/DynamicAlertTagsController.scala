package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppAction, AuthAction}
import com.lucidchart.open.nark.models.DynamicAlertTagModel

import com.lucidchart.open.nark.views
import play.api.libs.json.Json

object DynamicAlertTagsController extends DynamicAlertTagsController
class DynamicAlertTagsController extends AppController {
	/**
	 * Search tags by name. Returns json formatted for jquery-tokeninput.
	 */
	def searchToJson(term: String) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val (found, matches) = DynamicAlertTagModel.search(term + "%", 0)
			Ok(Json.toJson(matches.map{ m =>
				Json.obj("id" -> m.alertId.toString, "name" -> m.tag)
			}))
		}
	}
}