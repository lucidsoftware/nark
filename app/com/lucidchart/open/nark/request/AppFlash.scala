package com.lucidchart.open.nark.request

import play.api.mvc.Flash

object AppFlash {
	protected def flash(level: String, message: String, title: String): Flash = Flash(Map("level" -> level, "message" -> message, "title" -> title))
	
	/**
	 * Create a new success flash message with no title
	 */
	def success(message: String): Flash = success(message, "")
	
	/**
	 * Create a new success flash message
	 */
	def success(message: String, title: String): Flash = flash("success", message, title)
	
	/**
	 * Create a new error flash message with no title
	 */
	def error(message: String): Flash = error(message, "")
	
	/**
	 * Create a new error flash message
	 */
	def error(message: String, title: String): Flash = flash("error", message, title)
	
	/**
	 * Create a new warning flash message with no title
	 */
	def warning(message: String): Flash = warning(message, "")
	
	/**
	 * Create a new warning message
	 */
	def warning(message: String, title: String): Flash = flash("warning", message, title)
	
	/**
	 * Create a new informational flash message with no title
	 */
	def info(message: String): Flash = info(message, "")
	
	/**
	 * Create a new informational flash message
	 */
	def info(message: String, title: String): Flash = flash("info", message, title)
}
