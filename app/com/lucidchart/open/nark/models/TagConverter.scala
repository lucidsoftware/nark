package com.lucidchart.open.nark.models

import com.lucidchart.open.nark.models.records.{HasId, Tag, TagMap}

import java.util.UUID

object TagConverter {
	/**
	 * Combine a list of tags and a list of records into a map
	 * of tag to list of matching record pairs
	 */
	def toTagMap[T <: HasId](tags: List[Tag], records: List[T]): TagMap[T] = {
		val recordsById = records.map { r => (r.id, r) }.toMap
		TagMap[T](
			tags.map { tag =>
				(tag, recordsById.get(tag.recordId))
			}.collect {
				case (tag, recordOption) if (recordOption.isDefined) => (tag, recordOption.get)
			}.groupBy { case (tag, record) =>
				tag.tag
			}.map { case (tag, tuples) =>
				(tag, tuples.map(_._2))
			}.toMap
		)
	}

	/**
	 * Find all the tags for each record ID
	 */
	def toTagMap(tags: List[Tag]): Map[UUID, List[String]] = {
		tags.groupBy { tag =>
			tag.recordId
		}.map { case (recordId, tags) =>
			(recordId, tags.map(_.tag))
		}
	}
}