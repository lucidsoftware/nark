package com.lucidchart.open.nark.models

import java.sql.Blob
import java.sql.Clob
import java.util.UUID

import org.apache.commons.io.IOUtils

import com.lucidchart.open.nark.utils.UUIDHelper

import anorm._

object AnormImplicits {
	/**
	 * Attempt to convert a SQL value into a byte array
	 * 
	 * @param value value to convert
	 * @return byte array
	 */
	private def valueToByteArrayOption(value: Any): Option[Array[Byte]] = {
		try {
			value match {
				case bytes: Array[Byte] => Some(bytes)
				case clob: Clob => Some(IOUtils.toByteArray(clob.getAsciiStream()))
				case blob: Blob => Some(blob.getBytes(1, blob.length.asInstanceOf[Int]))
				case _ => None
			}
		}
		catch {
			case e: Exception => None
		}
	}
	
	/**
	 * Attempt to convert a SQL value into a UUID
	 * 
	 * @param value value to convert
	 * @return UUID
	 */
	private def valueToUUIDOption(value: Any): Option[UUID] = {
		try {
			valueToByteArrayOption(value) match {
				case Some(bytes) => Some(UUIDHelper.fromByteArray(bytes))
				case _ => None
			}
		}
		catch {
			case e: Exception => None
		}
	}
	
	/**
	 * Implicit conversion from anorm row to byte array
	 */
	implicit def rowToByteArray: Column[Array[Byte]] = {
		Column.nonNull[Array[Byte]] { (value, meta) =>
			val MetaDataItem(qualified, nullable, clazz) = meta
			valueToByteArrayOption(value) match {
				case Some(bytes) => Right(bytes)
				case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Byte Array for column " + qualified))
			}
		}
	}
	
	/**
	 * Implicit converstion from anorm row to uuid
	 */
	implicit def rowToUUID: Column[UUID] = {
		Column.nonNull[UUID] { (value, meta) =>
			val MetaDataItem(qualified, nullable, clazz) = meta
			valueToUUIDOption(value) match {
				case Some(uuid) => Right(uuid)
				case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to UUID for column " + qualified))
			}
		}
	}
	
	/**
	 * Implicit conversion from UUID to anorm statement value
	 */
	implicit def uuidToStatement = new ToStatement[UUID] {
		def set(s: java.sql.PreparedStatement, index: Int, aValue: UUID): Unit = s.setObject(index, UUIDHelper.toByteArray(aValue))
	}
	
	class RichSQL(val query: String, val parameterValues: (Any, ParameterValue[Any])*) {
		/**
		 * Convert this object into an anorm.SqlQuery
		 */
		def toSQL = SQL(query).on(parameterValues: _*)
		
		/**
		 * Similar to anorm.SimpleSql.on, but takes lists instead of single values.
		 * Each list is converted into a set of values, and then passed to anorm's
		 * on function when toSQL is called.
		 */
		def onList[A](args: (String, Iterable[A])*)(implicit toParameterValue: (A) => ParameterValue[A]) = {
			val condensed = args.map { case (name, values) =>
				val search = "{" + name + "}"
				val valueNames = values.zipWithIndex.map { case (value, index) => name + "_" + index }
				val placeholders = valueNames.map { name => "{" + name + "}" }
				val replace = placeholders.mkString(",")
				val converted = values.map { value => toParameterValue(value).asInstanceOf[ParameterValue[Any]] }
				val parameters = valueNames.zip(converted)
				
				(search, replace, parameters)
			}
			
			val newQuery = condensed.foldLeft(query) { case (newQuery, (search, replace, _)) =>
				newQuery.replace(search, replace)
			}
			
			val newValues = parameterValues ++ condensed.map { case (_, _, parameters) => parameters }.flatten
			
			new RichSQL(newQuery, newValues: _*)
		}


		
		/**
		 * Helper for inserting multiple elements at the same time.
		 *
		 * Example:
		 *
		 * RichSQL("""
		 *     insert into mytable (a,b,c) values ({fields})
		 * """).multiInsert(2, Seq("a", "b", "c"), "fields")(
		 *     "a" -> List(5, 6),
		 *     "b" -> List(new Date(), new Date()),
		 *     "c" -> records.map(_.toString)
		 * )
		 *
		 * @param count # of records being inserted
		 * @param fields Field names, in order, to insert
		 * @param searchName name of the replacement variable
		 * @param args
		 */
		def multiInsert(count: Int, fields: Seq[String], searchName: String = "fields")(args: (String, Seq[ParameterValue[_]])*) = {
			require(count > 0)
			require(!fields.isEmpty)
			require(searchName.length() > 0)

			val search = "{" + searchName + "}"
			val expandedFields = (for (i <- 0 until count) yield {
				"{" + fields.map(_ + i).mkString("}, {") + "}"
			}).mkString("), (")
			val newQuery = query.replace(search, expandedFields)

			val newValues = parameterValues ++ args.map { case (argName, argValues) =>
				require(argValues.size == count)

				(for (i <- 0 until count) yield {
					(argName + i, argValues(i).asInstanceOf[ParameterValue[Any]])
				})
			}.flatten

			new RichSQL(newQuery, newValues: _*)
		}
	}
	
	object RichSQL {
		def apply[A](query: String) = new RichSQL(query)
	}
}
