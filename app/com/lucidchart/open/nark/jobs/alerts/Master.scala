package com.lucidchart.open.nark.jobs.alerts

import java.util.Date
import play.api.Play.current
import play.api.Play.configuration
import play.api.libs.concurrent.Akka
import akka.routing.RoundRobinRouter
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class DoneMessage(success: Integer, failure: Integer)
case class DoneCleaningMessage()

class Master extends Actor {
	private var threadCount = configuration.getInt("alerts.initialThreadCount").get
	private var maxThreadCount = configuration.getInt("alerts.maxThreadCount").get

	private val workerRouter = setupWorkerRouter()
	private val initialSleepSeconds = configuration.getInt("alerts.initialSleepSeconds").get
	private var sleepSeconds = initialSleepSeconds
	private val maxSleepSeconds = configuration.getInt("alerts.maxSleepSeconds").get
	private val backoffMultiplier = configuration.getInt("alerts.backoffMultiplier").get
	private val alertsPerWorker = configuration.getInt("alerts.alertsPerWorker").get
	private val cleanupUnfinishedSeconds = configuration.getInt("alerts.cleanupSeconds").get
	private var cleanupFrequency = configuration.getInt("alerts.cleanupFrequencySeconds").get
	private var nextCleanup = new Date()


	private def setupWorkerRouter(): ActorRef = {
		val numberOfWorkers = configuration.getInt("alerts.numberOfWorkers").get
		context.actorOf(Props[Worker].withRouter(RoundRobinRouter(numberOfWorkers)), name = "workerRouter")
	}

	private def sendWork(threads: Integer) {
		if(nextCleanup.before(new Date())) {
			nextCleanup = new Date(new Date().getTime + (1000 * cleanupFrequency))

			for(i <- 1 to threads-1) {
				workerRouter ! CheckAlertMessage(alertsPerWorker)
			}
			workerRouter ! CleanupMessage(cleanupUnfinishedSeconds)
		} else {
			for(i <- 1 to threads) {
				workerRouter ! CheckAlertMessage(alertsPerWorker)
			}
		}
	}
	
	private def handleDone(m: DoneMessage) {
		if(m.success > 0 || m.failure > 0) {
			sleepSeconds = initialSleepSeconds
		}
		if(m.success > 0) {
			if(threadCount < maxThreadCount) {
				threadCount = threadCount + 1
				sendWork(2)
			}
			else {
				sendWork(1)
			}
		} else if(m.failure > 0) {
			sendWork(1)
		} else {

			if(threadCount > 1) {
				threadCount = threadCount-1
			}
			else {
				sleepSeconds = math.min(sleepSeconds * backoffMultiplier, maxSleepSeconds)
				// Akka.system.scheduler.scheduleOnce(Duration(sleepSeconds, SECONDS), workerRouter,  "checkAlert" )
				Thread.sleep(sleepSeconds * 1000)
				sendWork(1)
			}

		}
	}

	def handleDone(m: DoneCleaningMessage) {
		sendWork(1)
	}

	def receive = {
		case m: DoneMessage => handleDone(m)
		case m: DoneCleaningMessage => handleDone(m)
		case "start" => sendWork(threadCount)
	}
}