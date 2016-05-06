package pl.sito.liiteri.arcgis.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class ArcgisMapLayerLegendConfiguration {	
	
    private static final Logger log = LogFactory.getLogger(ArcgisMapLayerLegendConfiguration.class);
    public static ObjectMapper mapper = new ObjectMapper();    
    
    private static final String LEGEND = "legend";
    private static final String LAYERID = "layerId";    
    
    private List<ArcgisMapLayerLegendItemConfiguration> items = new ArrayList<ArcgisMapLayerLegendItemConfiguration>();
    private int layerId;
    
    @JsonProperty(LEGEND)
	public List<ArcgisMapLayerLegendItemConfiguration> getItems()
	{
		return items;
	}

	public void setItems(List<ArcgisMapLayerLegendItemConfiguration> items)
	{
		this.items = items;
	}	
	
	public int getLayerId()
	{
		return layerId;
	}

	public void setLayerId(int layerId)
	{
		this.layerId = layerId;
	}

	@JsonIgnore
    public String getAsJSON() {
        try {
            return ArcgisMapLayerLegendConfiguration.mapper.writeValueAsString(this);
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
    public static ArcgisMapLayerLegendConfiguration setJSON(JSONObject jsonObj) throws IOException 
    {
    	ArcgisMapLayerLegendConfiguration store = new ArcgisMapLayerLegendConfiguration();    	
    	
    	List<ArcgisMapLayerLegendItemConfiguration> items = new ArrayList<ArcgisMapLayerLegendItemConfiguration>();
    	
    	store.setLayerId(Integer.parseInt(jsonObj.get(LAYERID).toString()));
    	
    	JSONArray array = (JSONArray) jsonObj.get(LEGEND);
    	
    	for (int i = 0; i < array.size(); i++)
		{
			JSONObject jsonItem = (JSONObject) array.get(i);
			ArcgisMapLayerLegendItemConfiguration item = ArcgisMapLayerLegendItemConfiguration.setJSON(jsonItem);
			if(item.getLabel() == null || item.getLabel().isEmpty()) {
			    item.setLabel(jsonObj.get("layerName").toString());
			}
			items.add(item);
		}
        
    	store.setItems(items);

        return store;
    }
    
    @JsonIgnore
    public static ArcgisMapLayerLegendConfiguration setJSON(String json) throws IOException {
    	JSONObject jsonObj = (JSONObject) JSONValue.parse(json);
    	return ArcgisMapLayerLegendConfiguration.setJSON(jsonObj);
    }






    
//    @JsonIgnore
//    public static String getCache(String layerId) {
//        return JedisManager.get(KEY + layerId);
//    }

}