package com.lucidchart.open.nark.models.records

import java.util.UUID

case class Target(
	id: UUID,
	graphId: UUID,
	name: String,
	target: String,
	summarizer: TargetSummarizer.Value,
	deleted: Boolean
) extends AppRecord {
	/**
	 * Create a new Target record for inserting into the database
	 */
	def this(graphId: UUID, name: String, target: String, summarizer: TargetSummarizer.Value) = this(UUID.randomUUID(), graphId, name, target, summarizer, false)
}

object TargetSummarizer extends Enumeration {
	val average = Value(0, "avg")
	val sum     = Value(1, "sum")
	val max     = Value(2, "max")
	val min     = Value(3, "min")
	val last    = Value(4, "last")
}
