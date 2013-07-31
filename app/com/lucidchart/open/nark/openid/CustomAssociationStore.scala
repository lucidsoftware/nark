package com.lucidchart.open.nark.openid

import org.openid4java.association.Association
import org.openid4java.consumer.ConsumerAssociationStore
import com.lucidchart.open.nark.models.OpenIDModel
import com.lucidchart.open.nark.models.records.OpenIDAssociation
import com.lucidchart.open.nark.models.records.OpenIDAssociationType
import java.util.Date

class CustomAssociationStore extends ConsumerAssociationStore {
	protected val typesToEnum = Map(
		Association.TYPE_HMAC_SHA1   -> OpenIDAssociationType.hmacSha1,
		Association.TYPE_HMAC_SHA256 -> OpenIDAssociationType.hmacSha256
	)
	
	protected def associationToRecord(provider: String, association: Association) = {
		new OpenIDAssociation(
			provider,
			association.getHandle(),
			association.getExpiry(),
			typesToEnum(association.getType()),
			association.getMacKey().getEncoded()
		)
	}
	
	protected def recordToAssociation(record: OpenIDAssociation) = {
		record.associationType match {
			case OpenIDAssociationType.hmacSha1 => Association.createHmacSha1(record.handle, record.secret, record.expires)
			case OpenIDAssociationType.hmacSha256 => Association.createHmacSha256(record.handle, record.secret, record.expires)
		}
	}
	
	/**
	 * Save an association
	 */
	def save(provider: String, association: Association) {
		val associationRecord = associationToRecord(provider, association)
		OpenIDModel.saveAssociation(associationRecord)
	}
	
	/**
	 * Load a specific association
	 */
	def load(provider: String, handle: String): Association = {
		OpenIDModel.findAssociation(provider, handle).map { record =>
			recordToAssociation(record)
		}.orNull
	}
	
	/**
	 * Load the last association saved for this provider
	 */
	def load(provider: String): Association = {
		OpenIDModel.findLastAssociation(provider).map { record =>
			recordToAssociation(record)
		}.orNull
	}
	
	/**
	 * Remove an association
	 */
	def remove(provider: String, handle: String) {
		OpenIDModel.removeAssociation(provider, handle)
	}
	
	/**
	 * Cleanup all the expired associations
	 */
	def cleanup() {
		// give 1 hour of leeway
		val cutoff = new Date(System.currentTimeMillis() - 3600000)
		OpenIDModel.cleanAssociationsBefore(cutoff)
	}
}
