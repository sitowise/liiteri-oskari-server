package fi.nls.oskari.control.layer;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceIbatisImpl;
import fi.nls.oskari.util.ResponseHelper;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import pl.sito.liiteri.arcgis.domain.ArcgisLayer;
import pl.sito.liiteri.map.arcgislayer.service.ArcgisLayerService;
import pl.sito.liiteri.utils.ArcgisUtils;

@OskariActionRoute("GetArcGisFeatures")
public class GetArcGisFeaturesHandler extends ActionHandler {

    private static final Logger log = LogFactory.getLogger(GetArcGisFeaturesHandler.class);
    
    final private static String PARAM_LAYER_ID = "LAYERID";
    final private static String PARAM_FILTER = "FILTER";

    private final OskariLayerService service = new OskariLayerServiceIbatisImpl();
    private final ArcgisLayerService arcgisLayerService = new ArcgisLayerService();
    
    final public static String PARAM_LANGUAGE = "lang";  

    public void handleAction(final ActionParameters params) throws ActionException 
    {    	
    	final String id = params.getRequiredParam(PARAM_LAYER_ID);
    	final String filter = params.getHttpParam(PARAM_FILTER, null);
    	final OskariLayer layer = service.find(id);
    	
        try {        	               
        	
            if (layer == null) {
    			log.error("Cannot find layer [%s] in configuration", id);
    		}        
            
            int arcgisId = ArcgisUtils.getArcgisId(layer);
            if (arcgisId < 0) {
    			log.error("Cannot find arcgis layer id in OskariLayer", id);
    		}
            
            String arcgisMapServerUrl = ArcgisUtils.getArcgisMapServerUrl(layer);
            
            final ArcgisLayer arcgisLayer = new ArcgisLayer(arcgisMapServerUrl, arcgisId);
        	
        	String features = arcgisLayerService.getFeatures(arcgisLayer, null, filter);
        	
        	JSONObject json = new JSONObject();
        	json.put("type", "FeatureCollection");
        	json.put("features", new JSONArray(features));
        	
        	String data = json.toString(1);
        	
        	final HttpServletResponse response = params.getResponse(); 
        	
        	response.setContentType("application/json");
        	
        	ResponseHelper.writeResponse(params, data);
        	

        } catch (Exception e) {
            throw new ActionException("Couldn't proxy request to ArcGis server",
                    e);
        }
    }    
}