package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction, DashboardAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.DashboardTagsModel
import com.lucidchart.open.nark.models.DashboardModel
import com.lucidchart.open.nark.models.DashboardTagsConverter._
import com.lucidchart.open.nark.utils.StatsD
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import validation.Constraints
import java.util.UUID

class DashboardTagsController extends AppController {

	/*
	 *	Get tag and all the dashboards it is assocaited with.
	 */
	def tag(name: String) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction {implicit request =>
			val dashboardIds = DashboardTagsModel.findDashboardsWithTag(name).map(_.dashboardId)
			val dashboards = DashboardModel.findDashboardByID(dashboardIds).filter(!_.deleted)
			Ok(views.html.dashboardtags.dashboardtag(name, dashboards))
		}
	}

	/*
	 * Search tags by name.
	 */
	def search(term: String) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction {implicit request =>
			val tags = DashboardTagsModel.search(term)
			val dashboardTags = DashboardTagsModel.findDashboardsWithTag(tags)
			val dashboards = DashboardModel.findDashboardByID(dashboardTags.map(_.dashboardId).distinct).filter(!_.deleted)
			Ok(views.html.dashboardtags.search(term, convertToTagsMap(dashboardTags, dashboards)))
		}
	}

	/*
	 * Search tags by name. Returns json formatted for jquery-tokeninput.
	 */
	def searchToJson(term: String) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction { implicit request =>
			val matches = DashboardTagsModel.search(term).map{ m =>
				Json.obj("id" -> m, "name" -> m)
			}
			Ok(Json.toJson(matches))
		}
	}
}

object DashboardTagsController extends DashboardTagsController