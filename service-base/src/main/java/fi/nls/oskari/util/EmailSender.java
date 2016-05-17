package fi.nls.oskari.util;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailSender {

    public static void sendEmail(String title, String contentText,
            String smtpHostServer, String senderAddress, String senderName,
            String recipientAddress) {

        Properties props = System.getProperties();
        props.put("mail.smtp.host", smtpHostServer);
        Session session = Session.getInstance(props, null);

        try {
            MimeMessage msg = new MimeMessage(session);

            msg.setFrom(new InternetAddress(senderAddress, senderName));
            msg.setSubject(title, "UTF-8");
            msg.setContent(contentText, "text/html; charset=utf-8");

            if (recipientAddress != null) {
                msg.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(recipientAddress, false));

                Transport.send(msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}