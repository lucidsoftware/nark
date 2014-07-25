import sbt._
import Keys._
import play.Project._
import java.io.PrintWriter
import java.io.File

object ApplicationBuild extends Build {

	val appName         = "Nark"
	val appVersion      = "0.0.7." + "git rev-parse --short HEAD".!!.trim + ".SNAPSHOT"

	val appDependencies = Seq(
		jdbc,
		anorm,
		filters,
		"org.scala-lang" % "scala-actors" % "2.10.1",
		"mysql" % "mysql-connector-java" % "5.1.23",
		"commons-io" % "commons-io" % "2.4",
		"org.apache.commons" % "commons-email" % "1.3",
		"org.openid4java" % "openid4java" % "0.9.7",
		"org.apache.httpcomponents" % "httpclient" % "4.2.5",
		"com.lucidchart" %% "nark-plugin" % "1.0",
		"com.lucidchart" %% "nark-pagerduty-plugin" % "1.0"
	)

	def writeToFile(fileName: String, value: String) = {
		val file = new PrintWriter(new File(fileName))
		try { file.print(value) } finally { file.close() }
	}

	writeToFile("conf/app_version.txt", appVersion)
	writeToFile("conf/app_compilation_date.txt", System.currentTimeMillis.toString)

	val main = play.Project(appName, appVersion, appDependencies).settings(
		scalaVersion := "2.10.1"
	)

}
