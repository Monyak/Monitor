package imax.monitor.server;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mailer {

	public void sendMail(String email, String userName, String subject,
			String message) throws UnsupportedEncodingException,
			MessagingException, AddressException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		
		MimeMessage msg = new MimeMessage(session);
		String encodingOptions = "text/html; charset=UTF-8";
	    msg.setHeader("Content-Type", encodingOptions);
		msg.setFrom(new InternetAddress("monitor318@gmail.com", "Monitor Notification"));
		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email,
				userName));
		msg.setSubject(subject, "UTF-8");
		msg.setText(message);
		Transport.send(msg);
	}
}
