package com.lucidchart.open.nark.models

import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._

import play.api.Play.current
import play.api.db.DB

class MutexModelLockTakenException(name: String) extends Exception(name)

class MutexModel extends AppModel {
	/**
	 * Lock a mutex, and call the callback
	 *
	 * If the mutex cannot be locked within timeout, an exception will be thrown
	 */
	def lock[A](name: String, timeoutSeconds: Int)(callback: => A): A = {
		lock[Option[A]](name, timeoutSeconds, None) {
			Some(callback)
		} match {
			case Some(x) => x
			case None => throw new MutexModelLockTakenException(name)
		}
	}

	/**
	 * Lock a mutex, and call the callback
	 *
	 * If the mutex cannot be locked within timeout, noLockReturn will be returned
	 */
	def lock[A](name: String, timeoutSeconds: Int, noLockReturn: => A)(callback: => A): A = {
		DB.withConnection("main") { implicit connection =>
			val locked = SQL("""
				SELECT GET_LOCK({name}, {timeout})
			""").on { implicit query =>
				string("name", name)
				int("timeout", timeoutSeconds)
			}.asScalarOption[Long] match {
				case Some(x) if (x == 1) => true
				case _ => false
			}

			if (locked) {
				try {
					callback
				}
				finally {
					SQL("SELECT RELEASE_LOCK({lock})").on { implicit query => string("lock", name) }.asScalarOption[Long]
				}
			}
			else {
				noLockReturn
			}
		}
	}
}

object MutexModel extends MutexModel
