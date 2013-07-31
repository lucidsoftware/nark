package com.lucidchart.open.nark.openid

import java.util.Date
import java.util.UUID

import org.openid4java.server.NonceGenerator
import org.openid4java.util.InternetDateFormat

class CustomNonceGenerator extends NonceGenerator {
	protected val dateFormat = new InternetDateFormat()
	protected def currentTimestamp = dateFormat.format(new Date())
	
	/**
	 * Generate the next nonce
	 */
	def next = currentTimestamp + UUID.randomUUID().toString
}
