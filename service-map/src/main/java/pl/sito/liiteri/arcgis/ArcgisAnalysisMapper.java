package pl.sito.liiteri.arcgis;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import pl.sito.liiteri.arcgis.domain.ArcgisLayer;
import pl.sito.liiteri.map.arcgislayer.service.ArcgisLayerService;
import pl.sito.liiteri.utils.ArcgisUtils;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceIbatisImpl;
import fi.nls.oskari.util.JSONHelper;

public class ArcgisAnalysisMapper
{
	private static final Logger log = LogFactory.getLogger(ArcgisAnalysisMapper.class);
	private OskariLayerService layerService = new OskariLayerServiceIbatisImpl();
	private ArcgisLayerService arcgisLayerService = new ArcgisLayerService();
	
	private static final String JSON_KEY_FEATURES = "features";
	private static final String JSON_KEY_LAYERID = "layerId";
	private static final String JSON_KEY_FIELDS = "fields";
	
	public boolean isArcgisAnalysis(String configuration) 
	{
		JSONObject json = JSONHelper.createJSONObject(configuration);
		String layerType = json.optString("layerType", null);
		return "arcgis".equals(layerType);
	}
	
	public String MapArcgisAnalysisConfiguration(String configuration, String filter) throws Exception
	{
		JSONObject json = JSONHelper.createJSONObject(configuration);
		String layerId = json.optString(JSON_KEY_LAYERID, null);
		
		if (layerId == null) {
			log.error("Cannot find layerId in configuration");
			return configuration;
		}			
		        
        OskariLayer layer = layerService.find(layerId);
        
        if (layer == null) {
			log.error("Cannot find layer [%s] in configuration", layerId);
			return configuration;
		}        
        
        int arcgisId = ArcgisUtils.getArcgisId(layer);
        if (arcgisId < 0) {
			log.error("Cannot find arcgis layer id in OskariLayer", layerId);
			return configuration;
		}  
        
        String arcgisMapServerUrl = ArcgisUtils.getArcgisMapServerUrl(layer);
        
        ArcgisLayer arcgisLayer = new ArcgisLayer(arcgisMapServerUrl, arcgisId);
        
        List<String> fields = null;        
        JSONArray fieldsArray = json.optJSONArray(JSON_KEY_FIELDS);
        if (fieldsArray != null) {
        	fields = new ArrayList<String>();
        	for (int i = 0; i < fieldsArray.length(); i++)
				fields.add(fieldsArray.getString(i));
        }
        
        String featuresString = arcgisLayerService.getFeatures(arcgisLayer, fields, filter);
        
        JSONArray array = JSONHelper.createJSONArray(featuresString);             
        JSONHelper.putValue(json, JSON_KEY_FEATURES, array);
        
        return JSONHelper.getStringFromJSON(json, null);
	}	
}
