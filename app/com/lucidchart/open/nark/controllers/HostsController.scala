package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction, DashboardAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.HostModel
import com.lucidchart.open.nark.models.records.{Host, Pagination}

class HostsController extends AppController {
	/**
	 * Display a paginated list of hosts
	 */
	def search(term: String, page: Int) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction { implicit request =>
			val realPage = page.max(1)
			val (found, matches) = HostModel.search(term, realPage - 1)
			Ok(views.html.hosts.search(term, Pagination[Host](realPage, found, HostModel.configuredLimit, matches)))
		}
	}
}

object HostsController extends HostsController
