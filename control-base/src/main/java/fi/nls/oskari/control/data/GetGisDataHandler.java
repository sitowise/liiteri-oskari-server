package fi.nls.oskari.control.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.UserGisDataSharing;
import fi.nls.oskari.domain.map.UserWmsLayer;
import fi.nls.oskari.map.layer.UserWmsLayerService;
import fi.nls.oskari.map.layer.UserWmsLayerServiceIbatisImpl;
import fi.nls.oskari.map.userowndata.GisDataDbService;
import fi.nls.oskari.map.userowndata.GisDataDbServiceImpl;
import fi.nls.oskari.map.userowndata.GisDataService;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetGisData")
public class GetGisDataHandler extends ActionHandler {
	
	private static final GisDataDbService gisDataService = new GisDataDbServiceImpl();
    private static final UserWmsLayerService userWmsLayerService = new UserWmsLayerServiceIbatisImpl();
	private GisDataService _service = GisDataService.getInstance();
	
	private static final String CONTENT_TYPE = "type";

	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		List<UserGisData> gisDataList = new ArrayList<UserGisData>();
		List<UserGisDataSharing> sharingList = new ArrayList<UserGisDataSharing>();
		Date expirationDate;
		User user = params.getUser();
		
		if (!user.isGuest()) {
			String contentType = params.getHttpParam(CONTENT_TYPE, "own");
			
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				expirationDate = sdf.parse(sdf.format(new Date()));
			} catch (ParseException e) {
				throw new ActionException("Error during parsing date");
			}
			
			try {
				if (contentType.equals("own")) {
					gisDataList = gisDataService.getGisData(user.getId(), expirationDate);					
					for (UserGisData gisData : gisDataList)
					{
						sharingList.addAll(_service.getUserGisData(gisData.getId()).getUserGisDataSharing());
					}
					List<UserWmsLayer> userWmsLayers = userWmsLayerService
		                    .findForUser(params.getUser().getId());
					for(UserWmsLayer uwl : userWmsLayers) {
					    UserGisData ugd = new UserGisData();
					    ugd.setDataType("USERWMS");
					    ugd.setDataId(UserWmsLayer.PREFIX + uwl.getId());
					    ugd.setUserId(uwl.getUserId());
					    gisDataList.add(ugd);
					}
					
				} else if (contentType.equals("shared")) {
					gisDataList = _service.getExternalGisDataForUser(user, expirationDate);
				}
				
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			JSONArray arr = new JSONArray();
			for (UserGisData u : gisDataList) {
				JSONObject obExt = new JSONObject();
				JSONHelper.putValue(obExt, "id", u.getId());
				JSONHelper.putValue(obExt, "dataId", u.getDataId());
				JSONHelper.putValue(obExt, "dataType", u.getDataType());
				if(u.getExpirationDate() != null) 
				    JSONHelper.putValue(obExt, "expirationDate", new SimpleDateFormat(
						"yyyy-MM-dd").format(u.getExpirationDate()));
				JSONHelper.putValue(obExt, "userId", u.getUserId());
				JSONHelper.putValue(obExt, "status", u.getStatus());
				
				if (contentType.equals("own")) {
					JSONArray arrSharing = new JSONArray();
					
					List<UserGisDataSharing> currentSharing = new ArrayList<UserGisDataSharing>(); 
					for (UserGisDataSharing s : sharingList) {
						if (s.getDatasetId() == u.getId())
							currentSharing.add(s);
					}
					
					for (UserGisDataSharing sh : currentSharing) {
						JSONObject obSharing = new JSONObject();
						JSONHelper.putValue(obSharing, "id", sh.getId());
						JSONHelper.putValue(obSharing, "externalType", sh
								.getExternalType());
						JSONHelper.putValue(obSharing, "externalId",
								sh.getExternalId());
						JSONHelper.putValue(obSharing, "email", sh.getEmail());
						
						arrSharing.put(obSharing);
					}
					JSONHelper.putValue(obExt, "users", arrSharing);
				}
				
				arr.put(obExt);
			}
			//JSONObject ob = new JSONObject();
			//JSONHelper.putValue(ob, "gisDataList", arr);
			
			ResponseHelper.writeResponseAsJson(params, arr);
		}
	}

}
