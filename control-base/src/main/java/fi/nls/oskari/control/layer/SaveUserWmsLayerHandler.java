package fi.nls.oskari.control.layer;

import fi.mml.map.mapwindow.service.wms.WebMapService;
import fi.mml.map.mapwindow.service.wms.WebMapServiceFactory;
import fi.mml.map.mapwindow.service.wms.WebMapServiceParseException;
import fi.mml.map.mapwindow.service.wms.LayerNotFoundInCapabilitiesException;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.domain.map.UserWmsLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.UserWmsLayerService;
import fi.nls.oskari.map.layer.UserWmsLayerServiceIbatisImpl;
import fi.nls.oskari.map.layer.formatters.LayerJSONFormatter;
import fi.nls.oskari.map.layer.formatters.LayerJSONFormatterWMS;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.capabilities.CapabilitiesCacheService;
import fi.nls.oskari.service.capabilities.OskariLayerCapabilities;
import fi.nls.oskari.util.*;
import fi.nls.oskari.wmts.WMTSCapabilitiesParser;
import fi.nls.oskari.wmts.domain.ResourceUrl;
import fi.nls.oskari.wmts.domain.WMTSCapabilities;
import fi.nls.oskari.wmts.domain.WMTSCapabilitiesLayer;
import org.oskari.service.util.ServiceFactory;

import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Admin insert/update of WMS map layer
 */
@OskariActionRoute("SaveUserWmsLayer")
public class SaveUserWmsLayerHandler extends ActionHandler {

    private class SaveResult {
        long layerId = -1;
        boolean capabilitiesUpdated = false;
    }

    private UserWmsLayerService userWmsLayerService = new UserWmsLayerServiceIbatisImpl();

    private CapabilitiesCacheService capabilitiesService = ServiceFactory.getCapabilitiesCacheService();
    private final static LayerJSONFormatter FORMATTER = new LayerJSONFormatter();

    private static final Logger LOG = LogFactory.getLogger(SaveLayerHandler.class);
    private static final String PARAM_LAYER_ID = "layer_id";
    private static final String PARAM_LAYER_NAME = "layerName";
    private static final String PARAM_LAYER_URL = "layerUrl";

    private static final String LAYER_NAME_PREFIX = "name_";
    private static final String LAYER_TITLE_PREFIX = "title_";

    private static final String ERROR_UPDATE_OR_INSERT_FAILED = "update_or_insert_failed";
    private static final String ERROR_NO_LAYER_WITH_ID = "no_layer_with_id:";
    private static final String ERROR_OPERATION_NOT_PERMITTED = "operation_not_permitted_for_layer_id:";
    private static final String ERROR_MANDATORY_FIELD_MISSING = "mandatory_field_missing:";
    private static final String ERROR_INVALID_FIELD_VALUE = "invalid_field_value:";

    @Override
    public void handleAction(ActionParameters params) throws ActionException {

        final SaveResult result = saveLayer(params);
        final int layerId = (int)result.layerId;
        final UserWmsLayer ml = userWmsLayerService.find(layerId);
        if(ml == null) {
            throw new ActionParamsException("Couldn't get the saved layer from DB - id:" + layerId);
        }

        // construct response as layer json
        final JSONObject layerJSON = FORMATTER.getJSON(ml, params.getLocale().getLanguage(), false);
        if (layerJSON == null) {
            // handle error getting JSON failed
            throw new ActionException("Error constructing JSON for layer");
        }
        if(!result.capabilitiesUpdated) {
            // Cache update failed, no biggie
            JSONHelper.putValue(layerJSON, "warn", "metadataReadFailure");
            LOG.debug("Metadata read failure");
        }
        ResponseHelper.writeResponse(params, layerJSON);
    }

    private SaveResult saveLayer(final ActionParameters params) throws ActionException {

        // layer_id can be string -> external id!
        final String layer_id = params.getHttpParam(PARAM_LAYER_ID);
        SaveResult result = new SaveResult();

        try {
            // ************** UPDATE ************************
            if (layer_id != null) {

                final UserWmsLayer ml = userWmsLayerService.find(layer_id);
                if (ml == null) {
                    // layer wasn't found
                    throw new ActionException(ERROR_NO_LAYER_WITH_ID + layer_id);
                }
                if (params.getUser().getId() != ml.getUserId()) {
                    throw new ActionDeniedException(ERROR_OPERATION_NOT_PERMITTED + layer_id);
                }

                result.capabilitiesUpdated = handleRequestToMapLayer(params, ml);

                ml.setUpdated(new Date(System.currentTimeMillis()));
                userWmsLayerService.update(ml);
                //TODO: WFS spesific property update

                LOG.debug(ml);
                result.layerId = ml.getId();
                return result;
            }

            // ************** INSERT ************************
            else {

                final UserWmsLayer ml = new UserWmsLayer();
                final Date currentDate = new Date(System.currentTimeMillis());
                ml.setCreated(currentDate);
                ml.setUpdated(currentDate);
                result.capabilitiesUpdated = handleRequestToMapLayer(params, ml);

                int id = userWmsLayerService.insert(ml);
                ml.setId(id);

                if(ml.isCollection()) {
                    // update the name with the id for permission mapping
                    ml.setName(ml.getId() + "_group");
                    userWmsLayerService.update(ml);
                }

                // update keywords
                GetLayerKeywords glk = new GetLayerKeywords();
                glk.updateLayerKeywords(id, ml.getMetadataId());

                result.layerId = ml.getId();
                return result;
            }

        } catch (Exception e) {
            if (e instanceof ActionException) {
                throw (ActionException) e;
            } else {
                throw new ActionException(ERROR_UPDATE_OR_INSERT_FAILED, e);
            }
        }
    }

    private boolean handleRequestToMapLayer(final ActionParameters params, UserWmsLayer ml) throws ActionException, WebMapServiceParseException, LayerNotFoundInCapabilitiesException {

        HttpServletRequest request = params.getRequest();

        ml.setUserId(params.getUser().getId());

        if(ml.getId() == -1) {
            // setup type and parent for new layers only
            ml.setType(params.getHttpParam("layerType"));
            ml.setParentId(params.getHttpParam("parentId", -1));
        }

        // get names and descriptions
        final Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String nextName = paramNames.nextElement();
            if (nextName.indexOf(LAYER_NAME_PREFIX) == 0) {
                ml.setName(nextName.substring(LAYER_NAME_PREFIX.length()).toLowerCase(), params.getHttpParam(nextName));
            } else if (nextName.indexOf(LAYER_TITLE_PREFIX) == 0) {
                ml.setTitle(nextName.substring(LAYER_TITLE_PREFIX.length()).toLowerCase(), params.getHttpParam(nextName));
            }
        }

        ml.setVersion(params.getHttpParam("version"));

        ml.setBaseMap(ConversionHelper.getBoolean(params.getHttpParam("isBase"), false));

        if(ml.isCollection()) {
            // ulr is needed for permission mapping, name is updated after we get the layer id
            ml.setUrl(ml.getType());
            // the rest is not relevant for collection layers
            return true;
        }

        ml.setName(params.getRequiredParam(PARAM_LAYER_NAME, ERROR_MANDATORY_FIELD_MISSING + PARAM_LAYER_NAME));
        final String url = params.getRequiredParam(PARAM_LAYER_URL, ERROR_MANDATORY_FIELD_MISSING + PARAM_LAYER_URL);
        ml.setUrl(url);
        validateUrl(ml.getSimplifiedUrl(true));

        ml.setOpacity(params.getHttpParam("opacity", ml.getOpacity()));
        ml.setStyle(params.getHttpParam("style", ml.getStyle()));
        ml.setMinScale(ConversionHelper.getDouble(params.getHttpParam("minScale"), ml.getMinScale()));
        ml.setMaxScale(ConversionHelper.getDouble(params.getHttpParam("maxScale"), ml.getMaxScale()));

        ml.setLegendImage(params.getHttpParam("legendImage", ml.getLegendImage()));
        ml.setMetadataId(params.getHttpParam("metadataId", ml.getMetadataId()));
        ml.setTileMatrixSetId(params.getHttpParam("tileMatrixSetId"));

        final String gfiContent = request.getParameter("gfiContent");
        if (gfiContent != null) {
            // Clean GFI content
            final String[] tags = PropertyUtil.getCommaSeparatedList("gficontent.whitelist");
            HashMap<String,String[]> attributes = new HashMap<String, String[]>();
            HashMap<String[],String[]> protocols = new HashMap<String[], String[]>();
            String[] allAttributes = PropertyUtil.getCommaSeparatedList("gficontent.whitelist.attr");
            if (allAttributes.length > 0) {
                attributes.put(":all",allAttributes);
            }
            List<String> attrProps = PropertyUtil.getPropertyNamesStartingWith("gficontent.whitelist.attr.");
            for (String attrProp : attrProps) {
                String[] parts = attrProp.split("\\.");
                if (parts[parts.length-2].equals("protocol")) {
                    protocols.put(new String[]{parts[parts.length-3],parts[parts.length-1]},PropertyUtil.getCommaSeparatedList(attrProp));
                } else {
                    attributes.put(parts[parts.length-1],PropertyUtil.getCommaSeparatedList(attrProp));
                }
            }
            ml.setGfiContent(RequestHelper.cleanHTMLString(gfiContent, tags, attributes, protocols));
        }

        ml.setUsername(params.getHttpParam("username", ml.getUsername()));
        ml.setPassword(params.getHttpParam("password", ml.getPassword()));

        String attributes = params.getHttpParam("attributes");
        if (attributes == null || attributes.equals("")) {
            attributes = "{}";
        }

        ml.setAttributes(JSONHelper.createJSONObject(attributes));

        String parameters = params.getHttpParam("params");
        if (parameters == null || parameters.equals("")) {
            parameters = "{}";
        }
        if (!parameters.startsWith("{")) {
            parameters = "{time="+parameters+"}";
        }
        ml.setParams(JSONHelper.createJSONObject(parameters));

        ml.setSrs_name(params.getHttpParam("srs_name", ml.getSrs_name()));
        ml.setVersion(params.getHttpParam("version",ml.getVersion()));

        ml.setRealtime(ConversionHelper.getBoolean(params.getHttpParam("realtime"), ml.getRealtime()));
        ml.setRefreshRate(ConversionHelper.getInt(params.getHttpParam("refreshRate"), ml.getRefreshRate()));

        return handleWMSSpecific(params, ml);
    }

    private boolean handleWMSSpecific(final ActionParameters params, UserWmsLayer ml) throws ActionException, WebMapServiceParseException, LayerNotFoundInCapabilitiesException {

        HttpServletRequest request = params.getRequest();
        final String xslt = request.getParameter("xslt");
        if(xslt != null) {
            // TODO: some validation of XSLT data
            ml.setGfiXslt(xslt);
        }
        ml.setGfiType(params.getHttpParam("gfiType", ml.getGfiType()));

        try {
            OskariLayerCapabilities capabilities = capabilitiesService.getCapabilities(ml, true);
            // flush cache, otherwise only db is updated but code retains the old cached version
            WebMapServiceFactory.flushCache(ml.getId());
            // parse capabilities
            WebMapService wms = WebMapServiceFactory.createFromXML(ml.getName(), capabilities.getData());
            if (wms == null) {
                throw new ServiceException("Couldn't parse capabilities for service!");
            }
            JSONObject caps = LayerJSONFormatterWMS.createCapabilitiesJSON(wms);
            ml.setCapabilities(caps);
            return true;
        } catch (ServiceException ex) {
            LOG.error(ex, "Couldn't update capabilities for layer", ml);
            return false;
        }
    }

    private String validateUrl(final String url) throws ActionParamsException {
        try {
            // check that it's a valid url by creating an URL object...
            new URL(url);
        } catch (MalformedURLException e) {
            throw new ActionParamsException(ERROR_INVALID_FIELD_VALUE + PARAM_LAYER_URL);
        }
        return url;
    }

}