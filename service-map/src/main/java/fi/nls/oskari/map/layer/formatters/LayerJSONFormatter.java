package fi.nls.oskari.map.layer.formatters;

import fi.mml.map.mapwindow.service.db.InspireThemeService;
import fi.mml.map.mapwindow.service.db.InspireThemeServiceIbatisImpl;
import fi.nls.oskari.domain.map.InspireTheme;
import fi.nls.oskari.domain.map.LayerGroup;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.LayerGroupService;
import fi.nls.oskari.map.layer.LayerGroupServiceIbatisImpl;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: SMAKINEN
 * Date: 17.12.2013
 * Time: 15:37
 * To change this template use File | Settings | File Templates.
 */
public class LayerJSONFormatter {

    public static final String PROPERTY_AJAXURL = "oskari.ajax.url.prefix";
    public static final String KEY_STYLES = "styles";

    private static final InspireThemeService inspireThemeService = new InspireThemeServiceIbatisImpl();
    private static final LayerGroupService groupService = new LayerGroupServiceIbatisImpl();

    private static final String KEY_ID = "id";
    private static final String KEY_TYPE = "type";
    private static final String KEY_ADMIN = "admin";
    protected static final String[] STYLE_KEYS ={"name", "title", "legend"};

    private static Logger log = LogFactory.getLogger(LayerJSONFormatter.class);
    // map different layer types for JSON formatting
    private static Map<String, LayerJSONFormatter> typeMapping = new HashMap<String, LayerJSONFormatter>();
    static {
        typeMapping.put(OskariLayer.TYPE_WMS, new LayerJSONFormatterWMS());
        typeMapping.put(OskariLayer.TYPE_WFS, new LayerJSONFormatterWFS());
        typeMapping.put(OskariLayer.TYPE_WMTS, new LayerJSONFormatterWMTS());
        typeMapping.put(OskariLayer.TYPE_STATS, new LayerJSONFormatterStats());
    }

    private static LayerJSONFormatter getFormatter(final String type) {
        if(type == null) {
            return null;
        }
        final LayerJSONFormatter formatter = typeMapping.get(type);
        if(formatter != null) {
            return formatter;
        }
        return null;
    }

    /**
     * Use to add custom layer JSON formatter for custom layer type
     * @param type
     * @param formatter
     */
    public static void addFormatter(final String type, final LayerJSONFormatter formatter) {
        typeMapping.put(type, formatter);
    }

    public JSONObject getJSON(final OskariLayer layer,
                                     final String lang,
                                     final boolean isSecure) {
        LayerJSONFormatter formatter = getFormatter(layer.getType());
        // to prevent nullpointer and infinite loop
        if(formatter != null && !formatter.getClass().equals(LayerJSONFormatter.class)) {
            return formatter.getJSON(layer, lang, isSecure);
        }
        return getBaseJSON(layer, lang, isSecure);
    }

    public JSONObject getBaseJSON(final OskariLayer layer,
                                     final String lang,
                                     final boolean isSecure) {
        JSONObject layerJson = new JSONObject();

        final String externalId = layer.getExternalId();
        if(externalId != null && !externalId.isEmpty()) {
            JSONHelper.putValue(layerJson, KEY_ID, externalId);
        }
        else {
            JSONHelper.putValue(layerJson, KEY_ID, layer.getId());
        }

        //log.debug("Type", layer.getType());
        if(layer.isCollection()) {
            // fixing frontend type for collection layers
            if(layer.isBaseMap()) {
                JSONHelper.putValue(layerJson, KEY_TYPE, "base");
            }
            else {
                JSONHelper.putValue(layerJson, KEY_TYPE, "groupMap");
            }
        }
        else {
            JSONHelper.putValue(layerJson, KEY_TYPE, layer.getType());
            //log.debug("wmsName", layer.getName());
            // for easier proxy routing on ssl hosts, maps all urls with prefix and a simplified url
            // so tiles can be fetched from same host from browsers p.o.v. and the actual url
            // is proxied with a proxy for example: /proxythis/<actual wmsurl>
            JSONHelper.putValue(layerJson, "url", layer.getUrl(isSecure));
            JSONHelper.putValue(layerJson, "layerName", layer.getName());
            if (useProxy(layer)) {
                JSONHelper.putValue(layerJson, "url", getProxyUrl(layer));
            }
        }

        JSONHelper.putValue(layerJson, "name", layer.getName(lang));
        JSONHelper.putValue(layerJson, "subtitle", layer.getTitle(lang));
        if(layer.getGroup() != null) {
            JSONHelper.putValue(layerJson, "orgName", layer.getGroup().getName(lang));
        }
        if(layer.getInspireTheme() != null) {
            JSONHelper.putValue(layerJson, "inspire", layer.getInspireTheme().getName(lang));
        }

        if(layer.getOpacity() != null && layer.getOpacity() > -1 && layer.getOpacity() <= 100) {
            JSONHelper.putValue(layerJson, "opacity", layer.getOpacity());
        }
        if(layer.getMinScale() != null && layer.getMinScale() != -1) {
            JSONHelper.putValue(layerJson, "minScale", layer.getMinScale());
        }
        if(layer.getMaxScale() != null && layer.getMaxScale() != -1) {
            JSONHelper.putValue(layerJson, "maxScale", layer.getMaxScale());
        }
        JSONHelper.putValue(layerJson, "geom", layer.getGeometry());

        JSONHelper.putValue(layerJson, "params", layer.getParams());
        JSONHelper.putValue(layerJson, "options", layer.getOptions());
        JSONHelper.putValue(layerJson, "attributes", layer.getAttributes());

        JSONHelper.putValue(layerJson, "realtime", layer.getRealtime());
        JSONHelper.putValue(layerJson, "refreshRate", layer.getRefreshRate());

        JSONHelper.putValue(layerJson, "srs_name", layer.getSrs_name());
        JSONHelper.putValue(layerJson, "version", layer.getVersion());

        JSONHelper.putValue(layerJson, "legendImage", layer.getLegendImage());
        JSONHelper.putValue(layerJson, "baseLayerId", layer.getParentId());

        JSONHelper.putValue(layerJson, "created", layer.getCreated());
        JSONHelper.putValue(layerJson, "updated", layer.getUpdated());
        
        JSONHelper.putValue(layerJson, "downloadServiceUrl", layer.getDownloadServiceUrl());
        JSONHelper.putValue(layerJson, "copyrightInfo", layer.getCopyrightInfo());

        JSONHelper.putValue(layerJson, "dataUrl_uuid", getFixedDataUrl(layer));

        // sublayer handling
        if(layer.getSublayers() != null && !layer.getSublayers().isEmpty()) {
            JSONArray sublayers = new JSONArray();
            for(OskariLayer sub : layer.getSublayers()) {
                JSONObject subJSON = getJSON(sub, lang, isSecure);
                sublayers.put(subJSON);
            }
            JSONHelper.putValue(layerJson, "subLayer", sublayers);
        }
        return layerJson;
    }
    public void removeAdminInfo(final JSONObject layer) {
        if(layer == null) {
            return;
        }
        layer.remove(KEY_ADMIN);
    }

    public void addInfoForAdmin(final JSONObject layer, final String key, final Object value) {
        if(layer == null) {
            return;
        }
        // ensure we have the admin block in place
        JSONObject additionalData = layer.optJSONObject(KEY_ADMIN);
        if(additionalData == null) {
            additionalData = new JSONObject();
            JSONHelper.putValue(layer, KEY_ADMIN, additionalData);
        }
        JSONHelper.putValue(additionalData, key, value);
    }

    protected boolean useProxy(final OskariLayer layer) {
        boolean forceProxy = false;
        boolean mixedContent = false;
        if (layer.getAttributes() != null) {
            forceProxy = layer.getAttributes().optBoolean("forceProxy", false);
        }
        if (PropertyUtil.get("oskari.domain", "").startsWith("https://") && layer.getUrl().startsWith("http://")) {
            mixedContent = true;
        }
        return ((layer.getUsername() != null) && (layer.getUsername().length() > 0)) || forceProxy || mixedContent;
    }

    public String getProxyUrl(final OskariLayer layer) {
        Map<String, String> urlParams = new HashMap<String, String>();
        urlParams.put("action_route", "GetLayerTile");
        urlParams.put(KEY_ID, Integer.toString(layer.getId()));
        return IOHelper.constructUrl(PropertyUtil.get(PROPERTY_AJAXURL), urlParams);
    }

    protected boolean isBeingProxiedViaOskariServer(String url) {
        if(url == null || url.isEmpty()) {
            return false;
        }
        return url.startsWith(PropertyUtil.get(PROPERTY_AJAXURL));
    }

    public static JSONObject createStylesJSON(String name, String title, String legend) {
        final JSONObject style = JSONHelper.createJSONObject(STYLE_KEYS[0], name);
        JSONHelper.putValue(style, STYLE_KEYS[1], title);
        JSONHelper.putValue(style, STYLE_KEYS[2], legend);
        return style;
    }

    // This is solution of transition for dataUrl and for dataUrl_uuid
    private String getFixedDataUrl(final OskariLayer layer) {
        final String metadataId = layer.getMetadataId();
        if(metadataId == null || metadataId.isEmpty()) {
            return null;
        }       
        
/*        if(metadataId.toLowerCase().startsWith("http")) {
            try {
                URL url = new URL(metadataId);

                String[] parameters = url.getQuery().split("&");
                for (String param : parameters) {
                    String[] keyvalue = param.split("=");
                    if("uuid".equalsIgnoreCase(keyvalue[0]) || KEY_ID.equalsIgnoreCase(keyvalue[0])) {
                        return keyvalue[1];
                    }
                }
            } catch (Exception ignored) {
                // propably just not valid URL
            }
            log.debug("Couldn't parse uuid from metadata url:", metadataId);
            return null;
        }*/
        return metadataId;
    }

    /**
     * Minimal implementation for parsing layer in json format.
     * @param json
     * @return
     */
    public OskariLayer parseLayer (final JSONObject json) throws JSONException {
        OskariLayer layer = new OskariLayer();

        // read mandatory values, an JSONException is thrown if these are missing
        layer.setType(json.getString("type"));
        layer.setUrl(json.getString("url"));
        layer.setName(json.getString("name"));
        final String orgName = json.getString("organization");
        final String themeName = json.getString("inspiretheme");
        layer.setLocale(json.getJSONObject("locale"));

        // read optional values
        layer.setBaseMap(json.optBoolean("base_map", layer.isBaseMap()));
        layer.setOpacity(json.optInt("opacity", layer.getOpacity()));
        layer.setStyle(json.optString("style", layer.getStyle()));
        layer.setMinScale(json.optDouble("minscale", layer.getMinScale()));
        layer.setMaxScale(json.optDouble("maxscale", layer.getMaxScale()));
        layer.setLegendImage(json.optString("legend_image", layer.getLegendImage()));
        layer.setMetadataId(json.optString("metadataid", layer.getMetadataId()));
        layer.setTileMatrixSetId(json.optString("tile_matrix_set_id", layer.getTileMatrixSetId()));
        layer.setGfiType(json.optString("gfi_type", layer.getGfiType()));
        layer.setGfiXslt(json.optString("gfi_xslt", layer.getGfiXslt()));
        layer.setGfiContent(json.optString("gfi_content", layer.getGfiContent()));
        layer.setGeometry(json.optString("geometry", layer.getGeometry()));
        layer.setRealtime(json.optBoolean("realtime", layer.getRealtime()));
        layer.setRefreshRate(json.optInt("refresh_rate", layer.getRefreshRate()));
        layer.setSrs_name(json.optString("srs_name", layer.getGeometry()));
        layer.setVersion(json.optString("version", layer.getGeometry()));
        // omit permissions, these are handled by LayerHelper

        // handle params, check for null to avoid overwriting empty JS Object Literal
        final JSONObject params = json.optJSONObject("params");
        if (params != null) {
            layer.setParams(params);
        }

        // handle options, check for null to avoid overwriting empty JS Object Literal
        final JSONObject options = json.optJSONObject("options");
        if (options != null) {
            layer.setOptions(options);
        }

        // handle inspiretheme
        final InspireTheme theme = inspireThemeService.findByName(themeName);
        if (theme == null) {
            log.warn("Didn't find match for theme:", themeName);
        } else {
            layer.addInspireTheme(theme);
        }

        // setup data producer/layergroup
        final LayerGroup group = groupService.findByName(orgName);
        if(group == null) {
            log.warn("Didn't find match for layergroup:", orgName);
        } else {
            layer.addGroup(group);
        }

        return layer;
    }
}
