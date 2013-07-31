package com.lucidchart.open.nark.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

object UUIDHelper {
	/**
	 * BIG ENDIAN is easier to read in mysql.
	 */
	private val ENDIANNESS = ByteOrder.BIG_ENDIAN
	
	/**
	 * Deterministically convert a byte array into a UUID.
	 * 
	 * @param value byte array to convert
	 * @return corresponding UUID
	 */
	def fromByteArray(value: Array[Byte]) = {
		require(value.length == 16)
		
		val buffer = ByteBuffer.wrap(value)
		buffer.order(ENDIANNESS)
		val high = buffer.getLong
		val low = buffer.getLong
		new UUID(high, low)
	}
	
	/**
	 * Deterministically convert a UUID into a byte array
	 * 
	 * @param value UUID to convert
	 * @return corresponding byte array
	 */
	def toByteArray(value: UUID) = {
		val buffer = ByteBuffer.wrap(new Array[Byte](16))
		buffer.order(ENDIANNESS)
		buffer.putLong(value.getMostSignificantBits)
		buffer.putLong(value.getLeastSignificantBits)
		buffer.array
	}
}
