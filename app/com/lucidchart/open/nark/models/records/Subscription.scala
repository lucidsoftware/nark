package com.lucidchart.open.nark.models.records

import java.util.UUID

case class Subscription (
	userId: UUID,
	alertId: UUID,
	alertType: AlertType.Value,
	active: Boolean
) extends AppRecord {
	/**
	 * Create a new Subscription to insert into the database
	 */
	def this(userId: UUID, alertId: UUID, alertType: AlertType.Value) = this(userId, alertId, alertType, true)
}
