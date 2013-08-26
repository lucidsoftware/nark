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
	protected val usersRowParser = {
		get[UUID]("id") ~
		get[String]("email") ~
		get[Date]("created") ~
		get[String]("name") ~
		get[String]("warn_address") ~
		get[Boolean]("warn_enable") ~
		get[String]("error_address") ~
		get[Boolean]("error_enable") map {
			case id ~ email ~ created ~ name ~ warn_address ~ warn_enable ~ error_address ~ error_enable=>
				new User(id, email, created, name, warn_address, warn_enable, error_address, error_enable)
		}
	}
	
	/**
	 * Find the user that has the matching id
	 * 
	 * @param id
	 * @return user
	 */
	def findUserByID(id: UUID): Option[User] = findUsersByID(List(id)).headOption

	/**
	 * Find all users within the set of user ids
	 * @param userIds the ids of the users to find
	 */
	def findUsersByID(userIds: List[UUID]): List[User] = {
		DB.withConnection("main") { connection =>
			RichSQL("""
				SELECT *
				FROM `users`
				WHERE `id` IN ({user_ids})
			""").onList(
				"user_ids" -> userIds
			).toSQL.as(usersRowParser *)(connection)
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
				SELECT *
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
				INSERT INTO `users` (`id`, `email`, `created`, `name`, `warn_address`, `warn_enable`, `error_address`, `error_enable`)
				VALUES ({id}, {email}, {created}, {name}, {warn_address}, {warn_enable}, {error_address}, {error_enable})
			""").on(
				"id"         -> user.id,
				"email"      -> user.email,
				"created"    -> user.created,
				"name"       -> user.name,
				"warn_address"	-> user.warnAddress,
				"warn_enable"	-> user.warnEnable,
				"error_address"	-> user.errorAddress,
				"error_enable"	-> user.errorEnable
			).executeUpdate()(connection)
		}
	}
}

object UserModel extends UserModel
