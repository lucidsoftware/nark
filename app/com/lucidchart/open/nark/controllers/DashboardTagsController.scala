package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction, DashboardAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.{DashboardTagsModel, DashboardModel, TagConverter}
import com.lucidchart.open.nark.models.records.{Dashboard, Pagination, TagMap}
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
			val dashboardIds = DashboardTagsModel.findDashboardsWithTag(name).map(_.recordId)
			val dashboards = DashboardModel.findDashboardByID(dashboardIds).filter(!_.deleted)
			Ok(views.html.dashboardtags.dashboardtag(name, dashboards))
		}
	}

	/*
	 * Search tags by name.
	 */
	def search(term: String, page: Int) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction {implicit request =>
			val realPage = page.max(1)
			val (found, tags) = DashboardTagsModel.search(term, realPage - 1)
			val dashboardTags = DashboardTagsModel.findDashboardsWithTag(tags)
			val dashboards = DashboardModel.findDashboardByID(dashboardTags.map(_.recordId).distinct).filter(!_.deleted)
			Ok(views.html.dashboardtags.search(term, Pagination(realPage, found, DashboardModel.configuredLimit, List(TagConverter.toTagMap[Dashboard](dashboardTags, dashboards)))))
		}
	}

	/*
	 * Search tags by name. Returns json formatted for jquery-tokeninput.
	 */
	def searchToJson(term: String) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction { implicit request =>
			val (found, matches) = DashboardTagsModel.search(term + "%", 0)
			Ok(Json.toJson(matches.map{ m =>
				Json.obj("id" -> m, "name" -> m)
			}))
		}
	}
}

object DashboardTagsController extends DashboardTagsController