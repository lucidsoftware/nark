package com.lucidchart.open.nark.request

import play.api.mvc._

/**
 * Provides helpers for creating `AppAction` values.
 */
trait AppActionBuilder {
	/**
	 * Constructs an `AppAction`.
	 *
	 * For example:
	 * {{{
	 * val echo = AppAction(parse.anyContent) { request =>
	 *	 Ok("Got request [" + request + "]")
	 * }
	 * }}}
	 *
	 * @param A the type of the request body
	 * @param bodyParser the `BodyParser` to use to parse the request body
	 * @param block the action code
	 * @return an action
	 */
	def apply[A](bodyParser: BodyParser[A])(block: AppRequest[A] => Result): Action[A] = new Action[A] {
		def parser = bodyParser
		
		def apply(request: Request[A]): Result = {
			apply(new AppRequest(request))
		}
		
		def apply(request: AppRequest[A]): Result = {
			block(request)
		}
	}

	/**
	 * Constructs an `AppAction` with default content.
	 *
	 * For example:
	 * {{{
	 * val echo = AppAction { request =>
	 *	 Ok("Got request [" + request + "]")
	 * }
	 * }}}
	 *
	 * @param block the action code
	 * @return an action
	 */
	def apply(block: AppRequest[AnyContent] => Result): Action[AnyContent] = apply(BodyParsers.parse.anyContent)(block)

	/**
	 * Constructs an `AppAction` with default content, and no request parameter.
	 *
	 * For example:
	 * {{{
	 * val hello = AppAction {
	 *	 Ok("Hello!")
	 * }
	 * }}}
	 *
	 * @param block the action code
	 * @return an action
	 */
	def apply(block: => Result): Action[AnyContent] = apply(_ => block)
}

/**
 * Helper object to create `AppAction` values.
 */
object AppAction extends AppActionBuilder
