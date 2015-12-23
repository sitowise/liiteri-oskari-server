package pl.sito.liiteri.map.arcgislayer.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import pl.sito.liiteri.arcgis.ArcgisToken;
import pl.sito.liiteri.arcgis.ArcgisTokenConfiguration;
import pl.sito.liiteri.arcgis.ArcgisTokenService;
import pl.sito.liiteri.arcgis.domain.ArcgisMapLayerConfiguration;
import pl.sito.liiteri.arcgis.domain.ArcgisMapLegendConfiguration;
import pl.sito.liiteri.arcgis.domain.ArcgisMapServerConfiguration;
import pl.sito.liiteri.utils.HttpUtils;

public class ArcgisLayerConfigurationService
{
    private static class ArcgisLayerConfigurationServiceHolder {
        static final ArcgisLayerConfigurationService INSTANCE = new ArcgisLayerConfigurationService();
    }
	
	public static ArcgisLayerConfigurationService getInstance() {
		return ArcgisLayerConfigurationServiceHolder.INSTANCE;
	}
	
	private static final Logger log = LogFactory.getLogger(ArcgisLayerConfigurationService.class);		
	
	private final ArcgisTokenService tokenService;
	
	private ArcgisLayerConfigurationService() 
	{
		tokenService = ArcgisTokenService.getInstance();
	}
	
	public ArcgisMapServerConfiguration getMapServerConfiguration(String mapServerUrl) 
	{
		boolean newValue = false;
		ArcgisMapServerConfiguration result = null;
		
		String json = ArcgisMapServerConfiguration.getCache(mapServerUrl);
		
		if (json == null) {
			json = loadMapServerConfigurationFromServer(mapServerUrl);
			newValue = true;
		}
		
        try 
        {
            result = ArcgisMapServerConfiguration.setJSON(json);                                               
            
            if (newValue) {
            	result.setServerUrl(mapServerUrl);
            	setParentLayersValues(result);            	
            	//loadLayersConfigurationFromServer(result);
            	loadLegendConfigurationFromServer(result);
            	
            	result.save();            
            }            	
        } 
        catch (Exception e) 
        {
            log.error(e, "JSON parsing failed for ArcgisMapServerConfiguration \n" + json);
        }
        
        return result;
	}

	public ArcgisMapLayerConfiguration getLayerConfiguration(String mapServerUrl, int layerId)
	{
		ArcgisMapLayerConfiguration result = null;		
		ArcgisMapServerConfiguration mapServerConfiguration = getMapServerConfiguration(mapServerUrl);
		
		if (mapServerConfiguration != null) 
		{
			result = mapServerConfiguration.getConfigurationForLayer(layerId);
		}	
		
		return result;
	}
	
	private void loadLegendConfigurationFromServer(ArcgisMapServerConfiguration conf) {
		String mapServerUrl = conf.getServerUrl();
		String url = mapServerUrl + "/legend";
		
		ArcgisToken token = tokenService.getTokenForUrl(mapServerUrl, ArcgisTokenConfiguration.createRequestConfiguration());		
		if (token.isEmpty()) {
			log.error("Token is empty");
			return;
		}
		
    	HashMap<String, String> postData = new HashMap<String, String>();
    	postData.put("f", "json");    
    	postData.put("token", token.getToken());    	
		
		String responseString;
		try
		{
			responseString = HttpUtils.sendPost(url, postData);
		} catch (IOException e)
		{
			log.error(e, "Error sending legend request");
			return;
		}
		
		try
		{
			ArcgisMapLegendConfiguration legendConf = ArcgisMapLegendConfiguration.setJSON(responseString);
			conf.setLegendConfiguration(legendConf);
		} catch (IOException e)
		{
			log.error(e, "Cannot parse legend configuration");
		}				
	}

	private void setParentLayersValues(ArcgisMapServerConfiguration conf)
	{
		if (conf == null || conf.getLayers() == null)
			return;
		
		HashMap<Integer, ArcgisMapLayerConfiguration> map = new HashMap<Integer, ArcgisMapLayerConfiguration>();
		
		for (ArcgisMapLayerConfiguration layer : conf.getLayers())
			map.put(layer.getId(), layer);
		
		for (ArcgisMapLayerConfiguration layer : conf.getLayers()) 
		{
			if (layer.getMinScale() != 0 || layer.getMaxScale() != 0)
				continue;
			
			ArrayList<Integer> subLayersIds = layer.getSubLayerIds();
			if (subLayersIds == null || subLayersIds.size() == 0)
				continue;		
			
			for (Integer layerId : subLayersIds)
			{
				ArcgisMapLayerConfiguration subLayerConf = map.get(layerId);
				double minScale = layer.getMinScale();
				double maxScale = layer.getMaxScale();
				
				if (minScale == 0 || minScale < subLayerConf.getMinScale())
					layer.setMinScale(subLayerConf.getMinScale());
				
				if (maxScale == 0 || maxScale > subLayerConf.getMaxScale())
					layer.setMaxScale(subLayerConf.getMaxScale());
			}
		}
	}
	
//	private String loadLayerConfigurationFromServer(String mapServerUrl, String layerId)
//	{
//		ArcgisToken token = tokenService.getTokenForUrl(mapServerUrl, ArcgisTokenConfiguration.createRequestConfiguration());
//		
//		if (token.isEmpty()) {
//			log.error("Token is empty");
//			return null;
//		}
//		
//		String url = mapServerUrl + "/" + layerId;
//		
//    	HashMap<String, String> postData = new HashMap<String, String>();
//    	postData.put("f", "json");    
//    	postData.put("token", token.getToken());
//		
//    	String responseString;
//		try
//		{
//			responseString = HttpUtils.sendPost(url, postData);
//		} catch (IOException e)
//		{
//			log.error(e, "Error during sending Post request");
//			return null;
//		}
//    	
//    	return responseString;
//	}
	
	private String loadMapServerConfigurationFromServer(String mapServerUrl)
	{
		ArcgisToken token = tokenService.getTokenForUrl(mapServerUrl, ArcgisTokenConfiguration.createRequestConfiguration());
		
		if (token.isEmpty()) {
			log.error("Token is empty");
			return null;
		}
		
		String url = mapServerUrl;
		
    	HashMap<String, String> postData = new HashMap<String, String>();
    	postData.put("f", "json");    
    	postData.put("token", token.getToken());
		
    	String responseString;
		try
		{
			responseString = HttpUtils.sendPost(url, postData);
		} catch (IOException e)
		{
			log.error(e, "Error during sending Post request");
			return null;
		}
    	
    	return responseString;
	}
}
