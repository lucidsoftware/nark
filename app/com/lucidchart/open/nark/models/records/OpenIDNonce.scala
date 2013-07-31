package com.lucidchart.open.nark.models.records

import java.util.Date

case class OpenIDNonce(
	provider: String,
	nonce: String,
	created: Date
) {
	/**
	 * Used for creating/inserting a brand new nonce using
	 * the OpenIDModel
	 */
	def this(provider: String, nonce: String) = this(
		provider,
		nonce,
		new Date()
	)
}
