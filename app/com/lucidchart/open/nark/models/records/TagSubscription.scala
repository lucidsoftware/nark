package com.lucidchart.open.nark.models.records

import java.util.UUID

case class TagSubscription (
	userId: UUID,
	tag: String,
	active: Boolean
) extends AppRecord {
	/**
	 * Create a new Subscription to insert into the database
	 */
	def this(userId: UUID, tag: String) = this(userId, tag, true)
}

case class TagSubscriptionRecord (
	subscription: TagSubscription,
	userOption: Option[User]
) extends AppRecord
