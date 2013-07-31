package com.lucidchart.open.nark.utils

import java.security.MessageDigest

object HashHelper {
	private def hexHash(bytes: Array[Byte], digestName: String): String = {
		val messageDigest = java.security.MessageDigest.getInstance(digestName)
		hexHash(bytes, messageDigest)
	}
	
	private def hexHash(bytes: Array[Byte], messageDigest: MessageDigest): String = {
		messageDigest.digest(bytes).map { byte => "%02x".format(byte) }.foldLeft("")(_+_)
	}
	
	def md5(value: Array[Byte]): String = hexHash(value, "MD5")
	def md5(value: Any): String = md5(value.toString.getBytes)
	
	def sha1(value: Array[Byte]): String = hexHash(value, "SHA-1")
	def sha1(value: Any): String = sha1(value.toString.getBytes)
}
