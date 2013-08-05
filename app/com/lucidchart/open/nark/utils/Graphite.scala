package com.lucidchart.open.nark.utils

import play.api.Play.current
import play.api.Play.configuration
import java.util.Date
import java.net.URI
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.AutoRetryHttpClient
import org.apache.http.client.methods.HttpGet
import scala.io.Source
import play.api.libs.json._
import java.text.SimpleDateFormat

case class GraphiteData(targets: List[GraphiteTarget]) {
	def filterEmptyTargets() = copy(
		targets = targets.filter { target =>
			target.datapoints.exists(_.value.isDefined)
		}
	)
}

case class GraphiteTarget(target: String, datapoints: List[GraphiteDataPoint])
case class GraphiteDataPoint(date: Date, value: Option[BigDecimal])

class Graphite(protocol: String, host: String, port: Int) {
	protected val dateFormatter = new SimpleDateFormat("kk:mm_yyyyMMdd")

	protected def basicUriBuilder() = {
		val builder = new URIBuilder()
		builder.setScheme(protocol)
		builder.setHost(host)
		builder.setPort(port)
		builder.setPath("/render/")
		builder.setParameter("format", "json")
	}

	protected def addTargets(builder: URIBuilder, targets: List[String]) {
		targets.foreach { target =>
			builder.setParameter("target", target)
		}
	}

	protected def execute(builder: URIBuilder): JsValue = {
		execute(builder.build())
	}

	protected def execute(uri: URI): JsValue = {
		val client = new AutoRetryHttpClient()
		val request = new HttpGet(uri)
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
	protected def jsonToData(dataJson: JsValue, removeLastNull: Boolean) = {
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

	/**
	 * Get the graphite data for the target over the last x number of seconds.
	 */
	def data(target: String, seconds: Int): GraphiteData = data(List(target), seconds)

	/**
	 * Get the graphite data for the targets over the last x number of seconds.
	 */
	def data(targets: List[String], seconds: Int): GraphiteData = {
		val builder = basicUriBuilder()
		builder.setParameter("from", "-" + seconds.toString + "seconds")
		addTargets(builder, targets)
		jsonToData(execute(builder), true)
	}

	/**
	 * Get the graphite data for the target during a time period
	 */
	def data(target: String, from: Date, to: Date): GraphiteData = data(List(target), from, to)

	/**
	 * Get the graphite data for the targets during a time period
	 */
	def data(targets: List[String], from: Date, to: Date): GraphiteData = {
		val builder = basicUriBuilder()
		builder.setParameter("from", dateFormatter.format(from))
		builder.setParameter("until", dateFormatter.format(to))
		addTargets(builder, targets)
		jsonToData(execute(builder), false)
	}
}

object Graphite extends Graphite(
	configuration.getString("graphite.proto").get,
	configuration.getString("graphite.host").get,
	configuration.getInt("graphite.port").get
)
