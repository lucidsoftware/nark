package com.lucidchart.open.nark.utils

import play.api.Logger
import play.api.Play.current
import play.api.Play.configuration
import java.util.Date
import java.net.URI
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.AutoRetryHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpRequestBase
import scala.io.Source
import play.api.libs.json._
import java.text.SimpleDateFormat
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.Await

case class GraphiteData(targets: List[GraphiteTarget]) {
	def filterEmptyTargets() = copy(
		targets = targets.filter { target =>
			target.datapoints.exists(_.value.isDefined)
		}
	)
}

case class GraphiteTarget(target: String, datapoints: List[GraphiteDataPoint])
case class GraphiteDataPoint(date: Date, value: Option[BigDecimal])

case class GraphiteMetricData(metrics: List[GraphiteMetricItem])
case class GraphiteMetricItem(name: String, path: String, leaf: Boolean)

class Graphite(protocol: String, host: String, port: Int) {
	protected def basicUriBuilder() = {
		val builder = new URIBuilder()
		builder.setScheme(protocol)
		builder.setHost(host)
		builder.setPort(port)
	}

	protected def addTargets(builder: URIBuilder, targets: List[String]) {
		targets.foreach { target =>
			builder.addParameter("target", target)
		}
	}

	protected def executeGet(uri: URI): JsValue = {
		val request = new HttpGet(uri)
		execute(request)
	}

	protected def execute(request: HttpRequestBase): JsValue = {
		Logger.info("Getting information from graphite at url: " + request.getURI())
		val client = new AutoRetryHttpClient()
		val response = client.execute(request)
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception("Could not retrieve the graphite url!")
		}
		else {
			val body = Source.fromInputStream(response.getEntity().getContent()).getLines().mkString("\n")
			Json.parse(body)
		}
	}

	/**
	 * Convert the JSON render data from graphite into a type-checked case class
	 *
	 * @param dataJson Must be a JsArray
	 * @param removeLastNull As graphite time boundaries switch, the last element in the datapoints
	 *                       becomes null. Setting removeLastNull to true fixes this problem until
	 *                       StatsD can report the next value for the statistic.
	 */
	protected def jsonToGraphiteData(dataJson: JsValue, removeLastNull: Boolean) = {
		GraphiteData(
			dataJson.asInstanceOf[JsArray].value.toList.map { e1 =>
				val targetJson = e1.asInstanceOf[JsObject]
				val datapoints = targetJson.value("datapoints").asInstanceOf[JsArray].value.toList.map { e2 =>
					val pointJson = e2.asInstanceOf[JsArray]
					GraphiteDataPoint(
						new Date(pointJson.value(1).asInstanceOf[JsNumber].value.intValue * 1000L),
						pointJson.value(0) match {
							case x: JsNumber => Some(x.value)
							case x => None
						}
					)
				}

				val lastIsNull = !datapoints.isEmpty && datapoints.last.value.isEmpty
				val filterLastNullDatapoints = if (lastIsNull && removeLastNull) {
					datapoints.take(datapoints.size - 1)
				}
				else {
					datapoints
				}

				GraphiteTarget(
					targetJson.value("target").asInstanceOf[JsString].value,
					filterLastNullDatapoints
				)
			}
		)
	}

	protected def jsonToGraphiteMetricData(dataJson: JsValue) = {
		GraphiteMetricData(
			dataJson.asInstanceOf[JsObject].value("metrics").asInstanceOf[JsArray].value.toList.map { e1 =>
				val e1Json = e1.asInstanceOf[JsObject]
				GraphiteMetricItem(
					e1Json.value("name").asInstanceOf[JsString].value,
					e1Json.value("path").asInstanceOf[JsString].value,
					e1Json.value("is_leaf").asInstanceOf[JsString].value == "1"
				)
			}
		)
	}

	def baseUrl() = {
		basicUriBuilder.build()
	}

	/**
	 * Get the graphite data for the target over the last x number of seconds.
	 */
	def data(target: String, seconds: Int)(implicit executionContext: ExecutionContext): Future[GraphiteData] = data(List(target), seconds)

	/**
	 * Get the graphite data for the targets over the last x number of seconds.
	 */
	def data(targets: List[String], seconds: Int)(implicit executionContext: ExecutionContext): Future[GraphiteData] = {
		val builder = basicUriBuilder()
		builder.setPath("/render/")
		builder.setParameter("format", "json")
		builder.setParameter("from", "-" + seconds.toString + "seconds")
		addTargets(builder, targets)
		Future {
			jsonToGraphiteData(executeGet(builder.build()), true)
		}
	}

	/**
	 * Get the graphite data for the target during a time period
	 */
	def data(target: String, from: Date, to: Date)(implicit executionContext: ExecutionContext): Future[GraphiteData] = data(List(target), from, to)

	/**
	 * Get the graphite data for the targets during a time period
	 */
	def data(targets: List[String], from: Date, to: Date)(implicit executionContext: ExecutionContext): Future[GraphiteData] = {
		val builder = basicUriBuilder()
		builder.setPath("/render/")
		builder.setParameter("format", "json")
		builder.setParameter("from", (from.getTime() / 1000).toString)
		builder.setParameter("until", (to.getTime() / 1000).toString)
		addTargets(builder, targets)
		Future {
			jsonToGraphiteData(executeGet(builder.build()), false)
		}
	}

	/**
	 * Synchronously get the graphite data for the targets over the last x number of seconds.
	 */
	def synchronousData(target: String, seconds: Int)(implicit executionContext: ExecutionContext): GraphiteData = {
		synchronousData(target, seconds, configuration.getLong("graphite.timeoutMS.data.server").get.millis)
	}

	/**
	 * Synchronously get the graphite data for the targets over the last x number of seconds.
	 */
	def synchronousData(target: String, seconds: Int, timeout: Duration)(implicit executionContext: ExecutionContext): GraphiteData = {
		Await.result(data(target, seconds), timeout)
	}

	/**
	 * Synchronously get the graphite data for the target during a time period
	 */
	def synchronousData(targets: List[String], from: Date, to: Date)(implicit executionContext: ExecutionContext): GraphiteData = {
		synchronousData(targets, from, to, configuration.getLong("graphite.timeoutMS.data.server").get.millis)
	}

	/**
	 * Synchronously get the graphite data for the target during a time period
	 */
	def synchronousData(targets: List[String], from: Date, to: Date, timeout: Duration)(implicit executionContext: ExecutionContext): GraphiteData = {
		Await.result(data(targets, from, to), timeout)
	}

	/**
	 * Find metrics in graphite
	 */
	def metrics(target: String)(implicit executionContext: ExecutionContext): Future[GraphiteMetricData] = {
		val builder = basicUriBuilder()
		builder.setPath("/metrics/find/")
		builder.setParameter("format", "completer")
		builder.setParameter("query", target + "*")
		Future {
			jsonToGraphiteMetricData(executeGet(builder.build))
		}
	}

	/**
	 * Synchronously find metrics in graphite
	 */
	def synchronousMetrics(target: String)(implicit executionContext: ExecutionContext): GraphiteMetricData = {
		synchronousMetrics(target, configuration.getLong("graphite.timeoutMS.metrics.server").get.millis)
	}

	/**
	 * Synchronously find metrics in graphite
	 */
	def synchronousMetrics(target: String, timeout: Duration)(implicit executionContext: ExecutionContext): GraphiteMetricData = {
		Await.result(metrics(target), timeout)
	}
}

object Graphite extends Graphite(
	configuration.getString("graphite.proto").get,
	configuration.getString("graphite.host").get,
	configuration.getInt("graphite.port").get
)
