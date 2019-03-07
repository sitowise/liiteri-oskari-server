package fi.nls.oskari.control.groupings;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.szopa.requests.SzopaRequest;
import fi.nls.oskari.domain.groupings.*;
import fi.nls.oskari.groupings.db.*;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.sito.liiteri.groupings.service.GroupingsService;

@OskariActionRoute("GetGroupings")
public class GetGroupingsHandler extends ActionHandler {

	private static final Logger log = LogFactory
			.getLogger(GetGroupingsHandler.class);
	private static final GroupingServiceIbatisImpl groupingService = new GroupingServiceIbatisImpl();

	private static final GroupingThemeServiceIbatisImpl groupingThemesService = new GroupingThemeServiceIbatisImpl();
	private static final GroupingThemeDataServiceIbatisImpl groupingThemeDataService = new GroupingThemeDataServiceIbatisImpl();
	
	private static final GroupingsService _service = GroupingsService.getInstance();

	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		List<Grouping> groupings = groupingService.findAll();
		List<GroupingTheme> themes = groupingThemesService.findAll();
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
		if ((groupings.size() != 0 || themes.size() != 0) && data.size() == 0) {
			throw new ActionException("Inconsistent groupings data");
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
       	
		/*
		 * try { int y= ThemesRequestHelper.deleteTheme(920); String th =
		 * ThemesRequestHelper.getTheme(460); String c =
		 * ThemesRequestHelper.getThemeWithChildren(460); long theme =
		 * ThemesRequestHelper.writeTheme(null); } catch(Exception ex) { throw
		 * new ActionException( "Error during creating JSON groupings object");
		 * }
		 */

		try {
			JSONObject main = JSONGroupingsHelper.createGroupingsJSONObject(
					groupings, themes, data, groupingPermittedUsers,
					groupingPermittedRoles, indicatorNames);
			ResponseHelper.writeResponse(params, main);
		} catch (Exception e) {

			throw new ActionException(
					"Error during creating JSON groupings object", e);
		}
	}
}