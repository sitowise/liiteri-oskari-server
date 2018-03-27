package fi.nls.oskari.map.layer.formatters;

import fi.nls.oskari.wmts.domain.WMTSCapabilities;

import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.map.geometry.ProjectionHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.wmts.domain.TileMatrixSet;
import fi.nls.oskari.wmts.domain.TileMatrixLink;
import fi.nls.oskari.wmts.domain.WMTSCapabilitiesLayer;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class LayerJSONFormatterWMTS extends LayerJSONFormatter {

    public static final String KEY_TILEMATRIXIDS = "tileMatrixIds";

    public JSONObject getJSON(OskariLayer layer,
                              final String lang,
                              final boolean isSecure) {

        final JSONObject layerJson = getBaseJSON(layer, lang, isSecure);

        // Use capabities in 1st hand for to get matrix id
        String mid = LayerJSONFormatterWMTS.getTileMatrixSetId(layer.getCapabilities(), layer.getSrs_name());
        if(mid != null){
            layer.setTileMatrixSetId(mid);
        }
        JSONHelper.putValue(layerJson, "tileMatrixSetId", layer.getTileMatrixSetId());

        // TODO: parse tileMatrixSetData for styles and set default style name from the one where isDefault = true
        String styleName = layer.getStyle();

        if(styleName == null || styleName.isEmpty()) {
            styleName = "default";
        }
        JSONHelper.putValue(layerJson, "style", styleName);
        JSONArray styles = new JSONArray();
        // currently supporting only one style (default style)
        styles.put(createStylesJSON(styleName, styleName, null));
        JSONHelper.putValue(layerJson, "styles", styles);

        // if options have urlTemplate -> use it (treat as a REST layer)
        final String urlTemplate = JSONHelper.getStringFromJSON(layer.getOptions(), "urlTemplate", null);
        final boolean needsProxy = useProxy(layer);
        if(urlTemplate != null) {
            if(needsProxy || isBeingProxiedViaOskariServer(layerJson.optString("url"))) {
                // remove requestEncoding so we always get KVP params when proxying
                JSONObject options = layerJson.optJSONObject("options");
                options.remove("requestEncoding");
            } else {
                // setup tileURL for REST layers
                final String originalUrl = layer.getUrl();
                layer.setUrl(urlTemplate);
                JSONHelper.putValue(layerJson, "tileUrl", layer.getUrl(isSecure));
                // switch back the original url in case it's used down the line
                layer.setUrl(originalUrl);
            }
        }
        return layerJson;
    }

    /**
     * @deprecated replaced by {@link #createCapabilitiesJSON(WMTSCapabilitiesLayer layer)}
     */
    @Deprecated
    public static JSONObject createCapabilitiesJSON(final WMTSCapabilities wmts,final WMTSCapabilitiesLayer layer) {
        return createCapabilitiesJSON(layer);
}

    public static JSONObject createCapabilitiesJSON(final WMTSCapabilitiesLayer layer) {
        JSONObject capabilities = new JSONObject();
        if(layer == null) {
            return capabilities;
        }

        List<JSONObject> tileMatrix = LayerJSONFormatterWMTS.createTileMatrixArray(layer);
        JSONHelper.putValue(capabilities, KEY_TILEMATRIXIDS, new JSONArray(tileMatrix));

        return capabilities;
    }

    /**
     * Return array of wmts tilematrixsets  (Crs code and Identifier)
     * @param layer wmts layer capabilities
     * @return
     */
    public static List<JSONObject> createTileMatrixArray(final WMTSCapabilitiesLayer layer) {
        final List<JSONObject> tileMatrix = new ArrayList<>();
        if (layer == null) {
            return tileMatrix;
        }

        for (TileMatrixLink link : layer.getLinks()) {
            TileMatrixSet tms = link.getTileMatrixSet();
            String identifier = tms.getId();
            String crs = tms.getCrs();
            String epsg = ProjectionHelper.shortSyntaxEpsg(crs);
            tileMatrix.add(JSONHelper.createJSONObject(epsg, identifier));
        }
        return tileMatrix;
    }

    /**
     * Constructs a Set containing the supported Coordinate Reference Systems of WMTS service
     */
    public static Set<String> getCRSs(final WMTSCapabilitiesLayer layer) {
        if (layer == null) {
            return null;
        }

        Set<String> crss = new HashSet<String>();
        for (TileMatrixLink link : layer.getLinks()) {
            TileMatrixSet tms = link.getTileMatrixSet();
            String crs = tms.getCrs();
            String epsg = ProjectionHelper.shortSyntaxEpsg(crs);
            crss.add(epsg);
        }
        return crss;
    }

    /**
     * Get matrix id by current crs
     */
    public static String getTileMatrixSetId(final JSONObject capabilities, final String crs) {
        if (capabilities.has("tileMatrixIds")) {
            JSONArray jsa = JSONHelper.getJSONArray(capabilities, "tileMatrixIds");

            for (int i = 0, size = jsa.length(); i < size; i++) {
                JSONObject js = JSONHelper.getJSONObject(jsa, i);
                if(js.has(crs)){
                    return JSONHelper.getStringFromJSON(js,crs,null);
                }
            }
        }
        return null;
    }

}
