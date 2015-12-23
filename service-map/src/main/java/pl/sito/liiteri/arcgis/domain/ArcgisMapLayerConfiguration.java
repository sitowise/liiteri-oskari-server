package pl.sito.liiteri.arcgis.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class ArcgisMapLayerConfiguration {
    private static final Logger log = LogFactory.getLogger(ArcgisMapLayerConfiguration.class);

    public static ObjectMapper mapper = new ObjectMapper();
    
    public static final String KEY = "ArcgisMapLayer_";
    
    private static final String ERROR = "error";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String SUBLAYER_IDS = "subLayerIds";
    private static final String PARENT_LAYER_ID = "parentLayerId";    
    private static final String MINSCALE = "minScale";
    private static final String MAXSCALE = "maxScale";
    
    private static final String GEOMETRY_TYPE = "geometryType";
    private static final String TYPE = "type";
        
    private int id;        
    private String name;
    private int parentLayerId;        
    private ArrayList<Integer> subLayerIds;
    private double maxScale;
    private double minScale;
    private List<ArcgisMapLayerConfiguration> subLayers;
    
    private String geometryType;
    private String type;
    
//    public List<ArcgisMapLayerConfiguration> getSubLayers() {
//		return subLayers;
//	}
//
//	public void setSubLayers(List<ArcgisMapLayerConfiguration> subLayers) {
//		this.subLayers = subLayers;
//	}

	public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public ArrayList<Integer> getSubLayerIds() {
        return subLayerIds;
    }

    public void setSubLayerIds(ArrayList<Integer> subLayerIds) {
        this.subLayerIds = subLayerIds;
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
	
	public int getParentLayerId()
	{
		return parentLayerId;
	}

	public void setParentLayerId(int parentLayerId)
	{
		this.parentLayerId = parentLayerId;
	}
	
	public String getGeometryType()
	{
		return geometryType;
	}

	public void setGeometryType(String geometryType)
	{
		this.geometryType = geometryType;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}	
    
    @JsonIgnore
    public String getAsJSON() {
        try {
            return ArcgisMapLayerConfiguration.mapper.writeValueAsString(this);
        } catch (JsonGenerationException e) {
            log.error(e, "JSON Generation failed");
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
    public static ArcgisMapLayerConfiguration setJSON(JSONObject jsonObj) throws IOException 
    {
    	ArcgisMapLayerConfiguration store = new ArcgisMapLayerConfiguration();    	
    	
    	ArrayList<Integer> subLayerIds = new ArrayList<Integer>();
    	//ArrayList<ArcgisMapLayerConfiguration> subLayers = new ArrayList<ArcgisMapLayerConfiguration>();
                
        store.setId(Integer.parseInt(jsonObj.get(ID).toString()));
        store.setName(jsonObj.get(NAME).toString());
        store.setParentLayerId(Integer.parseInt(jsonObj.get(PARENT_LAYER_ID).toString()));
        store.setMinScale(Double.parseDouble(jsonObj.get(MINSCALE).toString()));
        store.setMaxScale(Double.parseDouble(jsonObj.get(MAXSCALE).toString()));
        
        store.setGeometryType(jsonObj.get(GEOMETRY_TYPE) != null ? jsonObj.get(GEOMETRY_TYPE).toString() : null);
        store.setType(jsonObj.get(TYPE) != null ? jsonObj.get(TYPE).toString() : null);
        
        JSONArray subLayersArray = (JSONArray) jsonObj.get(SUBLAYER_IDS);
        if (subLayersArray != null) {
            for (Object object : subLayersArray) {
            	int subLayerId = Integer.parseInt(object.toString());
            	subLayerIds.add(subLayerId);
    		}
        }
      
        store.setSubLayerIds(subLayerIds);

        return store;
    }
    
    @JsonIgnore
    public static ArcgisMapLayerConfiguration setJSON(String json) throws IOException {
    	JSONObject jsonObj = (JSONObject) JSONValue.parse(json);
    	return ArcgisMapLayerConfiguration.setJSON(jsonObj);
    }


    
//    @JsonIgnore
//    public static String getCache(String layerId) {
//        return JedisManager.get(KEY + layerId);
//    }

}