package fi.nls.oskari.control.workspaces;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.ResponseHelper;
import fi.nls.oskari.workspaces.service.WorkspaceService;

@OskariActionRoute("GetWorkspaces")
public class GetWorkSpacesHandler extends ActionHandler {

    private static final String WORKSPACE_TYPE = "type";
    private static final String SHOW_HIDDEN = "showHidden";
    private static final String WORKSPACE_ID = "workspaceId";

    private static final Logger log = LogFactory
            .getLogger(GetWorkSpacesHandler.class);

    private WorkspaceService _workspaceService = WorkspaceService.getInstance();

    @Override
    public void handleAction(ActionParameters params) throws ActionException {

        List<WorkSpace> ownWorkSpaces = new ArrayList<WorkSpace>();
        ;
        List<WorkSpace> externalWorkSpaces = new ArrayList<WorkSpace>();
        List<WorkSpace> hiddenWorkSpaces = new ArrayList<WorkSpace>();
        Date expirationDate;
        User user = params.getUser();

        Boolean showHidden = (params.getHttpParam(SHOW_HIDDEN, "false").equals(
                "true") ? true : false);
        String workspaceType = params.getRequiredParam(WORKSPACE_TYPE);

        if (user.isGuest())
            throw new ActionDeniedException("User is not logged");

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            expirationDate = sdf.parse(sdf.format(new Date()));

        } catch (ParseException e1) {
            throw new ActionException("Error during date parsing");
        }

        try {
            long workSpaceId = (params.getHttpParam(WORKSPACE_ID) != null ? Long
                    .parseLong(params.getHttpParam(WORKSPACE_ID)) : 0);
            if (workspaceType.equals("own") || workspaceType.equals("all")) {
                if (showHidden) {
                    ownWorkSpaces = _workspaceService.getWorkspacesForUser(
                            user, expirationDate);
                } else {
                    ownWorkSpaces = _workspaceService
                            .getVisibleWorkspacesForUser(user, expirationDate);
                }
            } else if (workspaceType.equals("hidden") && workSpaceId > 0) {
                hiddenWorkSpaces.add(_workspaceService
                        .getHiddenWorkspace(workSpaceId));
            }
            if (workspaceType.equals("shared") || workspaceType.equals("all")) {
                externalWorkSpaces = _workspaceService
                        .getExternalWorkspacesForUser(user, expirationDate);
            }

        } catch (Exception e) {

            throw new ActionException(
                    "Error during selecting required data from database");
        }
        try {
            JSONObject main = JSONWorkSpacesHelper.createWorkSpacesJSONOutput(
                    ownWorkSpaces, externalWorkSpaces, hiddenWorkSpaces);
            ResponseHelper.writeResponse(params, main);
        } catch (Exception e) {

            throw new ActionException(
                    "Error during creating JSON workspaces object");
        }

    }

}
