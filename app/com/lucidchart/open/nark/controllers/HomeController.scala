package com.lucidchart.open.nark.controllers

import play.api.mvc.Action

import com.lucidchart.open.nark.request.AppAction
import com.lucidchart.open.nark.request.AuthAction
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.DashboardModel

class HomeController extends AppController {
	/**
	 * Home / Intro / Welcome page.
	 * Authentication not required
	 */
	def index = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction { implicit request =>
			Ok(views.html.application.index(DashboardModel.findAll()))
		}
	}
}

object HomeController extends HomeController
