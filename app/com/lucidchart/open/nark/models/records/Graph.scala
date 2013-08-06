package com.lucidchart.open.nark.models.records

import java.util.UUID
import com.lucidchart.open.nark.models.{TargetModel, DashboardModel}

case class Graph(
	id: UUID,
	name: String,
	dashboardId: UUID,
	sort: Int,
	typeGraph: GraphType.Value,
	deleted: Boolean
) extends AppRecord {

	def this(name: String, dashboardId: UUID, sort: Int, typeGraph: GraphType.Value, deleted: Boolean) =
		this(UUID.randomUUID(), name, dashboardId, sort, typeGraph, deleted)

	lazy val dashboard = DashboardModel.findDashboardByID(dashboardId).get
	lazy val userId = dashboard.userId
	lazy val targets = TargetModel.findTargetByGraphId(id)
}

object GraphType extends Enumeration {
	val normal  = Value(0, "Normal")
	val stacked = Value(1, "Stacked")
}
