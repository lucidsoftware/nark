package com.lucidchart.open.nark.models.records

import java.util.UUID
import java.util.Date

case class User(
	id: UUID,
	email: String,
	created: Date,
	name: String,
	warnAddress: String,
	warnEnable: Boolean,
	errorAddress: String,
	errorEnable: Boolean
) extends AppRecord {
	/**
	 * Create a new User record for inserting into the database
	 */
	def this(email: String, name: String) = this(UUID.randomUUID(), email, new Date(), name, email, true, email, true)

	/**
	 * Create a new User record for inserting into the database
	 */
	def this(email: String, name: String, warnAddress: String, warnEnable: Boolean, errorAddress: String, errorEnable: Boolean) = this(UUID.randomUUID(), email, new Date(), name, warnAddress, warnEnable, errorAddress, errorEnable)
}
