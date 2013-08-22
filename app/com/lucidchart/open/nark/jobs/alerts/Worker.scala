package com.lucidchart.open.nark.jobs.alerts

import com.lucidchart.open.nark.models.{AlertModel, AlertTagModel, AlertHistoryModel, SubscriptionModel, TagSubscriptionModel, AlertTargetStateModel}
import com.lucidchart.open.nark.models.records.{Alert,AlertHistory,AlertState, AlertStatus, AlertType,Comparisons, AlertTargetState, User}
import com.lucidchart.open.nark.utils.Graphite
import akka.actor.Actor
import akka.actor.Props
import play.api.Play.current
import play.api.Play.configuration
import scala.concurrent.{Future,Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import java.util.Properties
import javax.mail._
import internet._

import java.util.{Date, UUID}

case class CheckAlertMessage(limit: Int)

class Worker extends Actor {
	private val threadId = UUID.randomUUID()
	private val maxConsecutiveFailures = configuration.getInt("alerts.maxConsecutiveFailures").get
	private val consecutiveFailuresMultiplier = configuration.getInt("alerts.consecutiveFailuresMultiplier").get
	private val secondsToCheck = configuration.getInt("alerts.secondsToCheckData").get
	private val url = configuration.getString("application.domain").get

	private val smtpHostName = configuration.getString("mailer.smtp.host").get
	private val smtpAuthUser = configuration.getString("mailer.smtp.user").get
	private val smtpAuthPwd  = configuration.getString("mailer.smtp.pass").get
	private val smtpPort  = configuration.getInt("mailer.smtp.port").get
	private val fromEmail  = configuration.getString("mailer.fromemail").get
	private val mailerEnabled = configuration.getBoolean("mailer.enabled").get

	class SMTPAuthenticator extends javax.mail.Authenticator {
		override def getPasswordAuthentication() = {
			val username = smtpAuthUser
			val password = smtpAuthPwd
			new PasswordAuthentication(username, password)
		}
	}

	private val port : java.lang.Integer = smtpPort
	private val props = new Properties()
	props.put("mail.transport.protocol", "smtp")
	props.put("mail.smtp.host", smtpHostName)
	props.put("mail.smtp.port", port)
	props.put("mail.smtp.auth", "true")

	private val auth = new SMTPAuthenticator()

	def receive = {
		case m: CheckAlertMessage => checkAlert(m)
	}

	private def checkAlert(message: CheckAlertMessage) {
		val alerts = AlertModel.takeNextAlertsToCheck(threadId, message.limit)(checkAlerts)
		val results = alerts.partition(alertStatus => alertStatus._2 == AlertStatus.success)

		sender ! DoneMessage(results._1.size, results._2.size)
	}

	private def checkAlerts(alertsToCheck : List[Alert]) : Map[Alert, AlertStatus.Value] = {
		alertsToCheck.map { alert =>
			try {
				// get all targets
				val returnedDataFuture = Graphite.data(List(alert.target), secondsToCheck)
				val returnedData = Await.result(returnedDataFuture, 4 seconds).filterEmptyTargets()

				// get current target states for this alert
				val alertTargetStates = AlertTargetStateModel.getTargetStatesByAlertID(alert.id)
				
				// for each target
				val nonNullTargets = returnedData.targets.filter(_.datapoints.last.value.isDefined)
				val previousStates = nonNullTargets.map { target => (target, alertTargetStates.find(_.target == target.target).getOrElse(new AlertTargetState(alert.id, target.target, AlertState.normal)).state) }.toMap
				val currentStates = nonNullTargets.map { target => (target, getState(alert, target.datapoints.last.value.get)) }.toMap
				val changedStates = nonNullTargets.filter { target => previousStates(target) != currentStates(target) }
				
				if(!changedStates.isEmpty) {
					val subscribers = getSubscribers(alert)
					
					//send out emails
					changedStates.foreach { changedState =>
						val previousState = previousStates(changedState)
						val currentState = currentStates(changedState)

						//filter out subscribers that actually care about this.
						val emails = subscribers.foldLeft[List[String]](Nil) { (ret, subscriber) =>
							val includeError = (previousState == AlertState.error || currentState == AlertState.error) && subscriber.errorEnable
							val includeWarn = !includeError && (previousState == AlertState.warn || currentState == AlertState.warn) && subscriber.warnEnable
							if (includeError) {
								subscriber.errorAddress  :: ret
							} else if (includeWarn) {
								subscriber.warnAddress :: ret
							} else {
								ret
							}			
						}.distinct

						val message = ((currentState) match {
							case (AlertState.normal) => (changedState.target + " recovered. \n Went from " + previousState.toString + " to " + currentState.toString)
							case (_) => (currentState + " : " + changedState.target + " went from " + previousState.toString + " to " + currentState.toString) 
						}) + "\n <a href='" + url + "alert/" + alert.id + "/view'>View Alert</a>"

						val subject = (currentState.toString + " : " + alert.name + " : " + changedState.target)

						val emailsSent = sendEmails(emails, subject, message)
						AlertHistoryModel.createAlertHistory(new AlertHistory(alert.id, changedState.target, currentState, emailsSent))
					}
				}

				val currentAlertTargetStates = nonNullTargets.map{target => new AlertTargetState(alert.id, target.target, currentStates(target)) }
				AlertTargetStateModel.createAlertTargetStates(currentAlertTargetStates)
				AlertTargetStateModel.deleteAlertTargetStatesBefore(alert.id, secondsFromNow(-60))

				(alert.copy(worstState = getWorstState(currentStates.map(_._2).toList), consecutiveFailures = 0), AlertStatus.success)
			} catch {
				case e : Exception => {
					if((alert.consecutiveFailures + 1) >= maxConsecutiveFailures) {
						val subscribers = getSubscribers(alert)
						val emails = subscribers.map{ subscriber => if(subscriber.errorEnable) subscriber.errorAddress else if (subscriber.warnEnable) subscriber.warnAddress else "" }.filter(!_.isEmpty)
						sendEmails(emails, "error: Failed to check alert " + alert.name + " .", "Check for alert " + alert.name + " has failed " + maxConsecutiveFailures + " times.")
						(alert.copy(nextCheck = secondsFromNow(alert.frequency*consecutiveFailuresMultiplier), consecutiveFailures = 0), AlertStatus.failure)
					} else {
						(alert.copy(nextCheck = secondsFromNow(alert.frequency), consecutiveFailures = (alert.consecutiveFailures + 1)), AlertStatus.failure)
					}
				}
			}
		}.toMap
	}

	private def sendEmails(toEmails: List[String], subject: String, body: String) : Int = {
		if(mailerEnabled && !toEmails.isEmpty) {
			val mailSession = Session.getInstance(props, auth)

			// uncomment for debugging infos to stdout
			// mailSession.setDebug(true)

			val transport = mailSession.getTransport()

			val message = new MimeMessage(mailSession)
			
			message.setFrom(new InternetAddress(fromEmail, "nark"))
			message.setReplyTo(Array(new InternetAddress(fromEmail, "nark")))
			
			message.setSubject(subject)

			toEmails.map{email => message.addRecipient(Message.RecipientType.TO, new InternetAddress(email, ""))}

			message.setContent(body, "text/html")
			message.saveChanges()
			transport.connect()
			transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO))
			transport.close()
			// no exceptions = success
			
			toEmails.length
		} else {
			0
		}
	}

	private def getWorstState(states: List[AlertState.Value]) : AlertState.Value = {
		if(states.contains(AlertState.error))
			AlertState.error
		else if(states.contains(AlertState.warn))
			AlertState.warn
		else
			AlertState.normal
	}

	private def getSubscribers(alert: Alert) : List[User] = {
		val alertTags = AlertTagModel.findTagsForAlert(alert.id).map(_.tag)		
		val alertIds = 	if(alert.dynamicAlertId.isDefined) { List(alert.id, alert.dynamicAlertId.get) } else { List(alert.id) }

		(TagSubscriptionModel.getSubscriptionsByTag(alertTags).filter(_.subscription.active).map(_.userOption).filter(_.isDefined).map(_.get)) ++ 
			(SubscriptionModel.getSubscriptionsByAlerts(alertIds).filter(_.subscription.active).map(_.userOption).filter(_.isDefined).map(_.get))
	}

	private def crossesThreshold(value: BigDecimal, threshold:BigDecimal, comparison: Comparisons.Value): Boolean = {
		comparison match {
			case Comparisons.< 	=> value < threshold
			case Comparisons.<=	=> value <= threshold
			case Comparisons.==	=> value == threshold
			case Comparisons.>	=> value > threshold
			case Comparisons.>=	=> value >= threshold
			case Comparisons.!=	=> value != threshold
		}
	}

	private def secondsFromNow(seconds: Int) : Date = {
		new Date(new Date().getTime + (1000 * seconds))
	}

	private def getState(alert:Alert, lastDataPoint:BigDecimal) : AlertState.Value = {
		if(crossesThreshold(lastDataPoint, alert.errorThreshold, alert.comparison)) {
			AlertState.error
		} else if (crossesThreshold(lastDataPoint, alert.warnThreshold, alert.comparison)) {
			AlertState.warn
		} else {
			AlertState.normal
		}
	}
}