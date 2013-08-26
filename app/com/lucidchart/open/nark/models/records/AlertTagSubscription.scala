package com.lucidchart.open.nark.models.records

import java.util.UUID

case class AlertTagSubscription (
	userId: UUID,
	tag: String,
	active: Boolean
) extends AppRecord {
	/**
	 * Create a new Subscription to insert into the database
	 */
	def this(userId: UUID, tag: String) = this(userId, tag, true)
}

case class AlertTagSubscriptionRecord (
	subscription: AlertTagSubscription,
	userOption: Option[User]
) extends AppRecord
