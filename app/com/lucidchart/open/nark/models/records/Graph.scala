package com.lucidchart.open.nark.models.records

import java.util.UUID

case class Graph(
	id: UUID,
	name: String,
	dashboardId: UUID,
	sort: Int,
	typeGraph: GraphType.Value,
	deleted: Boolean
) extends AppRecord {
	/**
	 * Create a new Dashboard record for inserting into the database
	 */
	def this(name: String, dashboardId: UUID, sort: Int, typeGraph: GraphType.Value, deleted: Boolean) = this(UUID.randomUUID(), name, dashboardId, sort, typeGraph, deleted)
}

object GraphType extends Enumeration {
	val normal  = Value(0, "Normal")
	val stacked = Value(1, "Stacked")
}
