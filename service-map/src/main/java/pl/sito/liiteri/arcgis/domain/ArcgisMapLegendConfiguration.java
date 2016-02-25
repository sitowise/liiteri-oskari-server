package pl.sito.liiteri.arcgis.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class ArcgisMapLegendConfiguration {
    private static final Logger log = LogFactory.getLogger(ArcgisMapLegendConfiguration.class);

    public static ObjectMapper mapper = new ObjectMapper();
    
    public static final String KEY = "ArcgisMapLayer_";

    private static final String LAYERS = "layers";
           
    private List<ArcgisMapLayerLegendConfiguration> items = new ArrayList<ArcgisMapLayerLegendConfiguration>();
    
    @JsonProperty(LAYERS)
	public List<ArcgisMapLayerLegendConfiguration> getItems()
	{
		return items;
	}

	public void setItems(List<ArcgisMapLayerLegendConfiguration> items)
	{
		this.items = items;
	}
	
	@JsonIgnore
	public ArcgisMapLayerLegendConfiguration getConfigurationForLayer(int layerId) {
		ArcgisMapLayerLegendConfiguration result = null;
		
		if (items != null) {
			for (ArcgisMapLayerLegendConfiguration item : items)
			{
				if (item.getLayerId() == layerId)
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
            return ArcgisMapLegendConfiguration.mapper.writeValueAsString(this);
        } catch (JsonMappingException e) {
            log.error(e, "Mapping from Object to JSON String failed");
        } catch (IOException e) {
            log.error(e, "IO failed");
        }
        return null;
    }

//    /**
//     * Saves object to redis
//     */
//    public void save() {
//        JedisManager.setex(KEY + this.layerId, 86400, getAsJSON()); // expire in 1 day
//    }
    
    @JsonIgnore
    public static ArcgisMapLegendConfiguration setJSON(JSONObject jsonObj) throws IOException 
    {
    	ArcgisMapLegendConfiguration store = new ArcgisMapLegendConfiguration();    	    	
    	List<ArcgisMapLayerLegendConfiguration> items = new ArrayList<ArcgisMapLayerLegendConfiguration>();
    	JSONArray array = (JSONArray) jsonObj.get(LAYERS);
    	
    	for (int i = 0; i < array.size(); i++)
		{
			JSONObject jsonItem = (JSONObject) array.get(i);
			ArcgisMapLayerLegendConfiguration item = ArcgisMapLayerLegendConfiguration.setJSON(jsonItem);
			items.add(item);
		}        
    	store.setItems(items);
    	
        return store;
    }
    
    @JsonIgnore
    public static ArcgisMapLegendConfiguration setJSON(String json) throws IOException {
    	JSONObject jsonObj = (JSONObject) JSONValue.parse(json);
    	return ArcgisMapLegendConfiguration.setJSON(jsonObj);
    }

    
//    @JsonIgnore
//    public static String getCache(String layerId) {
//        return JedisManager.get(KEY + layerId);
//    }

}