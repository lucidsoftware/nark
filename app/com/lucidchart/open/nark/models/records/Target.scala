package com.lucidchart.open.nark.models.records

import java.util.UUID
import com.lucidchart.open.nark.models.{GraphModel, DashboardModel}

case class Target(
	id: UUID,
	graphId: UUID,
	target: String,
	deleted: Boolean
) extends AppRecord {

	def this(graphId: UUID, target: String, deleted: Boolean) =
		this(UUID.randomUUID(), graphId, target, deleted)

	lazy val graph = GraphModel.findGraphByID(graphId).get
	lazy val dashboard = DashboardModel.findDashboardByID(graph.dashboardId).get
	lazy val userId = dashboard.userId
}