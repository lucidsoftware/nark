package com.lucidchart.open.nark.jobs.alerts

import com.lucidchart.open.nark.models.{AlertModel, AlertTagModel, AlertHistoryModel, SubscriptionModel, AlertTagSubscriptionModel, AlertTargetStateModel}
import com.lucidchart.open.nark.models.records.{Alert,AlertHistory,AlertState, AlertStatus, AlertType,Comparisons, AlertTargetState, User}
import com.lucidchart.open.nark.plugins
import com.lucidchart.open.nark.plugins.{AlertEvent, PluginManager}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.utils.Graphite
import akka.actor.Actor
import akka.actor.Props
import play.api.Logger
import play.api.Play.current
import play.api.Play.configuration
import scala.concurrent.{Future,Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import java.util.{Date, UUID}

case class CheckAlertMessage(limit: Int)
case class CleanupMessage(seconds: Int)

object AlertWorker {
	private[alerts] val maxConsecutiveFailures = configuration.getInt("alerts.maxConsecutiveFailures").get
	private[alerts] val secondsToCheck = configuration.getInt("alerts.secondsToCheckData").get
	private[alerts] val url = configuration.getString("application.domain").get.stripSuffix("/")
}

class Worker extends Actor with Mailer {
	private val threadId = UUID.randomUUID()

	def receive = {
		case m: CheckAlertMessage => checkAlert(m)
		case m: CleanupMessage => cleanUnfinishedAlerts(m)
		case _ =>
	}

	private def cleanUnfinishedAlerts(message: CleanupMessage) = {
		AlertModel.cleanAlertThreadsBefore(secondsFromNow(-message.seconds))
		sender ! DoneCleaningMessage()
	}

	private def checkAlert(message: CheckAlertMessage) {
		Logger.trace("AlertWorker: checking up to " + message.limit + " messages")
		try {
			val alerts = AlertModel.takeNextAlertsToCheck(threadId, message.limit)(checkAlerts)
			val results = alerts.groupBy { case (alert, status) => status }

			sender ! DoneMessage(
				results.get(AlertStatus.success).map(_.size).getOrElse(0),
				results.get(AlertStatus.failure).map(_.size).getOrElse(0),
				message.limit
			)
		}
		catch {
			case e: Exception => {
				sender ! DoneMessage(0, 0, message.limit)
			}
		}
	}

	private def checkAlerts(alertsToCheck : List[Alert]) : Map[Alert, AlertStatus.Value] = {
		alertsToCheck.map { alert =>
			Logger.trace("AlertWorker: checking alert named '" + alert.name + "'")
			try {
				// get all targets
				val returnedData = Graphite.synchronousData(alert.target, alert.dataSeconds)
				val filteredNullsData = (if (alert.dropNullTargets) returnedData.filterEmptyTargets() else returnedData).dropLastNullPoints(alert.dropNullPoints)

				// get current target states for this alert
				val alertTargetStatesByTarget = AlertTargetStateModel.getTargetStatesByAlertID(alert.id).map { state => (state.target, state) }.toMap

				// for each target
				val previousStates = filteredNullsData.targets.map { target => (target, alertTargetStatesByTarget.get(target.target).map(_.state).getOrElse(AlertState.normal)) }.toMap

				val currentStates = filteredNullsData.targets.map { target =>
					val lastValue = target.datapoints.last.value

					// If there are no datapoints, use the previous state
					val currentState = if(target.datapoints.isEmpty) {
							previousStates(target)
						}
						else if(Comparisons.nullables.contains(alert.comparison)) {
							// If the comparison is for a null value, get the state
							getState(alert, lastValue)
						}
						else if(target.datapoints.last.value.isDefined) {
							// If the last point exists get the state
							getState(alert, lastValue)
						} else {
							// If the last point does not exist, assume the state is the same as previous
							previousStates(target)
						}


					(target, currentState)
				}.toMap

				val changedStates = filteredNullsData.targets.filter { target => previousStates(target) != currentStates(target) }

				if(!changedStates.isEmpty) {
					val subscribers = getSubscribers(alert)
					
					//send out emails
					val alertHistories = changedStates.map { currentTarget =>
						val previousState = previousStates(currentTarget)
						val currentState = currentStates(currentTarget)
						val lastValue = currentTarget.datapoints.last.value
						//filter out subscribers that actually care about this.
						val emails = subscribers.map { subscriber =>
							val includeError = (previousState == AlertState.error || currentState == AlertState.error) && subscriber.errorEnable
							val includeWarn = !includeError && (previousState == AlertState.warn || currentState == AlertState.warn) && subscriber.warnEnable
							if (includeError) {
								Some(subscriber.errorAddress)
							} else if (includeWarn) {
								Some(subscriber.warnAddress)
							} else {
								None
							}
						}.collect {
							case Some(email) => email
						}

						val htmlMessage = ((currentState) match {
							case (AlertState.normal) => views.html.emails.alert(true,currentTarget,alert,previousState.toString,currentState.toString,AlertWorker.url).toString
							case (_) => views.html.emails.alert(false,currentTarget,alert,previousState.toString,currentState.toString,AlertWorker.url) .toString
						})
						val textMessage = ((currentState) match {
							case (AlertState.normal) =>views.txt.emails.alert(true,currentTarget,alert,previousState.toString,currentState.toString,AlertWorker.url).toString.trim
							case (_) => (views.txt.emails.alert(false,currentTarget,alert,previousState.toString,currentState.toString,AlertWorker.url).toString ) .toString.trim
						})
						val subject = "["+currentState.toString+"] "+alert.name+":"+ lastValue.getOrElse("null")
						val emailsSent = sendEmails(emails, subject, textMessage, htmlMessage)
						
						// send alerts to the plugins
						val alertTags = AlertTagModel.findTagsForAlert(alert.id).map(_.tag).toSet
						val alertEvent = AlertEvent(
							alert.id,
							alert.name,
							alert.target,
							currentTarget.target,
							plugins.Comparisons(alert.comparison.id),
							alert.warnThreshold,
							alert.errorThreshold,
							lastValue,
							plugins.AlertState(previousState.id),
							plugins.AlertState(currentState.id)
						)

						PluginManager.alertPlugins.foreach { plugin =>
							if (plugin.tags.intersect(alertTags).size > 0) {
								PluginMaster.send(PluginRequest(plugin, alertEvent))
							}
						}

						new AlertHistory(alert.id, currentTarget.target, currentState, emailsSent, lastValue)
					}


					AlertHistoryModel.createAlertHistory(alertHistories)
				}

				val currentAlertTargetStates = currentStates.map{ case (target, state) => new AlertTargetState(alert.id, target.target, state) }.toList
				AlertTargetStateModel.setAlertTargetStates(alert, currentAlertTargetStates)

				(alert.copy(worstState = getWorstState(currentStates.values.toList)), AlertStatus.success)
			} catch {
				case e : Exception => {
					Logger.error("AlertWorker: error processing alert named '" + alert.name + "'", e)
					if((alert.consecutiveFailures + 1) >= AlertWorker.maxConsecutiveFailures) {
						val subscribers = getSubscribers(alert)
						val emails = subscribers.map{ subscriber =>
							if(subscriber.errorEnable) {
								Some(subscriber.errorAddress)
							}
							else if (subscriber.warnEnable) {
								Some(subscriber.warnAddress)
							}
							else {
								None
							}
						}.collect {
							case Some(email) => email
						}
						val failed_text =  "Check for alert " + alert.name + " has failed " + AlertWorker.maxConsecutiveFailures + " times."
						sendEmails(emails,"error: Failed to check alert " + alert.name + " .", failed_text, failed_text )
					}
					(alert, AlertStatus.failure)
				}
			}
		}.toMap
	}

	private def getWorstState(states: List[AlertState.Value]) : AlertState.Value = {
		if(states.contains(AlertState.error)) {
			AlertState.error
		}
		else if(states.contains(AlertState.warn)) {
			AlertState.warn
		}
		else {
			AlertState.normal
		}
	}

	private def getSubscribers(alert: Alert) : List[User] = {
		val alertTags = AlertTagModel.findTagsForAlert(alert.id).map(_.tag)
		val tagSubscribers = AlertTagSubscriptionModel.getSubscriptionsByTag(alertTags).collect {
			case s if (s.userOption.isDefined && s.subscription.active) => s.userOption.get
		}

		val alertIds = 	if(alert.dynamicAlertId.isDefined) { List(alert.id, alert.dynamicAlertId.get) } else { List(alert.id) }
		val alertSubscribers = SubscriptionModel.getSubscriptionsByAlerts(alertIds).collect {
			case s if (s.userOption.isDefined && s.subscription.active) => s.userOption.get
		}

		(tagSubscribers ++ alertSubscribers).distinct
	}

	private def crossesThreshold(valueOption: Option[BigDecimal], threshold:BigDecimal, comparison: Comparisons.Value): Boolean = {
		comparison match {
			// Threshold operators
			case Comparisons.<  => (valueOption.isDefined && valueOption.get <  threshold)
			case Comparisons.<= => (valueOption.isDefined && valueOption.get <= threshold)
			case Comparisons.== => (valueOption.isDefined && valueOption.get == threshold)
			case Comparisons.>  => (valueOption.isDefined && valueOption.get >  threshold)
			case Comparisons.>= => (valueOption.isDefined && valueOption.get >= threshold)
			case Comparisons.!= => (valueOption.isDefined && valueOption.get != threshold)

			// Nullable Operators
			case Comparisons.isNull    => (valueOption.isEmpty   && threshold > 0)
			case Comparisons.isNotNull => (valueOption.isDefined && threshold > 0)
		}
	}

	private def secondsFromNow(seconds: Int) : Date = {
		new Date(new Date().getTime + (1000 * seconds))
	}

	private def getState(alert:Alert, lastDataPointOption:Option[BigDecimal]) : AlertState.Value = {
		if(crossesThreshold(lastDataPointOption, alert.errorThreshold, alert.comparison)) {
			AlertState.error
		}
		else if (crossesThreshold(lastDataPointOption, alert.warnThreshold, alert.comparison)) {
			AlertState.warn
		}
		else {
			AlertState.normal
		}
	}
}