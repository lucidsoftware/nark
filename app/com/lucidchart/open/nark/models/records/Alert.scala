package com.lucidchart.open.nark.models.records

import java.util.{Date, UUID}
import scala.math.BigDecimal

object Comparisons extends Enumeration {
	val < = Value(0, "<")
	val <= = Value(1, "<=")
	val == = Value(2, "==")
	val >= = Value(3, ">=")
	val > = Value(4, ">")
}

object AlertState extends Enumeration {
	val normal = Value(0, "normal")
	val error = Value(1, "error")
	val warn = Value(2, "warn")
}

object AlertType extends Enumeration {
	val alert = Value(0, "alert")
	val dynamicAlert = Value(1, "dynamic alert")
	val alertTag = Value(2, "alert tag")
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
	warnThreshold: BigDecimal,
	errorThreshold: BigDecimal,
	state: AlertState.Value
) extends AppRecord {
	/**
	 * Create a new Alert record for inserting into the database
	*/
	def this(name: String, userId: UUID, target: String, comparison: Comparisons.Value, frequency: Int, warnThreshold: BigDecimal, errorThreshold: BigDecimal) = this(UUID.randomUUID(), name, userId, target, comparison, true, false, new Date(), None, new Date(), new Date(), frequency, warnThreshold, errorThreshold, AlertState.normal)
}