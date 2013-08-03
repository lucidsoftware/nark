package com.lucidchart.open.nark.offline

import akka.actor.Actor
import akka.actor.Props

import com.lucidchart.open.nark.utils.Graphite
import com.lucidchart.open.nark.models.HostModel
import com.lucidchart.open.nark.models.MutexModel
import com.lucidchart.open.nark.models.records.Host
import com.lucidchart.open.nark.models.records.HostState

import java.util.Date

import scala.collection.JavaConversions
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import play.api.libs.concurrent.Akka
import play.api.Logger
import play.api.Play
import play.api.Play.current
import play.api.Play.configuration

case class HostDiscovererPattern(target: String, extractString: String, reverseHostname: Boolean) {
	val extractor = extractString.r
}

object HostDiscoverer {
	def schedule() = {
		val stateChangeTime = configuration.getInt("hostdiscovery.statechangetime").get
		val frequency = configuration.getInt("hostdiscovery.frequency").get
		val patterns = JavaConversions.iterableAsScalaIterable(configuration.getConfigList("hostdiscovery.patterns").get).toList.map { c =>
			HostDiscovererPattern(
				c.getString("target").get,
				c.getString("extract").get,
				c.getBoolean("reverse").get
			)
		}

		val instance = Akka.system.actorOf(Props(new HostDiscoverer(stateChangeTime, patterns)), name = "HostDiscoverer")
		Akka.system.scheduler.schedule(frequency.seconds, frequency.seconds, instance, "go")
	}
}

class HostDiscoverer(stateChangeTime: Int, patterns: List[HostDiscovererPattern]) extends Actor {
	private var runOnce = false

	protected def discoverNew(start: Date) {
		patterns.foreach { pattern =>
			Logger.info("HostDiscovery pattern " + pattern.target)
			Graphite.data(pattern.target, stateChangeTime).targets.foreach { target =>
				try {
					val pattern.extractor(extractedName) = target.target
					val name = if (pattern.reverseHostname) extractedName.split("\\.").reverse.mkString(".") else extractedName
					Logger.debug("HostDiscovery Discovered " + name)

					val up = !target.datapoints.exists(_.value.isEmpty)
					val down = !target.datapoints.exists(_.value.isDefined)
					val state = if (up) HostState.up else if (down) HostState.down else HostState.limbo

					val host = new Host(name, state, start)
					HostModel.upsert(host)

					Logger.debug("HostDiscovery Updated " + name)
				}
				catch {
					case e: Exception => {
						Logger.error("HostDiscovery Error while discovering " + target.target, e)
					}
				}
			}
		}
	}

	protected def cleanOld(start: Date) {
		Logger.info("HostDiscovery cleaning before " + start.toString)
		HostModel.cleanBefore(start)
	}

	def receive = {
		case _ => {
			if (Play.isProd || (Play.isDev && !runOnce)) {
				// we do this in a mutex to avoid overloading graphite needlessly
				runOnce = MutexModel.lock("hostdiscovery", 0, false) {
					Logger.info("HostDiscovery Starting")
					val start = new Date()
					discoverNew(start)
					cleanOld(start)
					Logger.info("HostDiscovery done!")
					true
				}
			}
		}
	}
}
