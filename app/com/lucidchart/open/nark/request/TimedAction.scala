package com.lucidchart.open.nark.request

import scala.concurrent.ExecutionContext.Implicits.global

import com.lucidchart.open.nark.utils.StatsD

import play.api.mvc._

/**
 * Provides helpers for creating `TimedAction` values.
 */
trait TimedActionBuilder {
	/**
	 * Constructs an `TimedAction`.
	 *
	 * @param key for stats
	 * @param sampleRate 1.0 to sample all
	 * @param block the action code
	 */
	def apply(key: String, sampleRate: Double = 1.0)(block: EssentialAction): EssentialAction = new EssentialAction {
		val start = System.currentTimeMillis()
		
		protected def record(requestHeader: RequestHeader) {
			val end = System.currentTimeMillis()
			StatsD.timing("action." + key, (end - start).toInt, sampleRate)
		}
		
		def apply(requestHeader: RequestHeader) = {
			block(requestHeader).map { result =>
				result match {
					case plain: PlainResult => {
						record(requestHeader)
						plain
					}
					case async: AsyncResult => async.transform { result =>
						record(requestHeader)
						result
					}
				}
			}
		}
	}
}

/**
 * Helper object to create `TimedAction` values.
 */
object TimedAction extends TimedActionBuilder
