package fi.nls.oskari.control.layer;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.userlayer.UserLayer;
import fi.nls.oskari.map.userlayer.service.UserLayerDataService;
import fi.nls.oskari.map.userlayer.service.UserLayerDbService;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Returns users user data layers as JSON.
 */
@OskariActionRoute("GetUserLayers")
public class GetUserLayersHandler extends ActionHandler {

    private UserLayerDbService userLayerService;
    private final UserLayerDataService userLayerDataService = new UserLayerDataService();

	private static final String JSKEY_USERLAYERS = "userlayers";
	private static final String USERLAYER_LAYER_PREFIX = "userlayer_";

    public void init() {
        userLayerService = OskariComponentManager.getComponentOfType(UserLayerDbService.class);
    }

	@Override
	public void handleAction(ActionParameters params) throws ActionException {

		final JSONObject response = new JSONObject();
		final JSONArray layers = new JSONArray();
		JSONHelper.putValue(response, JSKEY_USERLAYERS, layers);

		final User user = params.getUser();
		if (!user.isGuest()) {
			
			List<UserGisData> unexpiredUserGisData = userLayerService.getUnexpiredUserLayers(user.getId());
			for (UserGisData u : unexpiredUserGisData) {
				Long id = getIdFromDataIdString(u.getDataId());
				//FIXME: do it better, sending SQL to datbase for every layer is inefficiently
				UserLayer userLayer = userLayerService.getUserLayerById(id);
				
				JSONObject userLayerJson = userLayerDataService.parseUserLayer2JSON(userLayer);
				JSONHelper.putValue(userLayerJson, "shared", "false");
				JSONHelper.putValue(userLayerJson, "downloadServiceUrl", u.getDownloadServiceUrl());
				layers.put(userLayerJson);
			}
			
			List<UserGisData> sharedUserGisData = userLayerService.getSharedUserLayers(user.getId());
			for (UserGisData u : sharedUserGisData) {
				Long id = getIdFromDataIdString(u.getDataId());
				//FIXME: do it better, sending SQL to datbase for every layer is inefficiently
				UserLayer userLayer = userLayerService.getUserLayerById(id);
				
				JSONObject userLayerJson = userLayerDataService.parseUserLayer2JSON(userLayer);
				JSONHelper.putValue(userLayerJson, "shared", "true");
				JSONHelper.putValue(userLayerJson, "downloadServiceUrl", u.getDownloadServiceUrl());
				layers.put(userLayerJson);
			}
			
//			final List<UserLayer> list = userLayerService
//					.getUserLayerByUid(user.getUuid());
//			
//			for (UserLayer a : list) {
//				// Parse userlayer data to userlayer
//
//				layers.put(userlayerService.parseUserLayer2JSON(a));
//			}
		}
		ResponseHelper.writeResponse(params, response);
	}

	private Long getIdFromDataIdString(String dataId) {
		int pos = dataId.lastIndexOf('_');
		return Long.parseLong(dataId.substring(pos + 1));
	}
}
