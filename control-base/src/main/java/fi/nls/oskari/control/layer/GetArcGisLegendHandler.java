package fi.nls.oskari.control.layer;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.cache.JedisManager;
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

@OskariActionRoute("GetArcGisLegend")
public class GetArcGisLegendHandler extends ActionHandler {

    private static final Logger log = LogFactory.getLogger(GetArcGisLegendHandler.class);
    
    final private static String PARAM_LAYER_ID = "LAYERID";

    private final OskariLayerService service = new OskariLayerServiceIbatisImpl();
    private final ArcgisLayerService arcgisLayerService = new ArcgisLayerService();
    
    private final static String PARAM_LANGUAGE = "lang";      
	private final String LEGEND_IMAGE_KEY = "LegendImage_";

    public void handleAction(final ActionParameters params) throws ActionException 
    {    	
    	final String id = params.getRequiredParam(PARAM_LAYER_ID); 
    	final OskariLayer layer = service.find(id);
    	
    	if (layer == null) {    		
    		String message = String.format("Cannot find layer [%s] in configuration", id);
    		log.error(message);
    		throw new ActionException(message);			
		}        
    	    	
        try 
        {    
    		String key = LEGEND_IMAGE_KEY + id;		
    		byte[] imageBytes = JedisManager.get(key.getBytes());
    		if (imageBytes == null || imageBytes.length == 0) {
            	String arcgisMapServerUrl = ArcgisUtils.getArcgisMapServerUrl(layer);
            	int arcgisId = ArcgisUtils.getArcgisId(layer);
            	
                final ArcgisLayer arcgisLayer = new ArcgisLayer(arcgisMapServerUrl, arcgisId);        	
                imageBytes = arcgisLayerService.getLegendImage(arcgisLayer);  
            	
        		if (imageBytes != null && imageBytes.length > 0)			
        			JedisManager.setex(key.getBytes(), 86400, imageBytes);
    		}
    		
    		ResponseHelper.writeResponseAsImage(params, imageBytes);      	        	       
        } 
        catch (Exception e) 
        {
            throw new ActionException("Couldn't proxy request to ArcGis server", e);
        }
    }    
}