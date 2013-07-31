package com.lucidchart.open.nark.request

import java.util.UUID

import play.api.Play
import play.api.Play.current
import play.api.Mode
import play.api.libs.iteratee.Done
import play.api.mvc._
import play.api.mvc.Results._

/**
 * Provides helpers for creating `EnvironmentAction` values.
 */
trait EnvironmentActionBuilder {
	/**
	 * Make sure Play is running in the mode passed in.
	 * If not, return a 404. If so, run the block.
	 */
	def checkEnvironment(validMode: Mode.Value)(block: EssentialAction): EssentialAction = checkEnvironment(Set(validMode))(block)

	/**
	 * Make sure Play is running in one of the modes passed in.
	 * If not, return a 404. If so, run the block.
	 */
	def checkEnvironment(validModes: Set[Mode.Value])(block: => EssentialAction): EssentialAction = new EssentialAction {
		def apply(requestHeader: RequestHeader) = {
			if (!validModes.contains(Play.mode)) {
				Done(NotFound)
			}
			else {
				block(requestHeader)
			}
		}
	}

	/**
	 * Only execute the block if play is running in development mode
	 */
	def devOnly(block: => EssentialAction) = checkEnvironment(Mode.Dev)(block)

	/**
	 * Only execute the block if play is running in test mode
	 */
	def testOnly(block: => EssentialAction) = checkEnvironment(Mode.Test)(block)

	/**
	 * Only execute the block if play is running in production mode
	 */
	def prodOnly(block: => EssentialAction) = checkEnvironment(Mode.Prod)(block)
}

/**
 * Helper object to create `EnvironmentAction` values.
 */
object EnvironmentAction extends EnvironmentActionBuilder
