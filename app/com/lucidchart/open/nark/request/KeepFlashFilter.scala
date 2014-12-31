package com.lucidchart.open.nark.request

import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class KeepFlashFilter extends Filter {
	private def keepFlashForHttpStatus(status: Int) = {
		status < 200 || status >= 300
	}

	private def keepFlash(request: RequestHeader, result: Result): Result = {
		if (!request.flash.isEmpty && keepFlashForHttpStatus(result.header.status) && !result.header.headers.contains(Flash.COOKIE_NAME)) {
			result.withCookies(Flash.encodeAsCookie(request.flash))
		}
		else {
			result
		}
	}

	override def apply(next: RequestHeader => Future[Result])(request: RequestHeader) = {
		next(request).map { result =>
			keepFlash(request, result)
		}
	}
}

object KeepFlashFilter {
	def apply() = {
		new KeepFlashFilter
	}
}
