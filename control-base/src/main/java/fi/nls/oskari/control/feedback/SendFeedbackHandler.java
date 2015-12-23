package fi.nls.oskari.control.feedback;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.EmailSender;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("SendFeedback")
public class SendFeedbackHandler extends ActionHandler {
	private static final Logger log = LogFactory.getLogger(SendFeedbackHandler.class);
	private static final String EMAIL_TITLE = "feedback.emailTitle";
	private static final String EMAIL_SENDER_NAME = "mailing.senderName";
	private static final String EMAIL_SENDER_ADDRESS = "mailing.senderAddress";
	private static final String EMAIL_SMTP_HOST_SERVER = "mailing.smtpHostServer";
	
	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		ResponseHelper.writeResponse(params, "Response");
		
		String title = PropertyUtil.get(EMAIL_TITLE);
		String smtpHostServer = PropertyUtil.get(EMAIL_SMTP_HOST_SERVER);
		String senderAddress = PropertyUtil.get(EMAIL_SENDER_ADDRESS);
		String senderName = PropertyUtil.get(EMAIL_SENDER_NAME);
		String message = params.getHttpParam("feedback_message");
		
		String contentText = "Palautteen aihe: " + params.getHttpParam("feedback_topic") +
				"\r\nViesti: " + (message.length() > 2500 ? message.substring(0, 2500) : message) +
				"\r\nEtunimi: " + params.getHttpParam("feedback_first_name") +
				"\r\nSukunimi: " + params.getHttpParam("feedback_last_name") +
				"\r\nPuhelin: " + params.getHttpParam("feedback_phone") +
				"\r\nSähköposti: " + params.getHttpParam("feedback_email") +
				"\r\nSelain: " + params.getHttpParam("feedback_browser_version");
		
		String recipientAddress = PropertyUtil.get("feedback.recipientAddress");
		
		try
		{
			EmailSender.sendEmail(title, contentText, smtpHostServer, senderAddress, senderName, recipientAddress);
		}
		catch (Exception e)
		{
			log.error("Problem with sending email", e);
		}
	}
}
