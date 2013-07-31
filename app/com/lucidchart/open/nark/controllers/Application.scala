package com.lucidchart.open.nark.controllers

import play.api.mvc.Action

class Application extends AppController {
	def index = Action {
		Ok("hello")
	}
}

object Application extends Application
