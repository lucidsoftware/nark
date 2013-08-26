package com.lucidchart.open.nark.models.records

import com.lucidchart.open.nark.models.records.Alert._
import java.util.{Date,UUID}

case class AlertTargetState(
	alertId: UUID,
	target: String,
	state: AlertState.Value,
	lastUpdated: Date
) extends AppRecord {
	/**
	 * Create a new AlertHistory record for inserting into the database
	 */
	def this(alertId: UUID, target: String, state: AlertState.Value) = this(alertId, target, state, new Date())
}
