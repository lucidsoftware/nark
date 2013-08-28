package com.lucidchart.open.nark.models.records

import java.util.{Date, UUID}
import scala.math.BigDecimal

case class DynamicAlert (
	id: UUID,
	name: String,
	userId: UUID,
	searchTarget: String,
	matchExpr: String,
	buildTarget: String,
	comparison: Comparisons.Value,
	active: Boolean,
	deleted: Boolean,
	created: Date,
	frequency: Int,
	warnThreshold: BigDecimal,
	errorThreshold: BigDecimal
) extends AppRecord with HasId {
	
	/**
	 * Create a new DynamicAlert record for inserting into the database
	 */
	def this(name: String, userId: UUID, searchTarget: String, matchExpr: String, buildTarget: String, comparison: Comparisons.Value, frequency: Int, warnThreshold: BigDecimal, errorThreshold: BigDecimal) = this(UUID.randomUUID(), name, userId, searchTarget, matchExpr, buildTarget, comparison, true, false, new Date(), frequency, warnThreshold, errorThreshold)
}