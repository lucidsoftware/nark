package com.lucidchart.open.nark.models.records

import java.util.UUID

case class Target(
	id: UUID,
	graphId: UUID,
	name: String,
	target: String,
	deleted: Boolean
) extends AppRecord {
	/**
	 * Create a new Target record for inserting into the database
	 */
	def this(graphId: UUID, name: String, target: String, deleted: Boolean) = this(UUID.randomUUID(), graphId, name, target, deleted)
}
