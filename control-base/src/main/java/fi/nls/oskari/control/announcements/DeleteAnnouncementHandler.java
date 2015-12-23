package fi.nls.oskari.control.announcements;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.announcements.service.AnnouncementsDbService;
import fi.nls.oskari.announcements.service.AnnouncementsDbServiceIbatisImpl;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.workspaces.DeleteWorkSpaceHandler;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.ResponseHelper;
import fi.nls.oskari.workspaces.service.WorkSpaceDbService;
import fi.nls.oskari.workspaces.service.WorkSpaceServiceIbatisImpl;

@OskariActionRoute("DeleteAnnouncement")
public class DeleteAnnouncementHandler extends ActionHandler{
	
	private static final String ANNOUNCEMENT_ID = "id";

	private static final Logger log = LogFactory
			.getLogger(DeleteAnnouncementHandler.class);
	private static final AnnouncementsDbService announcementService = new AnnouncementsDbServiceIbatisImpl();

	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		Long id;
		User user = params.getUser();
	
		if (!user.isGuest()) {
			try {
				id = Long.parseLong(params.getHttpParam(ANNOUNCEMENT_ID));
			} catch (Exception e) {
				throw new ActionException(
						"Error during getting announcement parameters");
			}
			
			try {
				announcementService.deleteAnnouncement(id);
			} catch (ServiceException e) {
				throw new ActionException("Error during deleting the announcement from database");
			}
			ResponseHelper.writeResponse(params, "Announcement " + id + " deleted");
		}
		else
		{
			ResponseHelper.writeResponse(params, "Error. There is no logged user.");
		}
	}

}