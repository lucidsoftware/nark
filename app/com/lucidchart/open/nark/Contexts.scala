package com.lucidchart.open.nark

import play.api.libs.concurrent.Akka
import play.api.Play.current

object Contexts {
	implicit val graphite = Akka.system.dispatchers.lookup("akka.actor.graphite")
}
