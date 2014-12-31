import sbt._
import Keys._
import java.io.PrintWriter
import java.io.File
import play.Play.autoImport._
import play.PlayScala
import PlayKeys._

object ApplicationBuild extends Build {

	val appName         = "Nark"
	val appVersion      = "0.0.7." + "git rev-parse --short HEAD".!!.trim + ".SNAPSHOT"

	val appDependencies = Seq(
		jdbc,
		filters,
		"play" %% "anorm" % "2.1.5",
		"org.scala-lang" % "scala-actors" % "2.10.4",
		"mysql" % "mysql-connector-java" % "5.1.34",
		"commons-io" % "commons-io" % "2.4",
		"org.apache.commons" % "commons-email" % "1.3.3",
		"org.openid4java" % "openid4java" % "0.9.8",
		"org.apache.httpcomponents" % "httpclient" % "4.3.6",
		"com.lucidchart" %% "nark-plugin" % "1.1",
		"com.lucidchart" %% "nark-pagerduty-plugin" % "1.0"
	)

	def writeToFile(fileName: String, value: String) = {
		val file = new PrintWriter(new File(fileName))
		try { file.print(value) } finally { file.close() }
	}

	writeToFile("conf/app_version.txt", appVersion)
	writeToFile("conf/app_compilation_date.txt", System.currentTimeMillis.toString)

	val main = Project(appName, file(".")).enablePlugins(PlayScala).settings(
		libraryDependencies ++= appDependencies,
		resolvers ++= List(
			"Staging Sonatype repository" at "https://oss.sonatype.org/content/groups/staging/"
		),
		scalaVersion := "2.10.4",
		version := appVersion
	)

}
