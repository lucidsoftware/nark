package com.lucidchart.open.nark.models.records

import java.util.UUID

case class Target(
	id: UUID,
	graphId: UUID,
	target: String,
	userId: UUID,
	deleted: Boolean
) extends AppRecord {
	def this(graphId: UUID, target: String, userId: UUID, deleted: Boolean) =
		this(UUID.randomUUID(), graphId, target, userId, deleted)
}