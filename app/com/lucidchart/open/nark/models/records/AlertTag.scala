package com.lucidchart.open.nark.models.records

import java.util.UUID

case class AlertTag (
	alertId: UUID,
	tag: String
) extends AppRecord