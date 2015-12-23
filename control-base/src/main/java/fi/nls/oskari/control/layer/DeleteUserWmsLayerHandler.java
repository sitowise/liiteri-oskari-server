package fi.nls.oskari.control.layer;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.domain.map.UserWmsLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.UserWmsLayerService;
import fi.nls.oskari.map.layer.UserWmsLayerServiceIbatisImpl;
import fi.nls.oskari.map.layer.formatters.LayerJSONFormatter;
import fi.nls.oskari.util.*;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;

/**
 * Admin insert/update of WMS map layer
 */
@OskariActionRoute("DeleteUserWmsLayer")
public class DeleteUserWmsLayerHandler extends ActionHandler {

    private static final UserWmsLayerService userWmsLayerService = new UserWmsLayerServiceIbatisImpl();

    private static final Logger log = LogFactory.getLogger(DeleteUserWmsLayerHandler.class);
    private static final String PARAM_LAYER_ID = "layer_id";


    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        String layer_id = params.getHttpParam(PARAM_LAYER_ID);
        int layerId = -1;
        if (layer_id != null) {
            layerId = Integer.parseInt(layer_id.split("_")[1]);
            final UserWmsLayer ml = userWmsLayerService.find(layerId);
            
            if(ml == null) {
                throw new ActionException("Couldn't get the saved layer from DB - id:" + layerId);
            }
            
            if(ml.getUserId() == params.getUser().getId()) {
                userWmsLayerService.delete(layerId);
            } else {
                throw new ActionException("No permission to delete layer from DB - id:" + layerId);
            }
            
        }
        
        ResponseHelper.writeResponse(params, layerId);
    }
}
