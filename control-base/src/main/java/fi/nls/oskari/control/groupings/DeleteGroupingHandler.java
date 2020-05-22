package fi.nls.oskari.control.groupings;

import java.util.List;

import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.domain.Role;
import pl.sito.liiteri.groupings.service.GroupingsService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.groupings.db.GroupingDbService;
import fi.nls.oskari.groupings.db.GroupingServiceIbatisImpl;
import fi.nls.oskari.groupings.db.GroupingThemeDbService;
import fi.nls.oskari.groupings.db.GroupingThemeServiceIbatisImpl;
import fi.nls.oskari.groupings.themes.api.GroupingThemesApiService;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("DeleteGrouping")
public class DeleteGroupingHandler extends ActionHandler {

	private static final String GROUPINGID = "groupingId";
	private static final String GROUPINGTYPE = "type";
	private static final Logger log = LogFactory
			.getLogger(DeleteGroupingHandler.class);

	private static final GroupingsService _service = GroupingsService.getInstance();
	private final static String[] AUTHORIZED_ROLES = new String [] { Role.GROUPINGS_ADMIN };
	
	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		
		if (!params.getUser().isSuperAdmin() && !params.getUser().hasAnyRoleIn(AUTHORIZED_ROLES)) {
			throw new ActionDeniedException("Denied, user not admin");
		}
		
		Long id;
		String type = params.getHttpParam(GROUPINGTYPE);
		try {
			id = Long.parseLong(params.getHttpParam(GROUPINGID));
		} catch (Exception e) {
			throw new ActionException(
					"Error during getting grouping id paramaeter");
		}
		try {
			if (type.equals("package")) {
				_service.deleteServicePackage(id);
			} else if (type.equals("theme")) {				
				_service.deleteGroupingTheme(id);
			} else

			{
				throw new ServiceException("Unknown grouping type");
			}
		} catch (ServiceException e) {
			throw new ActionException(
					"Error during deleting object from database");
		}
		ResponseHelper.writeResponse(params, "Grouping " + id + " deleted");
	}
}
