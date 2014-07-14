package com.lucidchart.open.nark.models

import java.util.Date
import java.util.UUID

import com.lucidchart.open.nark.models.records.User
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._

import play.api.Play.current
import play.api.db.DB

class UserModel extends AppModel {
	protected val usersRowParser = RowParser { row =>
		User(
			row.uuid("id"),
			row.string("email"),
			row.date("created"),
			row.string("name"),
			row.string("warn_address"),
			row.bool("warn_enable"),
			row.string("error_address"),
			row.bool("error_enable")
		)
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
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `users`
				WHERE `id` IN ({user_ids})
			""").expand { implicit query =>
				commaSeparated("user_ids", userIds.size)
			}.on { implicit query =>
				uuids("user_ids", userIds)
			}.asList(usersRowParser)
		}
	}
	
	/**
	 * Find the user that has the matching email address
	 * 
	 * @param email
	 * @return user
	 */
	def findUserByEmail(email: String): Option[User] = {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				SELECT *
				FROM `users`
				WHERE `email` = {email}
				LIMIT 1
			""").on { implicit query =>
				string("email", email)
			}.asSingleOption(usersRowParser)
		}
	}
	
	/**
	 * Create a new user using all the details from the user object.
	 * Throws an exception on failure
	 * 
	 * @param user
	 */
	def createUser(user: User) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				INSERT INTO `users` (`id`, `email`, `created`, `name`, `warn_address`, `warn_enable`, `error_address`, `error_enable`)
				VALUES ({id}, {email}, {created}, {name}, {warn_address}, {warn_enable}, {error_address}, {error_enable})
			""").on { implicit query =>
				uuid("id", user.id)
				string("email", user.email)
				date("created", user.created)
				string("name", user.name)
				string("warn_address", user.warnAddress)
				bool("warn_enable", user.warnEnable)
				string("error_address", user.errorAddress)
				bool("error_enable", user.errorEnable)
			}.executeUpdate()
		}
	}

	/**
	 * Edit a user using the details from the user object
	 * @param user the edited User to put into the database
	 */
	def editUser(user: User) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				UPDATE `users`
				SET `error_address`={error_address}, `error_enable`={error_enable}, `warn_address`={warn_address}, `warn_enable`={warn_enable}
				WHERE `id`={id}
			""").on { implicit query =>
				uuid("id", user.id)
				string("error_address", user.errorAddress)
				bool("error_enable", user.errorEnable)
				string("warn_address", user.warnAddress)
				bool("warn_enable", user.warnEnable)
			}.executeUpdate()
		}
	}
}

object UserModel extends UserModel
