package com.lucidchart.open.nark.utils

import java.util.Date
import java.util.UUID

import scala.util.Random

import play.api.Play.configuration
import play.api.Play.current
import play.api.mvc.Cookie
import play.api.mvc.CookieBaker
import play.api.mvc.Cookies
import play.api.mvc.DiscardingCookie
import play.api.mvc.RequestHeader
import play.api.mvc.{Session => PlaySession}

object Auth {
	protected val applicationSecret = configuration.getString("application.secret").get
	
	/**
	 * Copied & modified from play source.
	 * 
	 * @see play.api.mvc.Session
	 */
	class SessionCookieBaker extends CookieBaker[PlaySession] {
		val COOKIE_NAME = configuration.getString("auth.cookie.name").get
		val emptyCookie = new PlaySession
		override val secure = configuration.getBoolean("auth.cookie.secure").get
		override val isSigned = true
		override val httpOnly = true
		override val path = configuration.getString("auth.cookie.path").get
		override val domain = configuration.getString("auth.cookie.domain")
		override val maxAge: Option[Int] = None
		val ttl = configuration.getInt("auth.cookie.ttl").get * 1000L
		
		def deserialize(data: Map[String, String]) = new PlaySession(data)
		def serialize(session: PlaySession) = session.data
		def maxTtlDate = new Date(System.currentTimeMillis + ttl)
	}
	
	protected class SessionCookieBakerRemembered extends SessionCookieBaker {
		override val maxAge = Some(configuration.getInt("auth.cookie.remembermeMaxAge").get)
		override val ttl = maxAge.get * 1000L
	}
	
	object SessionCookieBaker extends SessionCookieBaker
	object SessionCookieBakerRemembered extends SessionCookieBakerRemembered
	
	/**
	 * The cookie used to discard auth headers
	 */
	def discardingCookie = DiscardingCookie(
		SessionCookieBaker.COOKIE_NAME,
		SessionCookieBaker.path,
		SessionCookieBaker.domain,
		SessionCookieBaker.secure
	)
	
	/**
	 * Hash a password for user logins
	 * 
	 * @param password
	 * @param salt Unique salt per login
	 * @return hashed password
	 */
	def hashPassword(password: String, salt: String) = HashHelper.sha1(applicationSecret + salt + password)
	
	/**
	 * Generate a salt for a password hash
	 * 
	 * @param length
	 * @return string
	 */
	def generateSalt(length: Int) = new String((for (i <- 0 until length) yield Random.nextPrintableChar()).toArray)
	
	/**
	 * Generate the authentication cookie to send to the client.
	 * 
	 * @param userId ID of the user that is logged in
	 * @param rememberme True if the user does not want to login frequently
	 * @param userAgent from the request
	 * @return cookie
	 */
	def generateAuthCookie(userId: UUID, rememberme: Boolean, userAgent: String) = {
		val handler = if (rememberme) SessionCookieBakerRemembered else SessionCookieBaker
		val session = new Session(userId, new Date(), handler.maxTtlDate, rememberme, HashHelper.md5(userAgent))
		handler.encodeAsCookie(session.toSession)
	}
	
	/**
	 * Parses the auth cookie given. Returns the session if the cookie
	 * is valid.
	 * 
	 * @param cookie
	 * @param userAgent from the request
	 * @return session info
	 */
	def parseAuthCookie(cookie: Option[Cookie], userAgent: String) = {
		Session.fromCookie(cookie, HashHelper.md5(userAgent))
	}
	
	/**
	 * Finds the auth cookie, parses it, and returns the session if the cookie
	 * is valid.
	 * 
	 * @param cookies
	 * @param userAgent from the request
	 * @return session info
	 */
	def parseAuthCookie(cookies: Cookies, userAgent: String): Option[Session] = {
		parseAuthCookie(cookies.get(SessionCookieBaker.COOKIE_NAME), userAgent)
	}
	
	/**
	 * Finds the auth cookie, parses it, and returns the session if the cookie
	 * is valid.
	 * 
	 * @param request
	 * @return session info
	 */
	def parseAuthCookie(request: RequestHeader): Option[Session] = {
		parseAuthCookie(
			request.cookies,
			request.headers.get("User-Agent").getOrElse("")
		)
	}
	
	/**
	 * Auth Session
	 * 
	 * Contains user ID and created date for the session
	 */
	case class Session(userId: UUID, created: Date, expires: Date, rememberme: Boolean, userAgentHash: String) {
		/**
		 * Check to see if the session has expired (regardless of whether
		 * the client has enforced it or not).
		 */
		def expired = expires.before(new Date())
		
		/**
		 * Convert this AuthSession into a PlaySession
		 */
		def toSession = PlaySession(Map(
			Session.userIdKey     -> userId.toString,
			Session.createdKey    -> (created.getTime / 1000).toString,
			Session.expiresKey    -> (expires.getTime / 1000).toString,
			Session.remembermeKey -> (if (rememberme) "1" else "0"),
			Session.userAgentKey  -> userAgentHash
		))
	}
	
	object Session {
		private val userIdKey     = "u"
		private val createdKey    = "c"
		private val expiresKey    = "e"
		private val remembermeKey = "r"
		private val userAgentKey  = "a"
		
		/**
		 * Parse the details from a cookie, return the auth session information, if found.
		 * 
		 * @param cookie
		 * @return session
		 */
		def fromCookie(cookie: Option[Cookie], userAgentHash: String): Option[Session] = {
			val playSession = SessionCookieBaker.decodeFromCookie(cookie)
			if (playSession.isEmpty) {
				None
			}
			else {
				try {
					val session = new Session(
						UUID.fromString(playSession(userIdKey)),
						new Date(playSession(createdKey).toInt * 1000L),
						new Date(playSession(expiresKey).toInt * 1000L),
						playSession(remembermeKey) == "1",
						playSession(userAgentKey)
					)
					
					if (session.expired) {
						throw new Exception
					}
					
					if (session.userAgentHash != userAgentHash) {
						throw new Exception
					}
					
					Some(session)
				}
				catch {
					case e: Exception => None
				}
			}
		}
	}
}
