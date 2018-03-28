package fi.nls.oskari.control.groupings;

import org.json.JSONException;
import org.json.JSONObject;

import pl.sito.liiteri.groupings.service.GroupingsService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.groupings.Grouping;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.groupings.db.GroupingDbService;
import fi.nls.oskari.groupings.db.GroupingServiceIbatisImpl;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("SaveGrouping")
public class SaveGroupingHandler extends ActionHandler {

	private static final Logger log = LogFactory.getLogger(SaveGroupingHandler.class);

	private static final String GROUPING_PARAM = "grouping";
	private static final String GROUPING_TYPE = "type";
	
	private static final GroupingsService _service = GroupingsService.getInstance();
	private GroupingDbService groupingService = new GroupingServiceIbatisImpl();

	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		
		String groupingString = params.getRequiredParam(GROUPING_PARAM);
		String type = params.getRequiredParam(GROUPING_TYPE);		
		User user = params.getUser();
		if (user.isGuest())
			throw new ActionDeniedException("User is not logged");
		
		JSONObject result = new JSONObject();
		if (type.equals("package")) {
			Grouping grouping;
			
			try {
				grouping = JSONGroupingsHelper.buildServicePackageObject(groupingString);
			} catch (JSONException e) {
				throw new ActionException("Error during parsing JSON", e);
			}

			if (grouping.getId() == 0) {
				try {
					long id = _service.addServicePackage(grouping, user);
					grouping.setId(id);
					JSONHelper.putValue(result, "status", "created");
					JSONHelper.putValue(result, "grouping", createGroupingObjectResponse(grouping));
				} catch (ServiceException e) {
					throw new ActionException(
							"Error during saving new grouping to database", e);
				}

			} else {

				try {
					//map state is not sent from UI, but should be preserved here
					grouping.setMapState(groupingService.find((int)grouping.getId()).getMapState());

					_service.updateServicePackage(grouping, user);
					
					JSONHelper.putValue(result, "status", "updated");
					JSONHelper.putValue(result, "grouping", createGroupingObjectResponse(grouping));
				} catch (ServiceException e) {
					throw new ActionException(
							"Error during saving grouping to database", e);
				}

			}
		} else if (type.equals("theme")) {
			GroupingTheme groupingTheme;
			try {
				groupingTheme = JSONGroupingsHelper.buildUnbindedMainThemeObject(groupingString);
			} catch (JSONException e) {
				throw new ActionException("Error during parsing JSON", e);
			}

			if (groupingTheme.getId() == 0) {
				try {				
					long id = _service.addGroupingTheme(groupingTheme, user);					
					
					groupingTheme.setId(id);
					JSONHelper.putValue(result, "status", "created");
					JSONHelper.putValue(result, "grouping", createGroupingObjectResponse(groupingTheme));
				} catch (ServiceException e) {
					throw new ActionException(
							"Error during saving new them to database", e);
				}

			} else {

				try {					
					_service.updateGroupingTheme(groupingTheme, user);
					
					JSONHelper.putValue(result, "status", "updated");
					JSONHelper.putValue(result, "grouping", createGroupingObjectResponse(groupingTheme));
				} catch (ServiceException e) {
					throw new ActionException(
							"Error during saving theme to database", e);
				}

			}

		} else {
			throw new ActionException("Unknown grouping type");
		}
		ResponseHelper.writeResponse(params, result);
	}
	
	private JSONObject createGroupingObjectResponse(Grouping grouping) {
		JSONObject result = new JSONObject();		
		JSONHelper.putValue(result, "id", grouping.getId());
		JSONHelper.putValue(result, "status", grouping.getStatus());
		JSONHelper.putValue(result, "mainType", "package");
		return result;
	}
	
	private JSONObject createGroupingObjectResponse(GroupingTheme grouping) {
		JSONObject result = new JSONObject();		
		JSONHelper.putValue(result, "id", grouping.getId());
		JSONHelper.putValue(result, "status", grouping.getStatus());
		JSONHelper.putValue(result, "mainType", "theme");
		return result;
	}		
}
