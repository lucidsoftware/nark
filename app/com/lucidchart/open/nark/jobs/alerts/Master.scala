package com.lucidchart.open.nark.jobs.alerts

import java.util.Date
import play.api.Logger
import play.api.Play.current
import play.api.Play.configuration
import play.api.libs.concurrent.Akka
import akka.routing.RoundRobinRouter
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class DoneMessage(success: Int, failure: Int, requested: Int) {
	def found = success + failure
	def full = found == requested
}
case class DoneCleaningMessage()

object AlertMaster {
	private[alerts] val maxThreadCount = configuration.getInt("alerts.maxThreadCount").get
	private[alerts] val initialSleepSeconds = configuration.getInt("alerts.initialSleepSeconds").get
	private[alerts] val maxSleepSeconds = configuration.getInt("alerts.maxSleepSeconds").get
	private[alerts] val backoffMultiplier = configuration.getInt("alerts.backoffMultiplier").get
	private[alerts] val alertsPerWorker = configuration.getInt("alerts.alertsPerWorker").get
	private[alerts] val cleanupUnfinishedSeconds = configuration.getInt("alerts.cleanupSeconds").get
	private[alerts] val cleanupFrequency = configuration.getInt("alerts.cleanupFrequencySeconds").get
	private[alerts] val numberOfWorkers = configuration.getInt("alerts.numberOfWorkers").get

	def schedule() {
		val alertmaster = Akka.system.actorOf(Props[Master], name = "alertmaster")
		Akka.system.scheduler.scheduleOnce(1.seconds, alertmaster, "start")
		Akka.system.scheduler.schedule(cleanupFrequency.seconds, cleanupFrequency.seconds, alertmaster, "clean")
	}
}

class Master extends Actor {
	private var threadCount = 0
	private var sleepSeconds = AlertMaster.initialSleepSeconds

	private val workerRouter = {
		context.actorOf(Props[Worker].withRouter(RoundRobinRouter(AlertMaster.numberOfWorkers)), name = "workerRouter")
	}

	private def sendWork(threads: Integer) {
		Logger.trace("AlertMaster: sending " + threads + " more work messages")
		for(i <- 0 until threads) {
			workerRouter ! CheckAlertMessage(AlertMaster.alertsPerWorker)
		}
		threadCount = threadCount + threads
	}
	
	private def handleDone(m: DoneMessage) {
		threadCount = threadCount - 1
		if(m.found > 0) {
			sleepSeconds = AlertMaster.initialSleepSeconds
		}

		if (m.full) {
			val toSend = if (threadCount < AlertMaster.maxThreadCount) 2 else 1
			sendWork(toSend)
		}
		else if (m.found > 0) {
			sendWork(1)
		}
		else if (threadCount == 0) {
			sleepSeconds = math.min(sleepSeconds * AlertMaster.backoffMultiplier, AlertMaster.maxSleepSeconds)
			Logger.trace("AlertMaster: No jobs found. Sleeping for " + sleepSeconds + " seconds.")
			Akka.system.scheduler.scheduleOnce(sleepSeconds.seconds, self, "sleepdone")
		}
	}

	def receive = {
		case m: DoneMessage => {
			Logger.trace("AlertMaster: Received done message")
			handleDone(m)
		}
		case "sleepdone" => {
			Logger.trace("AlertMaster: Done sleeping!")
			sendWork(1)
		}
		case "clean" => {
			Logger.trace("AlertMaster: Sending clean message")
			workerRouter ! CleanupMessage(AlertMaster.cleanupUnfinishedSeconds)
		}
		case "start" => {
			Logger.trace("AlertMaster: Starting alert jobs")
			sendWork(configuration.getInt("alerts.initialThreadCount").get)
		}
		case _ =>
	}
}
