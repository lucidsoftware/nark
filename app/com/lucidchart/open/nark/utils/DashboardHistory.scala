package com.lucidchart.open.nark.utils

import com.lucidchart.open.nark.models.records.Dashboard

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

case class DashboardHistoryItem(dashboardId: UUID, url: String, name: String) {
	def this(d: Dashboard) = this(d.id, d.url, d.name)
}

object DashboardHistory {
	protected val maxItems = configuration.getInt("dashboardhistory.count").get

	object DashboardHistoryCookieBaker extends CookieBaker[List[DashboardHistoryItem]] {
		val COOKIE_NAME = "dashboardhistory"
		val emptyCookie = Nil
		override val secure = false
		override val isSigned = true
		override val httpOnly = true
		override val path = configuration.getString("application.context").get
		override val domain = None
		override val maxAge = Some(315360000)
		
		def deserialize(data: Map[String, String]) = {
			val nonEmptyData = data.filter(!_._1.isEmpty)
			val unserialized = for (i <- 0 until nonEmptyData.size) yield {
				val itemString = nonEmptyData(i.toString)
				val parts = itemString.split("\\|", 3)
				DashboardHistoryItem(
					UUID.fromString(parts(0)),
					parts(1),
					parts(2)
				)
			}

			unserialized.toList
		}

		def serialize(list: List[DashboardHistoryItem]) = {
			list.zipWithIndex.map { case (item, index) =>
				val serialized = "%s|%s|%s".format(
					item.dashboardId,
					item.url,
					item.name
				)

				(index.toString, serialized)
			}.toMap
		}
	}

	/**
	 * Returns the dashboard history, last viewed dashboard first
	 */
	def getHistory()(implicit request: RequestHeader): List[DashboardHistoryItem] = {
		DashboardHistoryCookieBaker.decodeFromCookie(request.cookies.get(DashboardHistoryCookieBaker.COOKIE_NAME))
	}

	/**
	 * Add an item to the beginning of the history.
	 * Return a cookie that represents the new history
	 */
	def addToHistory(history: List[DashboardHistoryItem], item: DashboardHistoryItem): Cookie = {
		val newlist = item +: history.filter(_ != item)
		val maxed = newlist.take(maxItems)
		DashboardHistoryCookieBaker.encodeAsCookie(maxed)
	}

	/**
	 * Add an item to the beginning of the history found in the request cookie.
	 * Return a cookie that represents the new history
	 */
	def addToHistory(request: RequestHeader, item: DashboardHistoryItem): Cookie = {
		val history = getHistory()(request)
		addToHistory(history, item)
	}

	/**
	 * Remove an item from the history.
	 * Return a cookie that represents the new history
	 */
	def removeFromHistory(history: List[DashboardHistoryItem], id: UUID): Cookie = {
		val newlist = history.filter(_.dashboardId != id)
		DashboardHistoryCookieBaker.encodeAsCookie(newlist)
	}

	/**
	 * Remove an item from the history.
	 * Return a cookie that represents the new history
	 */
	def removeFromHistory(history: List[DashboardHistoryItem], url: String): Cookie = {
		val newlist = history.filter(_.url != url)
		DashboardHistoryCookieBaker.encodeAsCookie(newlist)
	}

	/**
	 * Remove an item from the history found in the request cookie.
	 * Return a cookie that represents the new history
	 */
	def removeFromHistory(request: RequestHeader, id: UUID): Cookie = {
		val history = getHistory()(request)
		removeFromHistory(history, id)
	}

	/**
	 * Remove an item from the history found in the request cookie.
	 * Return a cookie that represents the new history
	 */
	def removeFromHistory(request: RequestHeader, url: String): Cookie = {
		val history = getHistory()(request)
		removeFromHistory(history, url)
	}
}
