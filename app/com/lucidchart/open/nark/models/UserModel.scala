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
		get[Boolean]("error_enable")~
		get[Boolean]("admin") map {
			case id ~ email ~ created ~ name ~ warn_address ~ warn_enable ~ error_address ~ error_enable ~ admin =>
				new User(id, email, created, name, warn_address, warn_enable, error_address, error_enable, admin)
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
				INSERT INTO `users` (`id`, `email`, `created`, `name`, `warn_address`, `warn_enable`, `error_address`, `error_enable`, `admin`)
				VALUES ({id}, {email}, {created}, {name}, {warn_address}, {warn_enable}, {error_address}, {error_enable}, {admin})
			""").on(
				"id"         -> user.id,
				"email"      -> user.email,
				"created"    -> user.created,
				"name"       -> user.name,
				"warn_address"	-> user.warnAddress,
				"warn_enable"	-> user.warnEnable,
				"error_address"	-> user.errorAddress,
				"error_enable"	-> user.errorEnable,
				"admin" -> user.admin
			).executeUpdate()(connection)
		}
	}

	/**
	 * Edit a user using the details from the user object
	 * @param user the edited User to put into the database
	 */
	def editUser(user: User) {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE `users`
				SET `error_address`={error_address}, `error_enable`={error_enable}, `warn_address`={warn_address}, `warn_enable`={warn_enable}
				WHERE `id`={id}
			""").on(
				"id" -> user.id,
				"error_address" -> user.errorAddress,
				"error_enable" -> user.errorEnable,
				"warn_address" -> user.warnAddress,
				"warn_enable" -> user.warnEnable
			).executeUpdate()(connection)
		}
	}

	def isAdmin(userId:UUID):Boolean = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT `admin`
				FROM `users`
				WHERE `id` = {userId}
			""").on(
				"userId" -> userId
			).as(scalar[Boolean].single)(connection)
		}
	}

	def getAdminUserId():List[UUID] = {
		DB.withConnection("main") { connection =>
			SQL("""
				SELECT `id` from `users` where `admin`=1
				""").as(scalar[UUID] *)(connection)
		}
	}

	def manageAdmin(userId:UUID,admin:Boolean) = {
		DB.withConnection("main") { connection =>
			SQL("""
				UPDATE `users` set `admin`= {admin} WHERE `id` = {userId}
				""").on(
				"userId" -> userId,
				"admin" -> admin

				).executeUpdate()(connection)
		}
	}

	def getAllUsers(page: Int, except:UUID) = {
		DB.withConnection("main") { connection =>
		 val found = SQL("""
				SELECT COUNT(1) FROM `users` WHERE `id` != {userId}
				""").on( "userId" -> except ).as(scalar[Long].single)(connection)


		val matches =  SQL("""SELECT * from `users` WHERE  `id` != {userId} ORDER BY `name` LIMIT {limit} OFFSET {offset}""").on(
				"userId" -> except,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
				).as(usersRowParser *)(connection)
		(found,matches)
		}
	}
}

object UserModel extends UserModel
