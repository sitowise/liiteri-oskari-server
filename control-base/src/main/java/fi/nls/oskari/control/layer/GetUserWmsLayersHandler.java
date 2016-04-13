package fi.nls.oskari.control.layer;

import java.util.List;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.UserWmsLayer;
import fi.nls.oskari.domain.map.userlayer.UserLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.UserWmsLayerService;
import fi.nls.oskari.map.layer.UserWmsLayerServiceIbatisImpl;
import fi.nls.oskari.map.layer.formatters.LayerJSONFormatter;
import fi.nls.oskari.map.layer.formatters.LayerJSONFormatterWMS;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Returns users user data layers as JSON.
 */
@OskariActionRoute("GetOwnWmsLayers")
public class GetUserWmsLayersHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(GetUserWmsLayersHandler.class);
    final static String LANGUAGE_ATTRIBUTE = "lang";
    private static final UserWmsLayerService userWmsLayerService = new UserWmsLayerServiceIbatisImpl();
    private final static LayerJSONFormatter FORMATTER = new LayerJSONFormatterWMS();

    @Override
    public void handleAction(ActionParameters params) throws ActionException {

        final JSONObject response = new JSONObject();
        final JSONArray layers = new JSONArray();
        final String lang = params.getHttpParam(LANGUAGE_ATTRIBUTE, params
                .getLocale().getLanguage());
        final User user = params.getUser();
        if (!user.isGuest()) {

            List<UserWmsLayer> userWmsLayers = userWmsLayerService
                    .findForUser(user.getId());
            for (UserWmsLayer u : userWmsLayers) {
                final JSONObject layerJson = FORMATTER.getJSON(u, lang, false);
                if (layerJson != null) {
                    try {
                        long layerId = layerJson.getLong("id");
                        layerJson.remove("id");
                        layerJson.put("id", UserWmsLayer.PREFIX + layerId);
                    } catch (JSONException e) {
                        throw new ActionException("Could not modify layer id", e);
                    }
                    layers.put(layerJson);
                }
            }
        }

        JSONHelper.putValue(response, "data", layers);

        ResponseHelper.writeResponse(params, response);
    }
}
