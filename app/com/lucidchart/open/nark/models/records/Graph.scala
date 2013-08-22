package com.lucidchart.open.nark.models.records

import java.util.UUID

case class Graph(
	id: UUID,
	name: String,
	dashboardId: UUID,
	sort: Int,
	typeGraph: GraphType.Value,
	axisLabel: GraphAxisLabel.Value,
	deleted: Boolean
) extends AppRecord {
	/**
	 * Create a new Dashboard record for inserting into the database
	 */
	def this(name: String, dashboardId: UUID, sort: Int, typeGraph: GraphType.Value, axisLabel: GraphAxisLabel.Value) = this(UUID.randomUUID(), name, dashboardId, sort, typeGraph, axisLabel, false)
}

object GraphType extends Enumeration {
	val normal  = Value(0, "Normal")
	val stacked = Value(1, "Stacked")
}

object GraphAxisLabel extends Enumeration {
	val auto       = Value(0, "Auto")
	val full       = Value(1, "Full")
	val powerOf2   = Value(2, "Power of 2")
	val powerOf10  = Value(3, "Power of 10")
}
