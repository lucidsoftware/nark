package com.lucidchart.open.nark.models.records

import java.util.{Date, UUID}

object Comparisons extends Enumeration {
	val < = Value(0, "<")
	val <= = Value(1, "<=")
	val == = Value("==")
	val >= = Value(">=")
	val > = Value(">")
}

object AlertState extends Enumeration {
	val normal = Value(0, "normal")
	val error = Value("error")
	val warn = Value("warn")
}

case class Alert (
	id: UUID,
	name: String,
	userId: UUID,
	target: String,
	comparison: Comparisons.Value,
	active: Boolean,
	deleted: Boolean,
	created: Date,
	threadId: Option[UUID],
	lastChecked: Date,
	nextCheck: Date,
	frequency: Int,
	warnThreshold: Double,
	errorThreshold: Double,
	state: AlertState.Value
) extends AppRecord {
	/**
	 * Create a new Alert record for inserting into the database
	*/
	def this(name: String, userId: UUID, target: String, comparison: Comparisons.Value, frequency: Int, warnThreshold: Double, errorThreshold: Double) = this(UUID.randomUUID(), name, userId, target, comparison, true, false, new Date(), None, new Date(), new Date(), frequency, warnThreshold, errorThreshold, AlertState.normal)
}