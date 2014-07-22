package com.lucidchart.open.nark.jobs.alerts

import java.util.Properties
import javax.mail._
import internet._
import play.api.Play.current
import play.api.Play.configuration

object Mailer {
	private[alerts] val fromEmail  = configuration.getString("mailer.fromemail").get
	private[alerts] val mailerEnabled = configuration.getBoolean("mailer.enabled").get
	private[alerts] val mailerDebug = configuration.getBoolean("mailer.debug").get

	private[alerts] class SMTPAuthenticator extends javax.mail.Authenticator {
		override def getPasswordAuthentication() = {
			val username = configuration.getString("mailer.smtp.user").get
			val password = configuration.getString("mailer.smtp.pass").get
			new PasswordAuthentication(username, password)
		}
	}

	private[alerts] val auth = new SMTPAuthenticator()
	private[alerts] val props = {
		val p = new Properties()
		val port: java.lang.Integer = configuration.getInt("mailer.smtp.port").get
		p.put("mail.transport.protocol", "smtp")
		p.put("mail.smtp.host", configuration.getString("mailer.smtp.host").get)
		p.put("mail.smtp.port", port)
		p.put("mail.smtp.auth", "true")
		p
	}
}

trait Mailer {
	protected def sendEmails(toEmails: List[String], subject: String, textBody: String, htmlBody:String) : Int = {
		if(Mailer.mailerEnabled && !toEmails.isEmpty) {
			val mailSession = Session.getInstance(Mailer.props, Mailer.auth)

			if (Mailer.mailerDebug) {
				mailSession.setDebug(true)
			}

			val transport = mailSession.getTransport()
			val message = new MimeMessage(mailSession)

			message.setFrom(new InternetAddress(Mailer.fromEmail, "nark"))
			message.setReplyTo(Array(new InternetAddress(Mailer.fromEmail, "nark")))
			message.setSubject(subject)

			toEmails.map{email => message.addRecipient(Message.RecipientType.TO, new InternetAddress(email, ""))}

			val textPart = new MimeBodyPart()
			textPart.setContent(textBody,"text/plain")
			val htmlPart = new MimeBodyPart()
			htmlPart.setContent(htmlBody,"text/html")
			val mp = new MimeMultipart("alternative")
			mp.addBodyPart(textPart)
			mp.addBodyPart(htmlPart)
			message.setContent(mp)
			message.saveChanges()
			transport.connect()
			transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO))
			transport.close()
			// no exceptions = success
			toEmails.length
		} else {
			0
		}
	}
}