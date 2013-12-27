package com.lucidchart.open.nark.utils

import java.util.Date
import play.api.Play
import play.api.Play.current

object AppVersioning {
	lazy val compilationDate = new Date(io.Source.fromURL(Play.resource("app_compilation_date.txt").get).mkString.toLong)
	lazy val version = io.Source.fromURL(Play.resource("app_version.txt").get).mkString
	lazy val versionParts = version.split(".")

	def major = versionParts(0)
	def minor = versionParts(1)
	def build = versionParts(2)
	def revision = versionParts(3)
	def snapshot = versionParts.size > 4 && versionParts(4) == "SNAPSHOT"
}
