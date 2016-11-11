package fi.nls.oskari.arcgis.pojo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import fi.nls.oskari.arcgis.ArcGisStyleMapper;
import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.pojo.style.CustomStyleStore;
import fi.nls.oskari.pojo.style.GroupCustomStyleStore;
import fi.nls.oskari.transport.TransportService;
import fi.nls.oskari.wfs.pojo.WFSLayerStore;

public class ArcGisLayerStore extends WFSLayerStore {
    private static final Logger log = LogFactory.getLogger(ArcGisLayerStore.class);

    public static final String KEY = "ArcGisLayer_";
    
    private static final String GROUP_LAYER_TYPE = "Group Layer";

    private static final String ARCGIS_ID = "id";
    private static final String TYPE = "type";
    private static final String SUBLAYERS = "subLayers";
    private static final String SUBLAYER_ID = "id";
    private static final String MINSCALE = "minScale";
    private static final String MAXSCALE = "maxScale";
    private static final String GEOMETRY_TYPE = "geometryType";
    private static final String FIELDS = "fields";
    private static final String NAME = "name";

    private String layerId;
    private String arcGisId;
    private String type;
    private ArrayList<String> subLayerIds;
    private List<ArcGisLayerStore> subLayers;

    private CustomStyleStore defaultStyle;
    private String name;

    public List<ArcGisLayerStore> getSubLayers() {
        return subLayers;
    }

    public void setSubLayers(List<ArcGisLayerStore> subLayers) {
        this.subLayers = subLayers;
    }

    public int getId() {
        return Integer.parseInt(getIdStr());
    }

    public void setId(int id) {
        this.setIdStr(""+id);
    }

    public String getIdStr() {
        return arcGisId;
    }

    public void setIdStr(String id) {
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

    @JsonIgnore
    public static ArcGisLayerStore setJSON(String layerId, String json) throws IOException {
        ArcGisLayerStore store = new ArcGisLayerStore();
        store.layerId = layerId;

        ArrayList<String> subLayerIds = new ArrayList<String>();
        ArrayList<ArcGisLayerStore> subLayers = new ArrayList<ArcGisLayerStore>();

        JSONObject jsonObj = (JSONObject) JSONValue.parse(json);
        store.setIdStr(jsonObj.get(ARCGIS_ID).toString());
        store.setType(jsonObj.get(TYPE).toString());
        store.setMinScale(Double.parseDouble(jsonObj.get(MINSCALE).toString()));
        store.setMaxScale(Double.parseDouble(jsonObj.get(MAXSCALE).toString()));

        if (jsonObj.containsKey(GEOMETRY_TYPE)) {
            Object geometryType = jsonObj.get(GEOMETRY_TYPE);
            store.setGeometryType(geometryType != null ? geometryType.toString() : null);
        }

        if (jsonObj.containsKey(FIELDS)) {
            JSONArray fields = (JSONArray) jsonObj.get(FIELDS);
            List<String> paramNames = new ArrayList<String>();
            if (fields != null) {
                for (int i = 0; i < fields.size(); ++i) {
                    JSONObject field = (JSONObject) fields.get(i);
                    paramNames.add(field.get("name").toString());
                }
            }
            store.addSelectedFeatureParams("default", paramNames);
        }

        if (jsonObj.containsKey("drawingInfo") && jsonObj.get("drawingInfo") != null) {
            JSONObject drawingInfo = (JSONObject) jsonObj.get("drawingInfo");
            CustomStyleStore style = ArcGisStyleMapper.mapRendererToStyle((JSONObject) drawingInfo.get("renderer"));
            store.setDefaultStyle(style);
        } else if (jsonObj.containsKey("defaultStyle") && jsonObj.get("defaultStyle") != null) {
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

    public CustomStyleStore getDefaultStyle() {
        return defaultStyle;
    }

    public void setDefaultStyle(CustomStyleStore defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    public void save() {
        //JedisManager.setex(KEY + this.layerId, 86400, getAsJSON()); // expire in 1 day
    }

    @JsonIgnore
    public static String getCache(String layerId) {
        return JedisManager.get(KEY + layerId);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
