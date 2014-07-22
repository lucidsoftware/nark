package com.lucidchart.open.nark.controllers

import com.lucidchart.open.nark.request.{AppAction, AuthAction}
import com.lucidchart.open.nark.models.{AlertModel, AlertTagModel, AlertTagSubscriptionModel, AlertTargetStateModel, TagConverter}
import com.lucidchart.open.nark.models.records.{Alert, Pagination, SickTarget, TagMap}
import com.lucidchart.open.nark.views
import play.api.libs.json.Json

object AlertTagsController extends AlertTagsController
class AlertTagsController extends AppController {
	/*
	 * Get tag and all the alerts it is associated with.
	 */
	def tag(tag: String) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val alertIds = AlertTagModel.findAlertsByTag(tag).map(_.recordId)
			val alerts = AlertModel.findAlertByID(alertIds)
			val subscriptions = AlertTagSubscriptionModel.getSubscriptionsByTag(tag)
			Ok(views.html.alerttags.tag(tag, alerts.filter(!_.deleted), subscriptions))
		}
	}

	/**
	 * Search for a specific tag
	 */
	def search(term: String, page: Int) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val realPage = page.max(1)
			val (found, tags) = AlertTagModel.search(term, realPage - 1)
			val alertTags = AlertTagModel.findAlertsByTag(tags.map{_.tag})
			val alerts = AlertModel.findAlertByID(alertTags.map(_.recordId).distinct).filter(!_.deleted)
			Ok(views.html.alerttags.search(term, Pagination[TagMap[Alert]](realPage, found, AlertTagModel.configuredLimit, List(TagConverter.toTagMap(alertTags, alerts)))))
		}
	}

	/**
	 * Search tags by name. Returns json formatted for jquery-tokeninput.
	 */
	def searchToJson(term: String) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val (found, matches) = AlertTagModel.search(term + "%", 0)
			Ok(Json.toJson(matches.map{ m =>
				Json.obj("id" -> m.recordId.toString, "name" -> m.tag)
			}))
		}
	}

	/**
	 * Get the active alerts associated with a tag
	 * @param tag the tag to search for
	 */
	def activeAlerts(tag: String) = AuthAction.maybeAuthenticatedUser { implicit user =>
		AppAction { implicit request =>
			val alertIds = AlertTagModel.findAlertsByTag(tag).map(_.recordId)
			val sickTargets = AlertTargetStateModel.getSickTargets(alertIds).map { target =>
				(target.alertId, target)
			}.groupBy { t => t._1 }.mapValues(_.map(_._2))
			val alerts = AlertModel.findAlertByID(sickTargets.keys.toList)
			val result = alerts.flatMap { alert =>
				sickTargets(alert.id).map { target =>
					SickTarget(
						alert.id,
						alert.name,
						target.target,
						target.state,
						target.lastUpdated
					)
				}
			}
			Ok(views.html.alerttags.activeAlerts(tag, result))
		}
	}
}