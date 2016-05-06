package pl.sito.liiteri.arcgis.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class ArcgisMapServerConfiguration {
    private static final Logger log = LogFactory.getLogger(ArcgisMapServerConfiguration.class);

    public static ObjectMapper mapper = new ObjectMapper();
    
    public static final String KEY = "ArcgisMapServer_";

    private static final String MINSCALE = "minScale";
    private static final String MAXSCALE = "maxScale";    
    private static final String LAYERS = "layers";    
    private static final String SUPPORTS_DYNAMIC_LAYERS = "supportsDynamicLayers";
    private static final String LEGEND_CONF = "legendConfiguration";
    
    private static final String SERVER_URL = "serverUrl";
    
    private boolean supportsDynamicLayers;
    private double maxScale;
    private double minScale;
    private String serverUrl;
    private List<ArcgisMapLayerConfiguration> layers;    
    private ArcgisMapLegendConfiguration legendConfiguration;
    
    public String getServerUrl()
	{
		return serverUrl;
	}

	public void setServerUrl(String serverUrl)
	{
		this.serverUrl = serverUrl;
	}

	public List<ArcgisMapLayerConfiguration> getLayers() {
		return layers;
	}

	public void setLayers(List<ArcgisMapLayerConfiguration> layers) {
		this.layers = layers;
	}
	
	public boolean isSupportsDynamicLayers()
	{
		return supportsDynamicLayers;
	}

	public void setSupportsDynamicLayers(boolean supportsDynamicLayers)
	{
		this.supportsDynamicLayers = supportsDynamicLayers;
	}
    
	public double getMinScale() {
		return minScale;
	}

	public void setMinScale(double minScale) {
		this.minScale = minScale;
	}

	public double getMaxScale() {
		return maxScale;
	}

	public void setMaxScale(double maxScale) {
		this.maxScale = maxScale;
	}
	
	public void setLegendConfiguration(ArcgisMapLegendConfiguration legendConf) {
		this.legendConfiguration = legendConf;		
	}
	
	public ArcgisMapLegendConfiguration getLegendConfiguration() {
		return this.legendConfiguration;
	}
	
	@JsonIgnore
	public ArcgisMapLayerConfiguration getConfigurationForLayer(int layerId) {
		ArcgisMapLayerConfiguration result = null;
		
		if (layers != null) {
			for (ArcgisMapLayerConfiguration item : layers)
			{
				if (item.getId() == layerId)
				{
					result = item;
					break;
				}
			}
		}
		
		return result;
	}
    
    @JsonIgnore
    public String getAsJSON() {
        try {
            return ArcgisMapServerConfiguration.mapper.writeValueAsString(this);
        } catch (JsonMappingException e) {
            log.error(e, "Mapping from Object to JSON String failed");
        } catch (IOException e) {
            log.error(e, "IO failed");
        }
        return null;
    }

    public void save() {
        JedisManager.setex(KEY + this.serverUrl, 86400, getAsJSON()); // expire in 1 day
    }
    
    @JsonIgnore
    public static ArcgisMapServerConfiguration setJSON(String json) throws IOException {
    	ArcgisMapServerConfiguration store = new ArcgisMapServerConfiguration();        	
    	
    	ArrayList<ArcgisMapLayerConfiguration> layers = new ArrayList<ArcgisMapLayerConfiguration>();
        
        JSONObject jsonObj = (JSONObject) JSONValue.parse(json);
        store.setSupportsDynamicLayers(Boolean.parseBoolean(jsonObj.get(SUPPORTS_DYNAMIC_LAYERS).toString()));                
        store.setMinScale(Double.parseDouble(jsonObj.get(MINSCALE).toString()));
        store.setMaxScale(Double.parseDouble(jsonObj.get(MAXSCALE).toString()));
        
        JSONArray layersArray = (JSONArray) jsonObj.get(LAYERS);
        for (Object object : layersArray) 
        {
        	JSONObject layerObj = (JSONObject) object;
        	ArcgisMapLayerConfiguration layer = ArcgisMapLayerConfiguration.setJSON(layerObj);
        	layers.add(layer);
		}
        
        store.setLayers(layers);
        
        /* optional */
        if (jsonObj.containsKey(SERVER_URL)) {
        	store.setServerUrl(jsonObj.get(SERVER_URL).toString());
        }
        
        if (jsonObj.containsKey(LEGEND_CONF)) {
        	ArcgisMapLegendConfiguration legendConf = ArcgisMapLegendConfiguration.setJSON((JSONObject) jsonObj.get(LEGEND_CONF));
        	store.setLegendConfiguration(legendConf);
        }

        return store;
    }
    
    @JsonIgnore
    public static String getCache(String serverUrl) {
        return JedisManager.get(KEY + serverUrl);
    }

}