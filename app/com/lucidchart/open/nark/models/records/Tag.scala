package com.lucidchart.open.nark.models.records

import java.util.UUID

trait HasId {
	def id: UUID
}

case class Tag(
	recordId: UUID,
	tag: String
) extends AppRecord

case class TagMap[T](
	contents: Map[String, List[T]]
) extends AppRecord
