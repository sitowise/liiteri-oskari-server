package fi.nls.oskari.control.groupings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.sito.liiteri.groupings.service.GroupingsService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.szopa.requests.SzopaRequest;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.groupings.Grouping;
import fi.nls.oskari.domain.groupings.GroupingPermission;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.domain.groupings.GroupingThemeData;
import fi.nls.oskari.groupings.db.GroupingThemeDataServiceIbatisImpl;
import fi.nls.oskari.groupings.db.GroupingThemeDbService;
import fi.nls.oskari.groupings.db.GroupingThemeServiceIbatisImpl;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetPermittedGroupings")
public class GetPermittedGroupingsHandler extends ActionHandler {

	private static final Logger log = LogFactory
			.getLogger(GetPermittedGroupingsHandler.class);

	private static final GroupingThemeDataServiceIbatisImpl groupingThemeDataService = new GroupingThemeDataServiceIbatisImpl();

	private static final GroupingsService _service = GroupingsService.getInstance();

	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		List<Grouping> groupings = new ArrayList<Grouping>();
		List<GroupingTheme> themes = new ArrayList<GroupingTheme>();
		List<GroupingThemeData> data = groupingThemeDataService.findAll();
		List<GroupingPermission> groupingPermittedUsers;
		try {
			groupingPermittedUsers = _service.getAllUserPermissions();	
		} catch (ServiceException ex) {
			throw new ActionException(
					"Error during getting permission for user", ex);
		}
		List<GroupingPermission> groupingPermittedRoles;
		try {
			groupingPermittedRoles = _service.getAllRolePermissions();	
		} catch (ServiceException ex) {
			throw new ActionException(
					"Error during getting permission for role", ex);
		}

		User user = params.getUser();

		try {
			groupings = _service.getServicePackagesForUser(user);
		} catch (ServiceException ex) {
			throw new ActionException(
					"Error during getting groupings for user", ex);
		}
		
		try {
			themes = _service.getGroupingThemesForUser(user);
		} catch (ServiceException ex) {
			throw new ActionException(
					"Error during getting groupings for user", ex);
		}

		HashMap<Long, String> indicatorNames = new HashMap<Long,String>();
		final SzopaRequest req = SzopaRequest.getInstance("indicators");

		req.setIndicator("");
		req.setFormat("");

		req.setVersion("v1"); 

		JSONArray indicators = JSONHelper.createJSONArray(req.getData());

		for(int i = 0; i < indicators.length(); ++i) {
			JSONObject obj;
			try {
				obj = indicators.getJSONObject(i);

				indicatorNames.put(obj.getLong("id"), obj.getJSONObject("name").toString());
			} catch (JSONException e) {
				log.debug("Error reading indicator", e);
			}
		}

		try {
			JSONObject main = JSONGroupingsHelper.createGroupingsJSONObject(
					groupings, themes, data, groupingPermittedUsers,
					groupingPermittedRoles, indicatorNames);
			ResponseHelper.writeResponse(params, main);
		} catch (Exception e) {

			throw new ActionException(
					"Error during creating JSON groupings object");
		}

	}
}
