package com.lucidchart.open.nark.models

import java.util.Date
import java.util.UUID

import com.lucidchart.open.nark.models.records.Host
import com.lucidchart.open.nark.models.records.HostState
import com.lucidchart.open.relate._
import com.lucidchart.open.relate.Query._

import play.api.Play.current
import play.api.db.DB

class HostModel extends AppModel {
	protected val hostsRowParser = RowParser { row =>
		Host(
			row.string("name"),
			HostState(row.int("state")),
			row.date("last_confirmed")
		)
	}

	/**
	 * upsert a host record
	 */
	def upsert(host: Host) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				INSERT INTO `hosts` (`name`, `state`, `last_confirmed`)
				VALUES ({name}, {state}, {last_confirmed})
				ON DUPLICATE KEY UPDATE `state` = {state}, `last_confirmed` = {last_confirmed}
			""").on { implicit query =>
				string("name", host.name)
				int("state", host.state.id)
				date("last_confirmed", host.lastConfirmed)
			}.executeUpdate()
		}
	}

	/**
	 * delete any host records that haven't been updated since before date
	 */
	def cleanBefore(before: Date) {
		DB.withConnection("main") { implicit connection =>
			SQL("""
				DELETE FROM `hosts` WHERE `last_confirmed` < {date}
			""").on { implicit query =>
				date("date", before)
			}.executeUpdate()
		}
	}

	/**
	 * Find all the hosts that match the name
	 */
	def search(name: String, page: Int) = {
		DB.withConnection("main") { implicit connection =>
			val found = SQL("""
				SELECT COUNT(1) FROM `hosts`
				WHERE `name` LIKE {name}
			""").on { implicit query =>
				string("name", name)
			}.asScalar[Long]

			val matches = SQL("""
				SELECT * FROM `hosts`
				WHERE `name` LIKE {name}
				ORDER BY `name` ASC
				LIMIT {limit} OFFSET {offset}
			""").on { implicit query =>
				string("name", name)
				int("limit", configuredLimit)
				int("offset", configuredLimit * page)
			}.asList(hostsRowParser)

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
			DB.withConnection("main") { implicit connection =>
				SQL("""
					SELECT * FROM `hosts`
					WHERE `name` LIKE {name} AND `state` IN ({states})
				""").expand { implicit query =>
					commaSeparated("states", states.size)
				}.on { implicit query =>
					ints("states", states.map(_.id))
					string("name", name)
				}.asList(hostsRowParser)
			}
		}
	}
}

object HostModel extends HostModel
