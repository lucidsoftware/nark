package com.lucidchart.open.nark.models.records

import java.util.UUID
import java.util.Date
import com.lucidchart.open.nark.models.GraphModel

case class Dashboard(
	id: UUID,
	name: String,
	url: String,
	created: Date,
	userId: UUID,
	deleted: Boolean
) extends AppRecord {

	def this(name: String, url: String, userId: UUID, deleted: Boolean) = this(UUID.randomUUID(), name, url, new Date(), userId, deleted)
	lazy val graphs = GraphModel.findGraphsByDashboardId(id)
}