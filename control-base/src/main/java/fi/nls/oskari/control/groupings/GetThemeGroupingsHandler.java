package fi.nls.oskari.control.groupings;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.szopa.requests.SzopaRequest;
import fi.nls.oskari.domain.groupings.*;
import fi.nls.oskari.domain.map.MaplayerGroup;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.groupings.db.*;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.group.link.OskariLayerGroupLink;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.sito.liiteri.groupings.service.GroupingsService;

import fi.nls.oskari.map.layer.OskariLayerServiceIbatisImpl;
import fi.nls.oskari.map.layer.group.link.OskariLayerGroupLinkServiceMybatisImpl;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.mml.map.mapwindow.service.db.OskariMapLayerGroupService;
import fi.mml.map.mapwindow.service.db.OskariMapLayerGroupServiceIbatisImpl;
import fi.nls.oskari.map.layer.group.link.OskariLayerGroupLinkService;


@OskariActionRoute("GetThemeGroupings")
public class GetThemeGroupingsHandler extends ActionHandler {

	private static final Logger log = LogFactory
			.getLogger(GetGroupingsHandler.class);

	private static final GroupingThemeServiceIbatisImpl groupingThemesService = new GroupingThemeServiceIbatisImpl();
	private static final GroupingThemeDataServiceIbatisImpl groupingThemeDataService = new GroupingThemeDataServiceIbatisImpl();
	private static final GroupingServiceIbatisImpl groupingService = new GroupingServiceIbatisImpl();

	private static final GroupingsService _service = GroupingsService.getInstance();

	private static final OskariLayerService layerService = new OskariLayerServiceIbatisImpl();
	private static final OskariMapLayerGroupService groupService = new OskariMapLayerGroupServiceIbatisImpl();
	private static final OskariLayerGroupLinkService linkService = new OskariLayerGroupLinkServiceMybatisImpl();

	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		List<Grouping> groupings = groupingService.findAll();
		List<GroupingThemeData> data = groupingThemeDataService.findAll();
		List<GroupingTheme> statisticsThemes;
		try {
			statisticsThemes = groupingThemesService.getAllStatisticsThemes();
		} catch (ServiceException ex) {
			throw new ActionException(
					"Error during getting statistics themes", ex);
		}

		List<OskariLayer> layers = layerService.findAll();

		Map<Integer, List<MaplayerGroup>> groupsByParentId = groupService.findAll().stream()
				.collect(Collectors.groupingBy(MaplayerGroup::getParentId));

		Map<Integer, List<OskariLayerGroupLink>> linksByGroupId = linkService.findAll().stream()
				.collect(Collectors.groupingBy(OskariLayerGroupLink::getGroupId));


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
		if ((groupings.size() != 0 || statisticsThemes.size() != 0) && data.size() == 0) {
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

		try {
			JSONObject main = JSONGroupingsHelper.createThemeGroupingsJSONObject(
					groupings, statisticsThemes, data, groupingPermittedUsers,
					groupingPermittedRoles, indicatorNames,
					layers, groupsByParentId, linksByGroupId);
			ResponseHelper.writeResponse(params, main);
		} catch (Exception e) {

			throw new ActionException(
					"Error during creating JSON groupings object", e);
		}
	}
}