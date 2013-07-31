package com.lucidchart.open.nark.models.records

import java.util.UUID
import java.util.Date

case class User(
	id: UUID,
	email: String,
	created: Date,
	name: String
) extends AppRecord {
	/**
	 * Create a new User record for inserting into the database
	 */
	def this(email: String, name: String) = this(UUID.randomUUID(), email, new Date(), name)
}
