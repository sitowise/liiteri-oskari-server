package pl.sito.liiteri.sharing;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.EmailSender;
import fi.nls.oskari.util.PropertyUtil;

public class EmailNotificationService implements INotificationService
{
    static {
        // populate properties before initializing logger since logger implementation is
        // configured in properties
        PropertyUtil.loadProperties("/oskari.properties");
        PropertyUtil.loadProperties("/oskari-ext.properties");
    }
	
	private static final Logger log = LogFactory.getLogger(EmailNotificationService.class);
	
	private static final String SMTP_HOST_SERVER = "mailing.smtpHostServer";
	private static final String SENDER_ADDRESS = "mailing.senderAddress";
	private static final String SENDER_NAME = "mailing.senderName";
	
	private String smtpHostServer;
	private String senderAddress;
	private String senderName;
	
	public EmailNotificationService() {
		this.smtpHostServer = PropertyUtil.get(SMTP_HOST_SERVER);
		this.senderAddress = PropertyUtil.get(SENDER_ADDRESS);
		this.senderName = PropertyUtil.get(SENDER_NAME);
	}
	
	public void SendNotification(NotificationItem item) {
		log.info("Sending notification %s", item.toString());
		
		if (item.getRecipient() == null)
			log.warn("There is no recipient for", item.toString());
		else {
			EmailSender.sendEmail(item.getTitle(), item.getContent(), smtpHostServer,
					senderAddress, senderName, item.getRecipient());	
		}			
	}
}
