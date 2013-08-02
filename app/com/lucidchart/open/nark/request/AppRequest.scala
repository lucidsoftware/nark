package com.lucidchart.open.nark.request

import play.api.mvc.Request
import play.api.mvc.WrappedRequest
import play.api.Play
import play.api.Play.current

class AppRequest[A](val request: Request[A]) extends WrappedRequest(request) {
	def c = Play.configuration

	def TOKEN_NAME: String = c.getString("csrf.token.name").getOrElse("csrf")
	def COOKIE_NAME: Option[String] = c.getString("csrf.cookie.name") // If None, we search for TOKEN_NAME in play session

	def tokenValue = {
		request.cookies.get(TOKEN_NAME).map(_.value).getOrElse("")
	}
}
