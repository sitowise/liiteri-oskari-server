package fi.nls.oskari.control.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.workspaces.JSONWorkSpacesHelper;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.UserGisDataSharing;
import fi.nls.oskari.map.userowndata.GisDataDbService;
import fi.nls.oskari.map.userowndata.GisDataDbServiceImpl;
import fi.nls.oskari.map.userowndata.GisDataRoleSettingsDbService;
import fi.nls.oskari.map.userowndata.GisDataRoleSettingsDbServiceImpl;
import fi.nls.oskari.map.userowndata.GisDataService;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;
import fi.nls.oskari.workspaces.service.WorkspaceService;

@OskariActionRoute("SaveGisData")
public class SaveGisDataHandler extends ActionHandler {
	
	private GisDataService _service;
	
	private static final String PARAM_ID = "id";
	private static final String PARAM_DATA_ID = "dataId";
	private static final String PARAM_DATA_TYPE = "dataType";
	private static final String PARAM_DATA_USERS = "users";
	
    public void setService(final GisDataService service) {
    	this._service = service;
    }
	
    @Override
    public void init() {
        super.init();
        if(_service == null) {
        	setService(GisDataService.getInstance());
        }
    }

	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		
		String usersString = params.getRequiredParam(PARAM_DATA_USERS);		
		User user = params.getUser();
		if (user.isGuest())
			throw new ActionDeniedException("User is not logged");
		
		String outputMessage;
		Long insertId = null;
		
		long id = 0;
		String idString = params.getHttpParam(PARAM_ID);
		if (idString != null && !idString.isEmpty()) {
			id = Long.parseLong(idString);
		}		

		List<UserGisDataSharing> sharingList;
		try {
			sharingList = getUserGisDataSharingObjectList(usersString);
		} catch (JSONException e1) {
			throw new ActionException("Error during parsing of shared users");
		}

		if (id == 0) {
			try {
				insertId = _service.insertUserGisData(user, params.getHttpParam(PARAM_DATA_ID), params.getHttpParam(PARAM_DATA_TYPE), sharingList);					
				//outputMessage = "GIS data has been created";
				outputMessage = "Aineisto luotiin onnistuneesti";

			} catch (Exception e) {

				//String errMess = "Error during saving GIS data to database.";
				String errMess = "Virhe aineiston tallennuksessa";
				//if (!e.getMessage().isEmpty()) {
				//	errMess += " " + e.getMessage();
				//}
				throw new ActionException(errMess);
			}

		} else {

			try {
				_service.updateUserGisData(id, user, params.getHttpParam(PARAM_DATA_ID), params.getHttpParam(PARAM_DATA_TYPE), sharingList);	
				//outputMessage = "GIS data has been updated";
				outputMessage = "Aineisto päivitettiin onnistuneesti";
			} catch (Exception e) {
				//String errMess = "Error during saving GIS data to database.";
				String errMess = "Virhe aineiston tallennuksessa";
				//if (!e.getMessage().isEmpty()) {
				//	errMess += " " + e.getMessage();
				//}
				throw new ActionException(errMess, e);
			}

		}

		ResponseHelper.writeResponse(params, JSONWorkSpacesHelper
				.createJSONMessageObject(insertId, outputMessage));
	}
	
	private List<UserGisDataSharing> getUserGisDataSharingObjectList(String json)
			throws JSONException {
		List<UserGisDataSharing> sharingList = new ArrayList<UserGisDataSharing>();
		if (json != null &&  !json.isEmpty()) {
			JSONArray sharing = JSONHelper.createJSONArray(json);
			if (sharing != null) {
				for (int i = 0; i < sharing.length(); i++) {
					sharingList.add(buildWorkSapceSharing(sharing
							.getJSONObject(i)));
				}
			}
		}
		return sharingList;
	}
	
	private UserGisDataSharing buildWorkSapceSharing(JSONObject sharing)
			throws JSONException {
		UserGisDataSharing s = new UserGisDataSharing();
		
		if (sharing.has("externalId")) {
			s.setExternalId(sharing.getLong("externalId"));
		} else {
			//in this case there is given only email address (user ID is unknown yet)
			s.setExternalId(0);
		}
		
		s.setEmail(sharing.getString("email"));
		s.setExternalType(sharing.getString("externalType"));
		s.setId(sharing.optLong("id", 0));
		
		return s;
	}

}
