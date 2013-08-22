package com.lucidchart.open.nark.models.records

import java.util.UUID
import java.util.Date

case class Dashboard(
	id: UUID,
	name: String,
	url: String,
	created: Date,
	userId: UUID,
	deleted: Boolean
) extends AppRecord {
	/**
	 * Create a new Dashboard record for inserting into the database
	 */
	def this(name: String, url: String, userId: UUID) = this(UUID.randomUUID(), name, url, new Date(), userId, false)
}
