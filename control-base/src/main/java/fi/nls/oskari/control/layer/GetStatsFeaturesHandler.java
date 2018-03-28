package fi.nls.oskari.control.layer;

import java.util.ArrayList;
import java.util.List;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;
import pl.sito.liiteri.arcgis.domain.ArcgisLayer;
import pl.sito.liiteri.map.arcgislayer.service.ArcgisLayerService;

@OskariActionRoute("GetStatsFeatures")
public class GetStatsFeaturesHandler extends ActionHandler {

    private static final Logger log = LogFactory.getLogger(GetStatsFeaturesHandler.class);
    final private static String PARAM_LAYER_ID = "LAYERID";
    final private static String PARAM_LAYER_ATTRIBUTE = "LAYERATTRIBUTE";        
    
    private final String MapServerUrl = PropertyUtil.get("liiteri.statistics.arcgis.url.mapserver");    
    
    private final ArcgisLayerService _service;
    
    public GetStatsFeaturesHandler() {
    	_service = new ArcgisLayerService();
    }

    public void handleAction(final ActionParameters params) throws ActionException {

    	int statsLayerId = params.getHttpParam(PARAM_LAYER_ID, -1);
    	String layerAttribute = params.getHttpParam(PARAM_LAYER_ATTRIBUTE, null);
    	if (statsLayerId == -1) 
    		throw new ActionParamsException("Layer not specified: " + PARAM_LAYER_ID + " parameter missing");
    	if (layerAttribute == null)
    		throw new ActionParamsException("Layer attribute not specified: " + PARAM_LAYER_ATTRIBUTE + " parameter missing");
          
        try 
        {     
        	String features = getFromCache(statsLayerId, layerAttribute);
        	if (features == null) {
        		List<String> fields = new ArrayList<String>();
           		fields.add(layerAttribute);
            	ArcgisLayer layer = new ArcgisLayer(MapServerUrl, statsLayerId);            	
            	features = _service.getFeatures(layer, fields, null);
            	
            	String beforeFeatures = "{\"type\": \"FeatureCollection\",\"features\": "; 
            	String afterFeatures = "}";
            	
            	features = beforeFeatures + features + afterFeatures;
            	
            	if (features != null)
            		saveToCache(statsLayerId, layerAttribute, features);
        	}
        	
        	ResponseHelper.writeResponseAsJson(params, features);
        } 
        catch (Exception e) 
        {
        	log.error(e, "Couldn't get features");
            throw new ActionException("Couldn't get features", e);
        }
    }
    
    public void saveToCache(final int layerId, final String layerAttribute, final String contents) {
    	String key = "ArcgisStatsFeatures_" + layerId + "_" + layerAttribute;
    	JedisManager.setex(key, JedisManager.EXPIRY_TIME_DAY, contents);
    }
    
    public String getFromCache(final int layerId, final String layerAttribute) {    	
    	String key = "ArcgisStatsFeatures_" + layerId + "_" + layerAttribute;    			
    	String result = JedisManager.get(key);
    	return result;
    }
}