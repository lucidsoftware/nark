package com.lucidchart.open.nark.models.records

import com.lucidchart.open.nark.models.records.Alert._
import java.util.{Date,UUID}

case class AlertHistory(
	alertId: UUID,
	target: String,
	date: Date,
	state: AlertState.Value,
	messagesSent: Int
) extends AppRecord {
	/**
	 * Create a new AlertHistory record for inserting into the database
	 */
	def this(alertId: UUID, target: String, state: AlertState.Value, messagesSent:Int) = this(alertId, target, new Date(), state, messagesSent)
}
