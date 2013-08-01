package com.lucidchart.open.nark.controllers

import play.api.mvc.Action

import com.lucidchart.open.nark.request.AppAction
import com.lucidchart.open.nark.request.AuthAction
import com.lucidchart.open.nark.views

class Application extends AppController {
	def index = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction { implicit request =>
			Ok(views.html.application.index())
		}
	}
}

object Application extends Application
