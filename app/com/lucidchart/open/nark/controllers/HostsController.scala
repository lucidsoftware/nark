package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppFlash, AppAction, AuthAction, DashboardAction}
import com.lucidchart.open.nark.views
import com.lucidchart.open.nark.models.HostModel
import com.lucidchart.open.nark.models.records.{Host, HostState, Pagination}

class HostsController extends AppController {
	/**
	 * Display a paginated list of hosts
	 */
	def search(term: String, page: Int) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction { implicit request =>
			val realPage = page.max(1)
			val (found, matches) = HostModel.search(term, realPage - 1)
			Ok(views.html.hosts.search(term, Pagination[Host](realPage, found, HostModel.configuredLimit, matches)))
		}
	}

	/**
	 * Generate a string that Graphite can use. The string will contain
	 * hostnames that were searched for. This is useful for only displaying
	 * active hosts in dashboards
	 */
	def buildGraphiteTarget(searchName: String, searchState: String) = AuthAction.maybeAuthenticatedUser { implicit userOption =>
		AppAction { implicit request =>
			val states = if (HostState.values.map(_.toString).contains(searchState)) {
				Set(HostState.withName(searchState))
			}
			else {
				HostState.values
			}

			val hosts = HostModel.search(searchName, states)
			val target = if (hosts.isEmpty) {
				None
			}
			else {
				val hostParts = hosts.map(h => h.name.split('.'))
				val numParts = hostParts(0).size

				// all hosts must have the same number of '.'s or the graphite target can't be built.
				hostParts.map(p => require(p.size == numParts))

				val targetParts = List.fill(numParts)(scala.collection.mutable.Set[String]())
				hostParts.map { hp =>
					for (i <- 0 until numParts) {
						targetParts(i) += hp(i)
					}
				}

				Some(targetParts.map { set =>
					"{" + set.mkString(",") + "}"
				}.mkString("."))
			}

			Ok(views.html.hosts.buildGraphiteTarget(searchName, searchState, target))
		}
	}
}

object HostsController extends HostsController
