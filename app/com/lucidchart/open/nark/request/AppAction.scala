package com.lucidchart.open.nark.request

import play.api.mvc._

import scala.concurrent.Future

/**
 * Helper object to create `AppAction` values.
 */
object AppAction extends ActionBuilder[AppRequest] {
	def invokeBlock[A](request: Request[A], block: AppRequest[A] => Future[Result]) = block(new AppRequest(request))
}
