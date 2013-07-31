import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

	val appName         = "Nark"
	val appVersion      = "1.0-SNAPSHOT"

	val appDependencies = Seq(
		jdbc,
		anorm,
		filters,
		"org.scala-lang" % "scala-actors" % "2.10.1",
		"mysql" % "mysql-connector-java" % "5.1.23",
		"commons-io" % "commons-io" % "2.4",
		"org.apache.commons" % "commons-email" % "1.3",
		"org.openid4java" % "openid4java" % "0.9.7"
	)


	val main = play.Project(appName, appVersion, appDependencies).settings(
		scalaVersion := "2.10.1"
	)

}
