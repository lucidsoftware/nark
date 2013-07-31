package com.lucidchart.open.nark.models.records

import java.util.Date

case class OpenIDAssociation(
	provider: String,
	handle: String,
	created: Date,
	expires: Date,
	associationType: OpenIDAssociationType.Value,
	secret: Array[Byte]
) {
	/**
	 * Used for creating/inserting a brand new association using
	 * the OpenIDModel
	 */
	def this(provider: String, handle: String, expires: Date, associationType: OpenIDAssociationType.Value, secret: Array[Byte]) = this(
		provider,
		handle,
		new Date(),
		expires,
		associationType,
		secret
	)
}

object OpenIDAssociationType extends Enumeration {
	val hmacSha1   = Value(0)
	val hmacSha256 = Value(1)
}
