package com.lucidchart.open.nark.plugins

import java.io.File
import java.net.URLClassLoader
import play.api.Play.current
import play.api.Play.configuration
import scala.collection.JavaConverters._

object PluginManager {

	val alertPlugins = loadAlertPlugins()

	/**
	 * Load all the alert plugins located in the /plugins directory. All plugins should extend
	 * the [[com.lucidchart.open.nark.plugins.AlertPlugin AlertPlugin]] trait.
	 * @return the loaded AlertPlugins
	 */
	def loadAlertPlugins(): List[AlertPlugin] = {

		val pluginNames = configuration.getStringList("plugins.names")
		pluginNames.map(_.asScala).getOrElse(Nil).foldLeft(List[AlertPlugin]()) { (plugins, name) =>
			var className = configuration.getString("plugins." + name + ".class").get
			val klass = Class.forName(className)
			val instance = klass.newInstance.asInstanceOf[AlertPlugin]
			instance.init()
			plugins :+ instance
		}
	}

}