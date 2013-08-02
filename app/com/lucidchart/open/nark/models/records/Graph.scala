package com.lucidchart.open.nark.models.records

import java.util.UUID
import java.util.Date
import com.lucidchart.open.nark.models.GraphTypes

case class Graph(
	id: UUID,
	name: String,
	dashboardId: UUID,
	sort: Int,
	typeGraph: GraphTypes.Value,
	userId: UUID,
	deleted: Boolean
) extends AppRecord {
	def this(name: String, dashboardId: UUID, sort: Int, typeGraph: GraphTypes.Value, userId: UUID, deleted: Boolean) =
		this(UUID.randomUUID(), name, dashboardId, sort, typeGraph, userId, deleted)
}