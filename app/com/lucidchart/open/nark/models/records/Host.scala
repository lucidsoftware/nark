package com.lucidchart.open.nark.models.records

import java.util.UUID
import java.util.Date

case class Host(
	name: String,
	state: HostState.Value,
	lastConfirmed: Date
) extends AppRecord

object HostState extends Enumeration {
	val up    = Value(0)
	val down  = Value(1)
	val limbo = Value(2)
}
