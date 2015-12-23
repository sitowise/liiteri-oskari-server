package fi.nls.oskari.pojo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import fi.nls.oskari.arcgis.ArcGisStyleMapper;
import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.domain.map.wfs.WFSSLDStyle;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.pojo.style.CustomStyleStore;
import fi.nls.oskari.pojo.style.CustomStyleStoreFactory;
import fi.nls.oskari.pojo.style.GroupCustomStyleStore;
import fi.nls.oskari.pojo.style.UniqueValueCustomStyleStore;
import fi.nls.oskari.transport.TransportService;

public class ArcGisLayerStore {
    private static final Logger log = LogFactory.getLogger(ArcGisLayerStore.class);

    public static final String KEY = "ArcGisLayer_";
    
    private static final String GROUP_LAYER_TYPE = "Group Layer";
    
    private static final String ERROR = "error";
    private static final String ARCGIS_ID = "id";
    private static final String TYPE = "type";
    private static final String SUBLAYERS = "subLayers";
    private static final String SUBLAYER_ID = "id";
    private static final String MINSCALE = "minScale";
    private static final String MAXSCALE = "maxScale";
    private static final String GEOMETRY_TYPE = "geometryType";
    private static final String NAME = "name";
    
    private static final String DRAWING_INFO = "drawingInfo";    
    private static final String RENDERER = "renderer";
    private static final String RENDERER_TYPE = "type";
    private static final String RENDERER_TYPE_UNIQUE = "uniqueValue";
    private static final String RENDERER_UNIQUE_FIELD = "field1";
    private static final String RENDERER_UNIQUE_VALUE_INFOS = "uniqueValueInfos";
    private static final String RENDERER_UNIQUE_VALUE_INFO_VALUE = "value";
    
    
    private String layerId;
    private String arcGisId;
    private String type;
    private ArrayList<String> subLayerIds;
    private double maxScale;
    private double minScale;
    private List<ArcGisLayerStore> subLayers;
    private String geometryType;
    private CustomStyleStore defaultStyle;
    private String name;
    
    public List<ArcGisLayerStore> getSubLayers() {
		return subLayers;
	}

	public void setSubLayers(List<ArcGisLayerStore> subLayers) {
		this.subLayers = subLayers;
	}

	public String getId() {
        return arcGisId;
    }

    public void setId(String id) {
        this.arcGisId = id;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public ArrayList<String> getSubLayerIds() {
        return subLayerIds;
    }

    public void setSubLayerIds(ArrayList<String> subLayerIds) {
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
	
	public String getGeometryType() {
		return geometryType;
	}

	public void setGeometryType(String geometryType) {
		this.geometryType = geometryType;
	}
    
    @JsonIgnore
    public String getAsJSON() {
        try {
            return TransportService.mapper.writeValueAsString(this);
        } catch (JsonGenerationException e) {
            log.error(e, "JSON Generation failed");
        } catch (JsonMappingException e) {
            log.error(e, "Mapping from Object to JSON String failed");
        } catch (IOException e) {
            log.error(e, "IO failed");
        }
        return null;
    }

    /**
     * Saves object to redis
     */
    public void save() {
        JedisManager.setex(KEY + this.layerId, 86400, getAsJSON()); // expire in 1 day
    }
    
    @JsonIgnore
    public static ArcGisLayerStore setJSON(String layerId, String json) throws IOException {
    	ArcGisLayerStore store = new ArcGisLayerStore();
    	store.layerId = layerId;
    	
    	ArrayList<String> subLayerIds = new ArrayList<String>();
    	ArrayList<ArcGisLayerStore> subLayers = new ArrayList<ArcGisLayerStore>();
        
        JSONObject jsonObj = (JSONObject) JSONValue.parse(json);
        store.setId(jsonObj.get(ARCGIS_ID).toString());
        store.setType(jsonObj.get(TYPE).toString());
        store.setName(jsonObj.get(NAME).toString());
        store.setMinScale(Double.parseDouble(jsonObj.get(MINSCALE).toString()));
        store.setMaxScale(Double.parseDouble(jsonObj.get(MAXSCALE).toString()));       
        
        if (jsonObj.containsKey(GEOMETRY_TYPE)) {
        	Object geometryType = jsonObj.get(GEOMETRY_TYPE);
        	store.setGeometryType(geometryType != null ? geometryType.toString() : null);
        }
        
        if (jsonObj.containsKey("drawingInfo") && jsonObj.get("drawingInfo") != null) {
            JSONObject drawingInfo = (JSONObject)jsonObj.get("drawingInfo");         
            CustomStyleStore style = ArcGisStyleMapper.mapRendererToStyle((JSONObject) drawingInfo.get("renderer"));
            store.setDefaultStyle(style);
        }
        else if (jsonObj.containsKey("defaultStyle") && jsonObj.get("defaultStyle") != null) {        	
        	CustomStyleStore style = CustomStyleStore.setJSON(jsonObj.get("defaultStyle").toString());        		
        	store.setDefaultStyle(style);
        }
        	
        
        JSONArray subLayersArray = (JSONArray) jsonObj.get(SUBLAYERS);
        for (Object object : subLayersArray) {
        	JSONObject subLayerObj = (JSONObject) object;
        	subLayerIds.add(subLayerObj.get(SUBLAYER_ID).toString());	
        	if (subLayerObj.containsKey(TYPE)) {
        		// loaded from cache
        		ArcGisLayerStore subLayer = ArcGisLayerStore.setJSON(layerId, subLayerObj.toJSONString());
        		if (subLayer != null) {
        			subLayers.add(subLayer);
        		}
        	}
		}        
        store.setSubLayerIds(subLayerIds);
        store.setSubLayers(subLayers);

        return store;
    }
    
    @JsonIgnore
    public static String getCache(String layerId) {
        return JedisManager.get(KEY + layerId);
    }
    
    @JsonIgnore
    public boolean hasDefaultStyle() {
    	if (this.defaultStyle != null)
    		return true;
    	
    	if (this.getType().equals(GROUP_LAYER_TYPE)) {
    		boolean subLayerHasDefaultStyle = false;
    		for (ArcGisLayerStore subLayer : this.getSubLayers()) {
    			subLayerHasDefaultStyle = subLayerHasDefaultStyle || subLayer.hasDefaultStyle();
			}
    		
    		return subLayerHasDefaultStyle;
    	}
    	
    	return false;
    }
    
    @JsonIgnore
    public CustomStyleStore getAggregatedDefaultStyle() {
    	if (!this.getType().equals(GROUP_LAYER_TYPE))
    		return this.getDefaultStyle();
    	
    	GroupCustomStyleStore style = new GroupCustomStyleStore();
    	for (ArcGisLayerStore subLayer : this.getSubLayers()) {
    		if (subLayer.getDefaultStyle() != null)
    			style.addSubStyle(subLayer.getName(), subLayer.getDefaultStyle());
		}
    	return style;
    }

	public CustomStyleStore getDefaultStyle() {
		return defaultStyle;
	}

	public void setDefaultStyle(CustomStyleStore defaultStyle) {
		this.defaultStyle = defaultStyle;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}




}
