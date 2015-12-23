package pl.sito.liiteri.map.arcgislayer.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.sito.liiteri.arcgis.domain.ArcgisLayer;
import pl.sito.liiteri.arcgis.domain.ArcgisMapLayerConfiguration;
import pl.sito.liiteri.arcgis.domain.ArcgisMapLayerLegendConfiguration;
import pl.sito.liiteri.arcgis.domain.ArcgisMapLegendConfiguration;
import pl.sito.liiteri.arcgis.domain.ArcgisMapServerConfiguration;
import pl.sito.liiteri.arcgis.ArcgisToken;
import pl.sito.liiteri.arcgis.ArcgisTokenConfiguration;
import pl.sito.liiteri.arcgis.ArcgisTokenService;
import pl.sito.liiteri.utils.HttpUtils;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorExportToGeoJson;
import com.esri.core.geometry.OperatorImportFromJson;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class ArcgisLayerService
{
	private static final Logger log = LogFactory.getLogger(ArcgisLayerService.class);
	
	private final ArcgisTokenService tokenService;
	private final ArcgisLayerConfigurationService layerConfigurationService;
	private final ArcgisImageService imageService;
	
	
	public ArcgisLayerService() {
		tokenService = ArcgisTokenService.getInstance();
		layerConfigurationService = ArcgisLayerConfigurationService.getInstance();
		imageService = ArcgisImageService.getInstance();
	}

	//TODO: make objects instead of Strings
    public String getFeatures(ArcgisLayer layer, List<String> fields, String filter) throws Exception
    {
    	String features = null;
    	try {
    		int arcgisId = layer.getLayerId();
    		if (arcgisId < 0) {
    			log.error("Cannot get arcgis layer id");
    			return null;
    		}
    		
    		ArcgisToken token = tokenService.getTokenForUrl(layer.getMapServerUrl(), ArcgisTokenConfiguration.createRequestConfiguration());
    		String bbox = null;
    		String objectIdsString = null;    		
    		String whereString = null;
    		String fieldsString = "*";
    		
    		if (filter != null) {
    			JSONObject filterJson = new JSONObject(filter);
    			bbox = MapBBox(filterJson);
    			objectIdsString = MapObjectIdsString(filterJson);
    			whereString = MapWhereString(filterJson);
    		}
    		
    		if (bbox == null)
    			bbox = getExtent(layer, token);    		
    		
    		if (fields != null) {
    			if (fields.size() == 0) {
    				fieldsString = null;
    			}
    			else {
    				fieldsString = StringUtils.join(fields, ',');	
    			}    			
    		}
    		
        	String url = getFeaturesUrl(layer);   	
        	
        	HashMap<String, String> postData = new HashMap<String, String>();
        	postData.put("geometry", bbox);
        	postData.put("geometryType", "esriGeometryEnvelope");
        	postData.put("spatialRel", "esriSpatialRelIntersects");
        	postData.put("returnGeometry", "true");
        	postData.put("returnIdsOnly", "false");
        	postData.put("returnCountOnly", "false");
        	postData.put("returnZ", "false");
        	postData.put("returnM", "false");
        	postData.put("returnDistinctValues", "false");
        	postData.put("outFields", fieldsString);
        	postData.put("objectIds", objectIdsString);
        	postData.put("where", whereString);   
        	postData.put("f", "json");    
        	postData.put("token", token.getToken());
        	
        	String responseString = HttpUtils.sendPost(url, postData);              
        	JSONObject responseJSON = new JSONObject(responseString);        	
        	if (responseJSON.has("error")) {
        		String msg = "Error during retrieving features" + responseString;
    			log.error(msg);
    			throw new Exception(msg);
    		}					    						
        	JSONArray resultArray = ConvertToGeoJSON(responseJSON);        	        	
    		features = resultArray.toString();		
    	}
    	catch (Exception e) {
    		log.error("Cannot get features", e);    
    		throw e;
    	}
    	
    	return features;
    }       
    
    public int getArcgisLayerIdDependingOnScale(ArcgisLayer layer, double scale) {
		int result = layer.getLayerId();			
    	ArcgisMapLayerConfiguration conf = layerConfigurationService.getLayerConfiguration(layer.getMapServerUrl(), layer.getLayerId());
		
		List<Integer> subLayerIds = conf.getSubLayerIds();
		if (subLayerIds != null && subLayerIds.size() > 0) {					
			for (Integer subLayerId : subLayerIds) 
			{
				ArcgisMapLayerConfiguration subLayerConf = layerConfigurationService.getLayerConfiguration(layer.getMapServerUrl(), subLayerId);				
				double minScale = subLayerConf.getMinScale();
				double maxScale = subLayerConf.getMaxScale();
				
				if (maxScale <= scale && scale <= minScale) 
				{
					result = subLayerConf.getId();
					log.info("Setting other arcgis layer id for scale", scale, result);					
					break;
				}
			}
		}
		
		return result;
	}
    
    public List<Integer> getArcgisSubLayersIds(ArcgisLayer layer) {
    	List<Integer> result = new ArrayList<Integer>();
    	ArcgisMapLayerConfiguration conf = layerConfigurationService.getLayerConfiguration(layer.getMapServerUrl(), layer.getLayerId());
		
		List<Integer> subLayerIds = conf.getSubLayerIds();
		if (subLayerIds != null && subLayerIds.size() > 0) {					
			result.addAll(subLayerIds);
		}
		else {
			result.add(layer.getLayerId());
		}
		
		return result;
	}
    
	public byte[] getLegendImage(ArcgisLayer arcgisLayer)
	{
		byte[] result = null;
		int id = arcgisLayer.getLayerId();
		
		List<ArcgisMapLayerLegendConfiguration> legendLayerConfigs = new ArrayList<ArcgisMapLayerLegendConfiguration>();
		
		ArcgisMapServerConfiguration mapServerConfiguration = layerConfigurationService.getMapServerConfiguration(arcgisLayer.getMapServerUrl());						
		ArcgisMapLegendConfiguration legendConfiguration = mapServerConfiguration.getLegendConfiguration();		
		ArcgisMapLayerLegendConfiguration legendLayerConf = legendConfiguration.getConfigurationForLayer(id);	
		
		if (legendLayerConf != null)
			legendLayerConfigs.add(legendLayerConf);
		
		ArcgisMapLayerConfiguration layerConf = mapServerConfiguration.getConfigurationForLayer(id);
		for (Integer subLayerId : layerConf.getSubLayerIds())
		{
			ArcgisMapLayerLegendConfiguration legendSublayerConf = legendConfiguration.getConfigurationForLayer(subLayerId);	
			if (legendSublayerConf != null)
				legendLayerConfigs.add(legendSublayerConf);
		}
				
		result = imageService.createLegendImage(legendLayerConfigs);
		
		return result;
	}	
    
    private String MapBBox(JSONObject filterJson) throws JSONException {
    	String bbox = null;
		if (filterJson.has("bbox")) {
			JSONObject bboxJson = filterJson.getJSONObject("bbox");
			String xmin = bboxJson.getString("left");
			String ymin = bboxJson.getString("bottom");
			String xmax = bboxJson.getString("right");
			String ymax = bboxJson.getString("top");
			
			bbox = xmin + "," + ymin + "," + xmax + "," + ymax;
		}
    	
    	return bbox;
    }
    
    private String MapObjectIdsString(JSONObject filterJson) throws JSONException {
    	String objectIdsString = null;
    	if (filterJson.has("featureIds")) {
    		JSONArray featureIdsArray = filterJson.getJSONArray("featureIds");
    		List<String> featureIds = new ArrayList<String>();
    		for (int i = 0; i < featureIdsArray.length(); i++)
    			featureIds.add(featureIdsArray.getString(i));
    		
    		objectIdsString = StringUtils.join(featureIds, ',');   		
    	}
    	
    	return objectIdsString;
    }
    
    private String MapWhereString(JSONObject filterJson) throws JSONException {
    	String whereString = null;
    	
		if (filterJson.has("filters")) {
			JSONArray filtersJson = filterJson.getJSONArray("filters");
			for (int i = 0; i < filtersJson.length(); i++)
			{
				if (i == 0)
					whereString = "";
				
				JSONObject filterItemJson = filtersJson.getJSONObject(i);
				if (filterItemJson.has("attribute") && filterItemJson.has("operator") && filterItemJson.has("value")) {
					String operator = filterItemJson.getString("operator");
					if (operator.equals("~="))
						operator = "=";
					else if (operator.equals("≠") || operator.equals("~≠"))
						operator = "<>";
					else if (operator.equals("≥"))
						operator = ">=";
					else if (operator.equals("≤"))
						operator = "<=";
					
					whereString += filterItemJson.getString("attribute") + " " + operator + " " + filterItemJson.getString("value");
				}
				else if (filterItemJson.has("boolean")) {
					whereString += " " + filterItemJson.getString("boolean") + " ";
				}
			}
		}
		
		return whereString;				
    }
    
    private JSONArray ConvertToGeoJSON(JSONObject responseJSON) throws JSONException, IOException
	{
		JSONArray featuresArray = responseJSON.getJSONArray("features");
		OperatorExportToGeoJson exportToGeoJson = OperatorExportToGeoJson.local();
		OperatorImportFromJson importFromJson = OperatorImportFromJson.local();
		JSONArray resultArray = new JSONArray();		
		
		for (int i = 0; i < featuresArray.length(); i++)
		{
			JSONObject resultItem = new JSONObject();
			JSONObject featureItem = featuresArray.getJSONObject(i);				
			JSONObject geometryItem = featureItem.getJSONObject("geometry");
			
			resultItem.put("type", "Feature");
			resultItem.put("properties", featureItem.getJSONObject("attributes"));
					
			MapGeometry geometry = importFromJson.execute(Type.Unknown, geometryItem);
			Envelope2D env = new Envelope2D();
			geometry.getGeometry().queryLooseEnvelope2D(env);
			String geoJSON = exportToGeoJson.execute(geometry.getSpatialReference(), geometry.getGeometry());
			resultItem.put("geometry", new JSONObject(geoJSON));
			double bbox[] = new double[]{env.xmin, env.ymin, env.xmax, env.ymax};

			resultItem.put("bbox", new JSONArray(bbox));
			
			resultArray.put(resultItem);
		}
		
		return resultArray;
	}

	private String getExtent(ArcgisLayer layer, ArcgisToken token) throws Exception 
    {
		//TODO: extent from configuration
    	String url = layer.getMetadataUrl();
    	String data = null;
    	url = url + "&token=" + token.getToken();
    			    	
    	String responseString = HttpUtils.sendGet(url);
		
		try
		{		
			JSONObject responseJSON = new JSONObject(responseString);
			JSONObject extent = responseJSON.getJSONObject("extent");
			String xmin = extent.getString("xmin");
			String ymin = extent.getString("ymin");
			String xmax = extent.getString("xmax");
			String ymax = extent.getString("ymax");
			
			data = xmin + "," + ymin + "," + xmax + "," + ymax;
		}  
		catch (JSONException e) 
		{
			log.error(e, "Error getting extent from result");
			throw new Exception("Cannot parse response", e);
		}
		
		return data;
    }       
    
    private String getFeaturesUrl(ArcgisLayer layer) 
    {
    	int id = getArcgisLayerIdDependingOnScale(layer, 1500000);
    	return layer.getMapServerUrl() + "/" + id + "/query?";
    }

      
}
