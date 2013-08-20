package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction, DashboardAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.HostModel
import com.lucidchart.open.nark.models.records.Host

class HostsController extends AppController {
	/**
	 * Display a paginated list of hosts
	 */
	def search(term: String, page: Int) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction { implicit request =>
			val (found, matches) = HostModel.search(term, page)
			Ok(views.html.hosts.search(term, page, HostModel.configuredLimit, found, matches))
		}
	}
}

object HostsController extends HostsController
