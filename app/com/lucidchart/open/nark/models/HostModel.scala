package com.lucidchart.open.nark.models

import java.util.Date
import java.util.UUID

import com.lucidchart.open.nark.models.records.Host
import com.lucidchart.open.nark.models.records.HostState

import AnormImplicits._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB

class HostModel extends AppModel {
	protected val hostsRowParser = {
		get[String]("name") ~
		get[Int]("state") ~
		get[Date]("last_confirmed") map {
			case name ~ state ~ lastConfirmed =>
				new Host(name, HostState(state), lastConfirmed)
		}
	}

	/**
	 * upsert a host record
	 */
	def upsert(host: Host) {
		DB.withConnection("main") { connection =>
			SQL("""
				INSERT INTO `hosts` (`name`, `state`, `last_confirmed`)
				VALUES ({name}, {state}, {last_confirmed})
				ON DUPLICATE KEY UPDATE `state` = {state}, `last_confirmed` = {last_confirmed}
			""").on(
				"name"           -> host.name,
				"state"          -> host.state.id,
				"last_confirmed" -> host.lastConfirmed
			).executeUpdate()(connection)
		}
	}

	/**
	 * delete any host records that haven't been updated since before date
	 */
	def cleanBefore(date: Date) {
		DB.withConnection("main") { connection =>
			SQL("""
				DELETE FROM `hosts` WHERE `last_confirmed` < {date}
			""").on(
				"date" -> date
			).executeUpdate()(connection)
		}
	}

	/**
	 * Find all the hosts that match the name
	 */
	def search(name: String, page: Int) = {
		DB.withConnection("main") { connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `hosts`
				WHERE `name` LIKE {name}
			""").on(
				"name" -> name
			).as(scalar[Long].single)(connection)

			val matches = SQL("""
				SELECT * FROM `hosts`
				WHERE `name` LIKE {name}
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on(
				"name" -> name,
				"limit" -> configuredLimit,
				"offset" -> configuredLimit * page
			).as(hostsRowParser *)(connection)

			(found, matches)
		}
	}

	/**
	 * Find all the hosts that match
	 */
	def search(name: String, states: Set[HostState.Value]) = {
		if (states.isEmpty) {
			Nil
		}
		else {
			DB.withConnection("main") { connection =>
				RichSQL("""
					SELECT * FROM `hosts`
					WHERE `name` LIKE {name} AND `state` IN ({states})
				""").onList(
					"states" -> states.map(_.id)
				).toSQL.on(
					"name" -> name
				).as(hostsRowParser *)(connection)
			}
		}
	}
}

object HostModel extends HostModel
