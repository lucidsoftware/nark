package com.lucidchart.open.nark.request

import com.lucidchart.open.nark.controllers.routes
import com.lucidchart.open.nark.models.UserModel
import com.lucidchart.open.nark.models.records.User
import com.lucidchart.open.nark.utils.Auth
import com.lucidchart.open.nark.utils.StatsD

import play.api.libs.iteratee.Done
import play.api.mvc._
import play.api.mvc.Results._

/**
 * Provides helpers for creating `AuthAction` values.
 */
trait AuthActionBuilder {
	/**
	 * Action that checks for an active session and performs one of
	 * two subactions (depending on existence of valid session).
	 * 
	 * @param out action to call if user is logged out
	 * @param in action to call if user is logged in
	 */
	def logged(out: => EssentialAction)(in: (Auth.Session) => EssentialAction): EssentialAction = new EssentialAction {
		def apply(requestHeader: RequestHeader) = {
			Auth.parseAuthCookie(requestHeader) match {
				case None => out(requestHeader)
				case Some(session) => in(session)(requestHeader)
			}
		}
	}
	
	/**
	 * The action used for the loggedOut portion of the loggedIn function
	 */
	def defaultOutAction = EssentialAction { requestHeader =>
		StatsD.increment("authaction.notloggedin")
		val isAjax = requestHeader.headers.get("X-Requested-With").map(_ == "XMLHttpRequest").getOrElse(false)
		
		if (isAjax) {
			Done(Unauthorized)
		}
		else {
			val origDestCookie = requestHeader.method match {
				case "GET" => Cookie("origdest", requestHeader.path + "?" + requestHeader.rawQueryString)
				case _ => DiscardingCookie("origdest").toCookie
			}
			
			// we discard the auth cookie here mostly for dev purposes
			Done(Redirect(routes.HomeController.index()).discardingCookies(Auth.discardingCookie).withCookies(origDestCookie))
		}
	}
	
	/**
	 * Action to ensure the requester is logged in.
	 */
	def loggedIn(block: => EssentialAction): EssentialAction = loggedIn { session => block }
	
	/**
	 * Action to ensure the requester is logged in.
	 */
	def loggedIn(block: (Auth.Session) => EssentialAction): EssentialAction = logged(defaultOutAction)(block)
	
	/**
	 * The action used for the loggedIn portion of the loggedOut function
	 */
	def defaultInAction(session: Auth.Session) = EssentialAction { requestHeader =>
		StatsD.increment("authaction.notloggedout")
		Done(Redirect(routes.HomeController.index()))
	}
	
	/**
	 * Action to ensure the requester is logged out.
	 */
	def loggedOut(block: => EssentialAction): EssentialAction = logged(block)(defaultInAction)

	/**
	 * Action to check whether or not a user is logged in
	 */
	def maybeAuthenticatedUser(block: (Option[User]) => EssentialAction): EssentialAction = logged {
		block(None)
	} { session =>
		block(UserModel.findUserByID(session.userId))
	}
	
	/**
	 * Action to ensure an authenticated user is logged in
	 */
	def authenticatedUser(block: () => EssentialAction): EssentialAction = authenticatedUser { (session, user) => block() }
	
	/**
	 * Action to ensure an authenticated user is logged in
	 */
	def authenticatedUser(block: (User) => EssentialAction): EssentialAction = authenticatedUser { (session, user) => block(user) }
	
	/**
	 * Action to ensure an authenticated user is logged in
	 */
	def authenticatedUser(block: (Auth.Session, User) => EssentialAction): EssentialAction = loggedIn { session =>
		UserModel.findUserByID(session.userId) match {
			case Some(user) => block(session, user)
			case None => defaultOutAction
		}
	}
}

/**
 * Helper object to create `AuthAction` values.
 */
object AuthAction extends AuthActionBuilder
