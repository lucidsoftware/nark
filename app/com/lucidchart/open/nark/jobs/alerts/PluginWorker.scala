package com.lucidchart.open.nark.jobs.alerts

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinRouter
import com.lucidchart.open.nark.plugins.{AlertEvent, AlertPlugin}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.Play.configuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class PluginRequest(
  plugin: AlertPlugin,
  event: AlertEvent
)

object PluginMaster {
  private val numWorkers = configuration.getInt("plugins.workers.count").get
  private val master = Akka.system.actorOf(Props[PluginWorker].withRouter(RoundRobinRouter(PluginMaster.numWorkers)), name = "pluginWorkRouter")

  def send(message: PluginRequest) {
    master ! message
  }
}

class PluginWorker extends Actor with Mailer {

  def receive = {
    case request: PluginRequest => doPlugin(request)
  }

  def doPlugin(alert: PluginRequest): Unit = {
    val plugin = alert.plugin
    val event = alert.event
    val notificationString = plugin.name + " notification failed: " + "[" + event.current.toString +
      "] " + event.name + ":" + event.lastValue

    if (!plugin.alert(event)) {
      sendEmails(
        plugin.fallbackEmails,
        notificationString,
        notificationString,
        notificationString
      )
    }
  }

}