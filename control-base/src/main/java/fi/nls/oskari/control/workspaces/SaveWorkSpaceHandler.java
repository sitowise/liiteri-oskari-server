package fi.nls.oskari.control.workspaces;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONException;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.domain.workspaces.WorkSpaceSharing;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;
import fi.nls.oskari.workspaces.service.WorkspaceService;

@OskariActionRoute("SaveWorkspace")
public class SaveWorkSpaceHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(SaveWorkSpaceHandler.class);

    private WorkspaceService _service;

    private static final String WORKSPACE_PARAM = "workspace";
    private static final String WORKSPACE_ID = "id";
    private static final String WORKSPACE_NAME = "name";
    private static final String WORKSPACE_USERS = "users";
    private static final String WORKSPACE_HIDDEN = "hidden";

    private static final String EXPIRATION_TIME_IN_DAYS = "workspaces.expirationTimeInDays";

    private static final String WORKSPACE_STATUS_SAVED = "SAVED";

    // UNIT TESTS
    public void setService(final WorkspaceService service) {
        this._service = service;
    }

    @Override
    public void init() {
        super.init();
        if (_service == null) {
            setService(WorkspaceService.getInstance());
        }
    }

    @Override
    public void handleAction(ActionParameters params) throws ActionException {

        String workSpaceString = params.getRequiredParam(WORKSPACE_PARAM);
        String name = params.getRequiredParam(WORKSPACE_NAME);

        User user = params.getUser();
        if (user.isGuest())
            throw new ActionDeniedException("User is not logged");

        String message;
        Long insertId = null;

        String idString = params.getHttpParam(WORKSPACE_ID);
        String users = params.getHttpParam(WORKSPACE_USERS);
        Boolean hidden = (params.getHttpParam(WORKSPACE_HIDDEN, "false")
                .equals("true") ? true : false);
        long id = 0;
        if (idString != null && !idString.isEmpty()) {
            id = Long.parseLong(idString);
        }

        Date dtExpDate;
        Calendar dtExpCalendar = Calendar.getInstance();
        dtExpCalendar.add(Calendar.DAY_OF_YEAR, Integer.parseInt(PropertyUtil
                .get(EXPIRATION_TIME_IN_DAYS, "60")));
        dtExpDate = dtExpCalendar.getTime();

        WorkSpace workspace = new WorkSpace();
        workspace.setName(name);
        workspace.setId(id);
        workspace.setSettings(workSpaceString);
        workspace.setExpirationDate(dtExpDate);
        workspace.setUserId(user.getId());
        workspace.setHidden(hidden);
        workspace.setStatus(WORKSPACE_STATUS_SAVED);

        try {
            List<WorkSpaceSharing> sharingList = JSONWorkSpacesHelper
                    .getWorkSpaceSharingObjectList(users);
            workspace.setWorkSpaceSharing(sharingList);
        } catch (JSONException eej) {
            throw new ActionException("Error during sharing data parsing");
        }

        if (workspace.getId() == 0) {
            // SAVE Operation
            try {
                if (hidden == true || _service.canAddWorkspace(user.getId())) {
                    _service.addWorkspace(workspace);
                    message = "WorkSpace has been created";
                } else {
                    // throw new
                    // ServiceException("The amount of workspaces reached the limit.");
                    throw new ServiceException(
                            "Sallittujen työtilojen määrä on ylittynyt.");
                }
            } catch (ServiceException e) {

                // String errMess =
                // "Error during saving new workspace to database.";
                String errMess = "Virhe työtilan tallennuksessa.";
                if (!e.getMessage().isEmpty()) {
                    errMess += " " + e.getMessage();
                }
                throw new ActionException(errMess);
            }

        } else {
            // UPDATE Operation
            try {
                _service.updateWorkspace(workspace);
                message = "WorkSpace has been updated";
            } catch (ServiceException e) {
                // throw new
                // ActionException("Error during saving workspace to database",
                // e);
                throw new ActionException("Virhe työtilan tallennuksessa", e);
            }

        }

        ResponseHelper
                .writeResponse(params, JSONWorkSpacesHelper
                        .createJSONMessageObject(insertId, message));
    }

}
