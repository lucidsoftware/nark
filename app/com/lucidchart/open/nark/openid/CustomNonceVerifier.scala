package com.lucidchart.open.nark.openid

import java.util.Date
import org.openid4java.consumer.NonceVerifier
import com.lucidchart.open.nark.models.OpenIDModel
import com.lucidchart.open.nark.models.records.OpenIDNonce
import play.api.Play.configuration
import play.api.Play.current
import org.openid4java.util.InternetDateFormat

class CustomNonceVerifier(protected var maxAgeSeconds: Int) extends NonceVerifier {
	def this() = this(configuration.getInt("openid.nonce.maxAgeSeconds").get)
	
	protected val dateFormat = new InternetDateFormat()
	protected val dateExtractor = """^(\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\dZ).*$""".r
	
	protected class InvalidTimestampException extends Exception
	protected class TimestampTooOldException extends Exception
	protected class NonceSeenException extends Exception
	
	protected def maxAgeDate = new Date(System.currentTimeMillis() - maxAgeSeconds * 1000L)
	
	/**
	 * Checks if a nonce was seen before.
	 *
	 * @return {@link NonceVerifier.OK} only if the nonce was not seen before.
	 */
	def seen(provider: String, nonce: String): Int = {
		try {
			val timestamp = try {
				val dateExtractor(timestamp) = nonce
				timestamp
			}
			catch {
				case e: Exception => throw new InvalidTimestampException
			}
			
			val date = dateFormat.parse(timestamp)
			if (date.before(maxAgeDate)) {
				throw new TimestampTooOldException
			}
			
			val nonceRecord = new OpenIDNonce(provider, nonce)
			val seen = OpenIDModel.seenNonce(nonceRecord)
			
			if (seen) {
				throw new NonceSeenException
			}
			
			NonceVerifier.OK
		}
		catch {
			case e: InvalidTimestampException => NonceVerifier.INVALID_TIMESTAMP
			case e: TimestampTooOldException => NonceVerifier.TOO_OLD
			case e: NonceSeenException => NonceVerifier.SEEN
		}
	}
	
	/**
	 * Cleanup the old nonces
	 */
	def cleanup: Int = {
		OpenIDModel.cleanNoncesBefore(maxAgeDate)
	}
	
	/**
	 * Returns the expiration timeout for nonces, in seconds.
	 */
	def getMaxAge: Int = maxAgeSeconds

	/**
	 * Sets the expiration timeout for nonces, in seconds.
	 */
	def setMaxAge(seconds: Int) {
		this synchronized {
			maxAgeSeconds = seconds
		}
	}
}
