package com.lucidchart.open.nark.models.records

case class Pagination[T <: AppRecord](
	page: Int,
	found: Long,
	limit: Int,
	matches: List[T]
)