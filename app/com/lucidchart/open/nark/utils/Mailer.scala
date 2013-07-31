package com.lucidchart.open.nark.utils

import org.apache.commons.mail.HtmlEmail
import play.api.Play.configuration
import play.api.Play.current
import play.api.Logger


case class MailerSMTPConfiguration(
	host: String,
	port: Int,
	user: String,
	pass: String
)

case class MailerAddress(
	email: String,
	name: String = ""
)

case class MailerMessage(
	from: MailerAddress,
	reply: Option[MailerAddress] = None,
	to: Iterable[MailerAddress],
	cc: Iterable[MailerAddress] = Nil,
	bcc: Iterable[MailerAddress] = Nil,
	subject: String,
	html: String,
	text: String
)


object Mailer {
	private val enabled = configuration.getBoolean("mailer.enabled").get
	private val smtpConfig = MailerSMTPConfiguration(
		configuration.getString("mailer.smtp.host").get,
		configuration.getInt("mailer.smtp.port").get,
		configuration.getString("mailer.smtp.user").get,
		configuration.getString("mailer.smtp.pass").get
	)
	
	/**
	 * Send an email message
	 * 
	 * Throws any and all exceptions
	 * 
	 * @param message Details about the message to send
	 */
	def send(message: MailerMessage) {
		if (!enabled) {
			Logger.info("Not sending email to " + message.to + " with subject '" + message.subject + "' because the mailer is disabled.")
		}
		else {
			val email = new HtmlEmail()
			
			email.setSmtpPort(smtpConfig.port)
			email.setHostName(smtpConfig.host)
			email.setAuthentication(smtpConfig.user, smtpConfig.pass)
			
			email.setHtmlMsg(message.html)
			email.setTextMsg(message.text)
			email.setSubject(message.subject)
			email.setFrom(message.from.email, message.from.name)
			
			message.reply.map { reply =>
				email.addReplyTo(reply.email, reply.name)
			}
			
			message.to.foreach { to =>
				email.addTo(to.email, to.name)
			}
			
			message.cc.foreach { cc =>
				email.addCc(cc.email, cc.name)
			}
			
			message.bcc.foreach { bcc =>
				email.addBcc(bcc.email, bcc.name)
			}
			
			email.send()
		}
	}
}
