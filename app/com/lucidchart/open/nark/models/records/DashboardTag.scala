package com.lucidchart.open.nark.models.records

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class DashboardTag (
	dashboardId: UUID,
	tag: String
) extends AppRecord { }
