package fi.nls.oskari.control.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.map.userowndata.GisDataDbService;
import fi.nls.oskari.map.userowndata.GisDataDbServiceImpl;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.EmailSender;
import fi.nls.oskari.util.PropertyUtil;

public class GisDataReminder implements Runnable {

    private static final GisDataDbService gisDataService = new GisDataDbServiceImpl();

    private static final String REMINDER_TIME_IN_DAYS = "userGisData.reminderTimeInDays";
    private static final String EMAIL_TITLE = "userGisData.emailTitle";
    private static final String EMAIL_TEXT = "userGisData.emailText";
    private static final String EMAIL_SENDER_ADDRESS = "mailing.senderAddress";
    private static final String EMAIL_SENDER_NAME = "mailing.senderName";
    private static final String EMAIL_SMTP_HOST_SERVER = "mailing.smtpHostServer";

    private static final String STATUS_SAVED = "SAVED";
    private static final String STATUS_REMINDED = "REMINDED";

    private void RemindOfExpiredUserGisData() throws ActionException {

        // get today date and number of days from config file and calculate
        // proper date
        Calendar expirationDate = Calendar.getInstance();
        expirationDate.add(Calendar.DAY_OF_YEAR,
                Integer.parseInt(PropertyUtil.get(REMINDER_TIME_IN_DAYS, "7")));

        // find data which are 'almost' expired
        List<UserGisData> userGisDataList;
        try {
            userGisDataList = gisDataService.getUserGisDataAfterExpirationDate(
                    expirationDate.getTime(), STATUS_SAVED);
        } catch (Exception e) {
            throw new ActionException(
                    "Error during selecting required data from database");
        }

        // send emails to users with alert about 'almost' expired data
        if (userGisDataList != null) {

            Date today = new Date();
            try {
                UserService userService = UserService.getInstance();

                for (UserGisData u : userGisDataList) {
                    String username = userService.getUser(u.getUserId())
                            .getScreenname();
                    // process only not expired data
                    if (today.before(u.getExpirationDate())) {
                        if (isUsernameEmail(username)) {
                            String title = PropertyUtil.get(EMAIL_TITLE);

                            SimpleDateFormat sdf = new SimpleDateFormat(
                                    "dd.MM.yyyy");
                            String contentText = String.format(
                                    PropertyUtil.get(EMAIL_TEXT),
                                    sdf.format(u.getExpirationDate()));
                            String smtpHostServer = PropertyUtil
                                    .get(EMAIL_SMTP_HOST_SERVER);
                            String senderAddress = PropertyUtil
                                    .get(EMAIL_SENDER_ADDRESS);
                            String senderName = PropertyUtil
                                    .get(EMAIL_SENDER_NAME);
                            String recipientAddress = username;

                            EmailSender.sendEmail(title, contentText,
                                    smtpHostServer, senderAddress, senderName,
                                    recipientAddress);
                            // update status to REMINDED, if email sending successful
                            u.setStatus(STATUS_REMINDED);
                        } else {
                            // update status to REMINDED, if no email address
                            u.setStatus(STATUS_REMINDED);
                        }

                        try {
                            gisDataService.update(u);
                        } catch (RuntimeException e) {
                            throw new ActionException(
                                    "Error during saving gis data to database",
                                    e);
                        }
                    }
                }
            } catch (ServiceException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        try {
            this.RemindOfExpiredUserGisData();
        } catch (ActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private boolean isUsernameEmail(String username) throws ServiceException {
        return username.contains("@");
    }

}
