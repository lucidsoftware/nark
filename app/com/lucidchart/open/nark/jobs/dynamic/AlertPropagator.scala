package com.lucidchart.open.nark.jobs.dynamic

import akka.actor.{Actor, Props}

import com.lucidchart.open.nark.models.{AlertModel, AlertTagModel, DynamicAlertModel, DynamicAlertTagModel, DynamicAlertTagSubscriptionModel, MutexModel, SubscriptionModel}
import com.lucidchart.open.nark.models.records.{Alert, AlertType, DynamicAlert, Subscription}
import com.lucidchart.open.nark.utils.{Graphite, GraphiteTarget}
import com.lucidchart.open.nark.utils.UUIDHelper


import java.util.Date

import play.api.libs.concurrent.Akka
import play.api.Logger
import play.api.Play
import play.api.Play.{configuration, current}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.matching.Regex

object AlertPropagator {
	def schedule() = {
		val frequency = configuration.getInt("dynamic.frequency").get
		val stateChangeTime = configuration.getInt("dynamic.statechangetime").get
		val instance = Akka.system.actorOf(Props(new AlertPropagator(stateChangeTime)), name = "AlertPropagator")
		Akka.system.scheduler.schedule(frequency.seconds, frequency.seconds, instance, "go")
	}
}

class AlertPropagator(stateChangeTime: Int) extends Actor {
	private var runOnce = false

	def receive = {
		case _ => {
			if (Play.isProd || (Play.isDev && !runOnce)) {
				// we do this in a mutex to avoid overloading graphite needlessly
				runOnce = MutexModel.lock("alertpropagation", 0, false) {
					propagateAlerts()
					true
				}
			}
		}
	}

	def propagateAlerts() = {
		Logger.info("Alert Propagation Starting")
		val dynamicAlerts = DynamicAlertModel.findActiveDynamicAlerts()
		dynamicAlerts.map { dynamicAlert =>
			val matchExpr = dynamicAlert.matchExpr.r
			val startedUpdating = new Date()

			getTargets(dynamicAlert.searchTarget).foreach { target =>
				val builtTarget = buildTarget(matchExpr, target.target, dynamicAlert.buildTarget, dynamicAlert.searchTarget)
			
				val up = !target.datapoints.exists(_.value.isEmpty)
				val down = !target.datapoints.exists(_.value.isDefined)
				val alertOption = AlertModel.findPropagatedAlert(dynamicAlert.id, builtTarget)
				
				if (up) {
					if (alertOption.isDefined) {
						updateAlert(dynamicAlert, alertOption.get)
						cleanSubscriptions(dynamicAlert)
						Logger.info("Updated: " + dynamicAlert.name + " (Propagated Alert) " + builtTarget)
					}
					else {
						createAlert(dynamicAlert, builtTarget)
						Logger.info("Created: " + dynamicAlert.name + " (Propagated Alert) " + builtTarget)
					}
				}
				else if (down) {
					if (alertOption.isDefined) {
						deleteAlert(dynamicAlert, alertOption.get, builtTarget)
						Logger.info("Deleted: " + alertOption.get.name + " " + builtTarget)
					}
					else {
						cleanSubscriptions(dynamicAlert)
					}
				}
				else {
					cleanSubscriptions(dynamicAlert)
				}
			}

			//clean out any alerts created with a different version of the searchTarget
			cleanUpBefore(dynamicAlert, startedUpdating)
		}
		Logger.info("Alert Propagation done!")
	}

	def getTargets(search: String): List[GraphiteTarget] = {
		Graphite.synchronousData(search, stateChangeTime).targets
	}

	def buildTarget(matchExpr: Regex, target: String, buildTarget: String, default: String): String = {
		val matched = matchExpr.findFirstMatchIn(target)
		if (matched.isDefined) {
			matched.get.subgroups.zipWithIndex.foldLeft(buildTarget) { case (builtTarget, (group, i)) =>
				builtTarget.replace("(" + (i + 1) + ")", group)
			}
		}
		else {
			default
		}
	}

	def updateAlert(dynamicAlert: DynamicAlert, alert: Alert): Unit = {
		AlertModel.editAlert(alert.copy(
			name = dynamicAlert.name + " (Propagated Alert)",
			comparison = dynamicAlert.comparison,
			updated = new Date(), 
			frequency = dynamicAlert.frequency,
			warnThreshold = dynamicAlert.warnThreshold,
			errorThreshold = dynamicAlert.errorThreshold
		))
	}

	def createAlert(dynamicAlert: DynamicAlert, builtTarget: String): Unit = {
		//create alert
		val alert = new Alert(
			dynamicAlert.name + " (Propagated Alert)", 
			dynamicAlert.userId,
			Some(dynamicAlert.id),
			builtTarget,
			dynamicAlert.comparison,
			dynamicAlert.frequency,
			dynamicAlert.warnThreshold,
			dynamicAlert.errorThreshold
		)
		AlertModel.createAlert(alert)

		//create subscriptions for that dynamic alert
		val subscriptions = SubscriptionModel.getSubscriptionsByAlert(dynamicAlert.id).map { subscription =>
			new Subscription(subscription.subscription.userId, alert.id, AlertType.alert)
		}

		//create subscriptions for the tags of that dynamic alert
		val tags = DynamicAlertTagModel.findTagsForAlert(dynamicAlert.id).map { tag =>
			tag.tag
		}
		val tagSubscriptions = DynamicAlertTagSubscriptionModel.getSubscriptionsByTag(tags).map { subscription =>
			new Subscription(subscription.subscription.userId, alert.id, AlertType.alert)
		}
		SubscriptionModel.createSubscriptions(subscriptions ++ tagSubscriptions)
	}

	def deleteAlert(dynamicAlert: DynamicAlert, alert: Alert, builtTarget: String): Unit = {
		AlertModel.deletePropagatedAlert(dynamicAlert.id, builtTarget)
		SubscriptionModel.deleteSubscriptionsByAlert(alert.id)
	}

	def cleanUpBefore(dynamicAlert: DynamicAlert, startedUpdating: Date): Unit = {
		val deletedIds = AlertModel.deletePropagatedAlerts(dynamicAlert.id, startedUpdating).map(_.id)
		SubscriptionModel.deleteSubscriptionsByAlert(deletedIds)
	}

	def cleanSubscriptions(dynamicAlert: DynamicAlert): Unit = {
		//get all subscriptions to alerts propagated by the dynamic alert
		val alertIds = AlertModel.findPropagatedAlerts(dynamicAlert.id).map(_.id)
		val subscribedUsers = SubscriptionModel.getSubscriptionsByAlerts(alertIds).map(_.subscription.userId).distinct

		//find all users subscribed to this dynamic alert in some way
		val dynamicUsers = SubscriptionModel.getSubscriptionsByAlert(dynamicAlert.id).filter(_.subscription.active).map(_.subscription.userId)
		val tags = DynamicAlertTagModel.findTagsForAlert(dynamicAlert.id).map(_.tag)
		val tagUsers = DynamicAlertTagSubscriptionModel.getSubscriptionsByTag(tags).filter(_.subscription.active).map(_.subscription.userId)
		val users = (dynamicUsers ++ tagUsers).distinct

		//create subscriptions for users subscribed to the dynamic alert
		val subscriptions = users.filter(!subscribedUsers.contains(_)).map { user =>
			alertIds.map { alert =>
				new Subscription(user, alert, AlertType.alert)
			}
		}.flatten
		SubscriptionModel.createSubscriptions(subscriptions)

		//delete subscriptions for all users not subscribed to the dynamic alert
		val usersWithUnwantedSubscriptions = subscribedUsers.filter(!users.contains(_))
		alertIds.foreach { alert =>
			SubscriptionModel.deleteSubscriptions(alert, usersWithUnwantedSubscriptions) 
		}
	}
}