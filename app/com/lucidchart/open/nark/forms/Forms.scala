package com.lucidchart.open.nark.forms

import java.util.TimeZone
import java.util.UUID

import play.api.data.FormError
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.format.Formats._
import play.api.data.format.Formatter

object Forms {
	val uuid: Mapping[UUID] = of[UUID]
	
	val rfc8601Date = date("yyyy-MM-dd'T'HH:mm:ssZ", TimeZone.getTimeZone("GMT"))
	val internetDate = rfc8601Date

	/**
	 * Exact copy of private function in play.api.data.format.Formats
	 */
	private def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Any])(key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
		stringFormat.bind(key, data).right.flatMap { s =>
			scala.util.control.Exception.allCatch[T]
				.either(parse(s))
				.left.map(e => Seq(FormError(key, errMsg, errArgs)))
		}
	}

	/**
	 * Default formatter for the `UUID` type.
	 */
	implicit def uuidFormat: Formatter[UUID] = new Formatter[UUID] {
		override val format = Some(("format.uuid", Nil))
		def bind(key: String, data: Map[String, String]) = parsing(v => UUID.fromString(v), "error.uuid", Nil)(key, data)
		def unbind(key: String, value: UUID) = Map(key -> value.toString)
	}
}
