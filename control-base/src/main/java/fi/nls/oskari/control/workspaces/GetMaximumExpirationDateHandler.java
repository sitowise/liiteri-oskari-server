package fi.nls.oskari.control.workspaces;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.ResponseHelper;
import fi.nls.oskari.workspaces.service.WorkSpaceRoleSettingsDbService;
import fi.nls.oskari.workspaces.service.WorkSpaceRoleSettingsServiceIbatisImpl;

@OskariActionRoute("GetWorkSpaceExpirationDateLimit")
public class GetMaximumExpirationDateHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(GetMaximumExpirationDateHandler.class);
    private static final WorkSpaceRoleSettingsDbService workSpaceRoleSettingsService = new WorkSpaceRoleSettingsServiceIbatisImpl();

    // private static final UserService userService = new DatabaseUserService();

    @Override
    public void handleAction(ActionParameters params) throws ActionException {

        Date dateWithoutTime;
        User user = params.getUser();
        /*
         * User user; try { user = userService.getUser("admin"); } catch
         * (ServiceException e2) { throw new ActionException(
         * "Error during selecting user from database"); } //params.getUser();
         */
        if (!user.isGuest()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                dateWithoutTime = sdf.parse(sdf.format(new Date()));

            } catch (ParseException e1) {
                throw new ActionException("Error during date parsing");
            }
            try {

                int days = workSpaceRoleSettingsService
                        .getMaxExpirationDateLimit(user.getId());
                Calendar cal = Calendar.getInstance();
                cal.setTime(dateWithoutTime);
                cal.add(Calendar.DATE, days);
                Date toSend = cal.getTime();
                ResponseHelper.writeResponse(params, new SimpleDateFormat(
                        "yyyy-MM-dd").format(toSend));

            } catch (ServiceException e) {
                throw new ActionException(
                        "Error during getting expiration date limit");
            }
        } else {
            ResponseHelper.writeResponse(params,
                    "Error. There is no logged user.");
        }

    }

}
