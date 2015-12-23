package fi.nls.oskari.control.workspaces;

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

@OskariActionRoute("GetMaximumWorkSpaceAmount")
public class GetMaximumWorkSpaceAmount extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(GetMaximumWorkSpaceAmount.class);
    private static final WorkSpaceRoleSettingsDbService workSpaceRoleSettingsService = new WorkSpaceRoleSettingsServiceIbatisImpl();

    // private static final UserService userService = new DatabaseUserService();

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        User user = params.getUser();
        /*
         * User user; try { user = userService.getUser("admin"); } catch
         * (ServiceException e2) { throw new ActionException(
         * "Error during selecting user from database"); } //params.getUser();
         */
        if (!user.isGuest()) {
            try {

                int workspacesLimit = workSpaceRoleSettingsService
                        .getMaxWorkSpaceAmount(user.getId());
                ResponseHelper.writeResponse(params, workspacesLimit);
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
