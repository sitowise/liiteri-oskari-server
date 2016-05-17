package fi.nls.oskari.control.workspaces;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.workspaces.service.WorkspaceService;
import pl.sito.liiteri.sharing.EmailNotificationService;
import pl.sito.liiteri.sharing.INotificationService;
import pl.sito.liiteri.sharing.NotificationItem;

public class WorkspaceReminder implements Runnable {

    private static final WorkspaceService service = WorkspaceService
            .getInstance();
    private static final INotificationService notificationService = new EmailNotificationService();

    private static final String REMINDER_TIME_IN_DAYS = "workspaces.reminderTimeInDays";
    private static final String EMAIL_TITLE = "workspaces.emailTitle";
    private static final String EMAIL_TEXT = "workspaces.emailText";

    private static final String WORKSPACE_STATUS_REMINDED = "REMINDED";

    private void RemindOfExpiredWorkspaces() throws ActionException {

        Calendar expirationDate = Calendar.getInstance();
        expirationDate.add(Calendar.DAY_OF_YEAR,
                Integer.parseInt(PropertyUtil.get(REMINDER_TIME_IN_DAYS, "7")));

        // find workspaces which are 'almost' expired
        List<WorkSpace> workspaces;
        try {
            workspaces = service.getExpiredWorkspaces(expirationDate.getTime());
        } catch (Exception e) {
            throw new ActionException(
                    "Error during selecting required data from database");
        }

        if (workspaces == null)
            return;

        try {
            UserService userService = UserService.getInstance();

            for (WorkSpace workspace : workspaces) {
                String username = userService.getUser(workspace.getUserId())
                        .getScreenname();
                if (isUsernameEmail(username)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

                    NotificationItem item = new NotificationItem();
                    item.setContent(String.format(PropertyUtil.get(EMAIL_TEXT),
                            workspace.getName(),
                            sdf.format(workspace.getExpirationDate())));
                    item.setRecipient(username);
                    item.setTitle(PropertyUtil.get(EMAIL_TITLE));
                    notificationService.SendNotification(item);
                    // update status of workspace to REMINDED, if email sending successful
                    workspace.setStatus(WORKSPACE_STATUS_REMINDED);
                } else {
                    // update status of workspace to REMINDED, if no email address
                    workspace.setStatus(WORKSPACE_STATUS_REMINDED);
                }

                try {
                    service.updateWorkspace(workspace, true);
                } catch (ServiceException e) {
                    throw new ActionException(
                            "Error during saving workspace to database", e);
                }
            }
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            this.RemindOfExpiredWorkspaces();
        } catch (ActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private boolean isUsernameEmail(String username) {
        return username.contains("@");
    }
}
