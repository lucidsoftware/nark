package com.lucidchart.open.nark.models

import java.util.Date
import java.util.UUID

import com.lucidchart.open.nark.models.records.User

import AnormImplicits._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB

class UserModel extends AppModel {
	protected val usersSelectAllFields = "`id`, `email`, `created`, `name`"
	
	protected val usersRowParser = {
		get[UUID]("id") ~
		get[String]("email") ~
		get[Date]("created") ~
		get[String]("name") map {
			case id ~ email ~ created ~ name =>
				new User(id, email, created, name)
		}
	}
	
	/**
	 * Find the user that has the matching id
	 * 
	 * @param id
	 * @return user
	 */
	def findUserByID(id: UUID): Option[User] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + usersSelectAllFields + """
				FROM `users`
				WHERE `id` = {id}
				LIMIT 1
			""").on(
				"id" -> id
			).as(usersRowParser.singleOpt)(connection)
		}
	}
	
	/**
	 * Find the user that has the matching email address
	 * 
	 * @param email
	 * @return user
	 */
	def findUserByEmail(email: String): Option[User] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT """ + usersSelectAllFields + """
				FROM `users`
				WHERE `email` = {email}
				LIMIT 1
			""").on(
				"email" -> email
			).as(usersRowParser.singleOpt)(connection)
		}
	}
	
	/**
	 * Create a new user using all the details from the user object.
	 * Throws an exception on failure
	 * 
	 * @param user
	 */
	def createUser(user: User) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO `users` (`id`, `email`, `created`, `name`)
				VALUES ({id}, {email}, {created}, {name})
			""").on(
				"id"         -> user.id,
				"email"      -> user.email,
				"created"    -> user.created,
				"name"       -> user.name
			).executeUpdate()(connection)
		}
	}
}

object UserModel extends UserModel
