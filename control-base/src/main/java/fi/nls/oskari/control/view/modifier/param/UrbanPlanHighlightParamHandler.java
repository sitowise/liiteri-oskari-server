package fi.nls.oskari.control.view.modifier.param;

import java.util.ArrayList;
import java.util.List;

import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.view.modifier.ModifierParams;
import fi.nls.oskari.annotation.OskariViewModifier;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceIbatisImpl;
import fi.nls.oskari.view.modifier.ModifierException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.sito.liiteri.arcgis.domain.ArcgisLayer;
import pl.sito.liiteri.map.arcgislayer.service.ArcgisLayerService;
import pl.sito.liiteri.utils.ArcgisUtils;

@OskariViewModifier("asemakaava")
public class UrbanPlanHighlightParamHandler extends WFSHighlightParamHandler {

    private static final Logger log = LogFactory.getLogger(UrbanPlanHighlightParamHandler.class);

    private final OskariLayerService service = new OskariLayerServiceIbatisImpl();
    private final ArcgisLayerService arcgisLayerService = new ArcgisLayerService();

    private static final String LAYER_ID = PropertyUtil.get("liiteri.urbanplanning.highlightlayerid");
    private static final String LAYER_ID_UNDERGROUND = PropertyUtil.get("liiteri.urbanplanning.undergroundhighlightlayerid");
    private static final String FILTER_ATTRIBUTE = "Tyvi_Id";

    @Override
    public boolean handleParam(ModifierParams params) throws ModifierException {
        if (params.getParamValue() == null) {
            return false;
        }

        final JSONObject postprocessorState = getPostProcessorState(params);
        String layerId = LAYER_ID;
        JSONArray features = findFeatures(layerId, params.getParamValue());
        if (features.length() == 0) { // not found from normal layer, check also underground areas
            layerId = LAYER_ID_UNDERGROUND;
            features = findFeatures(layerId, params.getParamValue());
        }

        final JSONArray featureIdList = new JSONArray();
        List<JSONArray> bboxes = new ArrayList<JSONArray>();

        for (int i = 0; i < features.length(); ++i) {
            try {
                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                String objectId = properties.get("OBJECTID").toString();
                JSONArray bbox = feature.getJSONArray("bbox");
                bboxes.add(bbox);
                featureIdList.put(objectId);
            } catch (JSONException e) {
                log.error(e, "Error reading features");
                return false;
            }
        }

        try {
            postprocessorState.put("highlightFeatureId", featureIdList);
            postprocessorState.put(STATE_LAYERID_KEY, layerId);

            JSONArray points = new JSONArray();

            for (JSONArray bbox : bboxes) {
                JSONObject ll = new JSONObject();
                JSONObject tr = new JSONObject();

                ll.put("lon", bbox.get(0));
                ll.put("lat", bbox.get(1));
                tr.put("lon", bbox.get(2));
                tr.put("lat", bbox.get(3));

                points.put(ll);
                points.put(tr);
            }

            postprocessorState.put("featurePoints", points);
        } catch (Exception ex) {
            log.error(ex, "Couldn't insert features to postprocessor bundle state");
        }

        return featureIdList.length() > 0;
    }

    private JSONArray findFeatures(String layerId, String featureId) {
        final OskariLayer layer = service.find(layerId);

        JSONArray features = new JSONArray();

        if (layer == null) {
            log.error("Cannot find layer [%s] in configuration", layerId);
            return features;
        }

        int arcgisId = ArcgisUtils.getArcgisId(layer);
        if (arcgisId < 0) {
            log.error("Cannot find arcgis layer id in OskariLayer", layerId);
            return features;
        }

        String arcgisMapServerUrl = ArcgisUtils.getArcgisMapServerUrl(layer);

        final ArcgisLayer arcgisLayer = new ArcgisLayer(arcgisMapServerUrl, arcgisId);
        try {
            String filter = getFilter(FILTER_ATTRIBUTE, featureId);
            features = new JSONArray(arcgisLayerService.getFeatures(arcgisLayer, null, filter));
        } catch (Exception e) {
            log.error(e, "Cannot get features for layer " + layerId);
            return features;
        }
        return features;
    }

    private String getFilter(String attribute, String value) throws JSONException {
        JSONObject ret = new JSONObject();
        JSONArray filters = new JSONArray();
        JSONObject filter = new JSONObject();
        filter.put("attribute", attribute);
        filter.put("operator", "=");
        filter.put("value", value);

        filters.put(filter);

        ret.put("filters", filters);

        return ret.toString();
    }
}
