package com.lucidchart.open.nark.openid

import java.net.URL

import scala.collection.JavaConversions

import org.apache.commons.codec.binary.Base64
import org.openid4java.consumer.ConsumerManager
import org.openid4java.discovery.Discovery

import org.openid4java.discovery.DiscoveryInformation

object DiscoveryInformationConverter {
	protected def base64EncodeString(string: String) = new String(Base64.encodeBase64(string.getBytes), "UTF-8")
	protected def base64DecodeString(string: String) = new String(Base64.decodeBase64(string.getBytes), "UTF-8")
	
	/**
	 * Turn an OpenID DiscoveryInformation object into a serialized string.
	 * 
	 * DiscoveryInformation itself is serializable, but I am not a big fan
	 * of the Java serialization stuff. I want interoperability with other
	 * languages, should I choose to use them.
	 * 
	 * @param info to be serialized
	 * @return serialized info
	 */
	def infoToString(info: DiscoveryInformation): String = List(
		info.getOPEndpoint().toString(),
		if (info.hasClaimedIdentifier()) info.getClaimedIdentifier().getIdentifier() else "<NULL>",
		if (info.hasDelegateIdentifier()) info.getDelegateIdentifier() else "<NULL>",
		info.getVersion(),
		JavaConversions.asScalaSet(info.getTypes()).toList.map { element =>
			base64EncodeString(element.asInstanceOf[String])
		}.mkString(",")
	).map { element =>
		base64EncodeString(element)
	}.mkString("-")
	
	/**
	 * Recreate an OpenID DiscoveryInformation from its serialized
	 * string form.
	 * 
	 * @param string to be unserialized
	 * @param discovery used to create the information in the first place
	 * @return recreated discovery information
	 */
	def stringToInfo(string: String, discovery: Discovery): DiscoveryInformation = {
		val parts = string.split("-", 5).map { element =>
			base64DecodeString(element)
		}
		
		val endpoint = new URL(parts(0))
		val claimedIdentifier = if (parts(1) == "<NULL>") null else discovery.parseIdentifier(parts(1))
		val delegate = if (parts(2) == "<NULL>") null else parts(2)
		val version = parts(3)
		val types = parts(4).split(",").map { element => base64DecodeString(element) }
		
		new DiscoveryInformation(endpoint, claimedIdentifier, delegate, version, JavaConversions.setAsJavaSet(types.toSet))
	}
	
	/**
	 * Recreate an OpenID DiscoveryInformation from its serialized
	 * string form.
	 * 
	 * @param string to be unserialized
	 * @param manager used to create the information in the first place
	 * @return recreated discovery information
	 */
	def stringToInfo(string: String, manager: ConsumerManager): DiscoveryInformation = stringToInfo(string, manager.getDiscovery())
}
