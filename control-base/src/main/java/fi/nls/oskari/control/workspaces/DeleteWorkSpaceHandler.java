package fi.nls.oskari.control.workspaces;

import pl.sito.liiteri.sharing.SharingItem;
import pl.sito.liiteri.sharing.SharingService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.workspaces.service.WorkspaceService;

@OskariActionRoute("DeleteWorkspace")
public class DeleteWorkSpaceHandler extends ActionHandler {

    private static final String WORKSPACEID = "id";
    private static final String DELETE_ONLY_PERMISSION = "deletetOnlyPermission";

    private static final Logger log = LogFactory
            .getLogger(DeleteWorkSpaceHandler.class);

    private WorkspaceService _workspaceService = WorkspaceService.getInstance();
    private SharingService _sharingService = SharingService.getInstance();

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        User user = params.getUser();
        if (user.isGuest())
            throw new ActionDeniedException("Error. There is no logged user.");

        long id;
        boolean deletetOnlyPermission = false;

        try {
            id = Long.parseLong(params.getHttpParam(WORKSPACEID));
            deletetOnlyPermission = Boolean.parseBoolean(params
                    .getHttpParam(DELETE_ONLY_PERMISSION));
        } catch (Exception e) {
            throw new ActionException(
                    "Error during getting workspace parameters");
        }

        if (!deletetOnlyPermission) {
            _workspaceService.deleteWorkspace(id);
        } else {
            _sharingService.DeleteSharingsForUser(
                    SharingItem.ResourceType.WORKSPACE, id, user);
        }

    }
}
