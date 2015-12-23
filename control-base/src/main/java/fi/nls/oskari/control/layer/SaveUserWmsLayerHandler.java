package fi.nls.oskari.control.layer;

import fi.mml.map.mapwindow.service.db.CapabilitiesCacheService;
import fi.mml.map.mapwindow.service.wms.WebMapServiceFactory;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.domain.map.CapabilitiesCache;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.domain.map.UserWmsLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.UserWmsLayerService;
import fi.nls.oskari.map.layer.UserWmsLayerServiceIbatisImpl;
import fi.nls.oskari.map.layer.formatters.LayerJSONFormatter;
import fi.nls.oskari.util.*;

import org.apache.commons.collections.bag.TreeBag;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;

/**
 * Admin insert/update of WMS map layer
 */
@OskariActionRoute("SaveUserWmsLayer")
public class SaveUserWmsLayerHandler extends ActionHandler {

    private static final UserWmsLayerService userWmsLayerService = new UserWmsLayerServiceIbatisImpl();
    private CapabilitiesCacheService capabilitiesService = ServiceFactory.getCapabilitiesCacheService();
    private final static LayerJSONFormatter FORMATTER = new LayerJSONFormatter();

    private static final Logger log = LogFactory.getLogger(SaveUserWmsLayerHandler.class);
    private static final String PARAM_LAYER_ID = "layer_id";
    private static final String PARAM_WMS_NAME = "wmsName";
    private static final String PARAM_WMS_URL = "wmsUrl"; 

    private static final String LAYER_NAME_PREFIX = "name_";
    private static final String LAYER_TITLE_PREFIX = "title_";

    private static final String ERROR_UPDATE_OR_INSERT_FAILED = "update_or_insert_failed";
    private static final String ERROR_NO_LAYER_WITH_ID = "no_layer_with_id:";
    private static final String ERROR_OPERATION_NOT_PERMITTED = "operation_not_permitted_for_layer_id:";
    private static final String ERROR_MANDATORY_FIELD_MISSING = "mandatory_field_missing:";
    private static final String ERROR_INVALID_FIELD_VALUE = "invalid_field_value:"; 

    final static String LANGUAGE_ATTRIBUTE = "lang";


    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        final int layerId = saveLayer(params);
        final UserWmsLayer ml = userWmsLayerService.find(layerId);
        if(ml == null) {
            throw new ActionException("Couldn't get the saved layer from DB - id:" + layerId);
        }
        
        // update cache - do this before creating json!
        boolean cacheUpdated = ml.isCollection();
        // skip cache update for collections since they don't have the info
        if(!ml.isCollection() && ml.getType().equals(OskariLayer.TYPE_WMS)) {
            cacheUpdated = updateCache(ml, params.getRequiredParam("version"));
        }

        // construct response as layer json
        final String lang = params.getHttpParam(LANGUAGE_ATTRIBUTE, params
                .getLocale().getLanguage());
        final JSONObject layerJSON = FORMATTER.getJSON(ml, lang, false);
        if (layerJSON == null) {
            // handle error getting JSON failed
            throw new ActionException("Error constructing JSON for layer");
        }
        
        if(!cacheUpdated && !ml.isCollection() && ml.getType().equals(OskariLayer.TYPE_WMS)) {
            // Cache update failed, no biggie
            JSONHelper.putValue(layerJSON, "warn", "metadataReadFailure");
        }
        
        ResponseHelper.writeResponse(params, layerJSON);
    }   

    private int saveLayer(final ActionParameters params) throws ActionException {

        // layer_id can be string -> external id!
        String layer_id = params.getHttpParam(PARAM_LAYER_ID);

        try {
            // ************** UPDATE ************************
            if (layer_id != null) {
                layer_id = layer_id.split("_")[1];

                final UserWmsLayer ml = userWmsLayerService.find(layer_id);
                if (ml == null) {
                    // layer wasn't found
                    throw new ActionException(ERROR_NO_LAYER_WITH_ID + layer_id);
                }
                if (params.getUser().getId() != ml.getUserId()) {
                    throw new ActionDeniedException(ERROR_OPERATION_NOT_PERMITTED + layer_id);
                }

                handleRequestToMapLayer(params, ml);

                ml.setUpdated(new Date(System.currentTimeMillis()));
                userWmsLayerService.update(ml);

                log.debug(ml);

                return ml.getId();
            }

            // ************** INSERT ************************
            else {

                final UserWmsLayer ml = new UserWmsLayer();
                final Date currentDate = new Date(System.currentTimeMillis());
                ml.setCreated(currentDate);
                ml.setUpdated(currentDate);
                handleRequestToMapLayer(params, ml);
                int id = userWmsLayerService.insert(ml);
                ml.setId(id);

                if(ml.isCollection()) {
                    // update the name with the id for permission mapping
                    ml.setName(ml.getId() + "_group");
                    userWmsLayerService.update(ml);
                }

                return ml.getId();
            }

        } catch (Exception e) {
            if (e instanceof ActionException) {
                throw (ActionException) e;
            } else {
                throw new ActionException(ERROR_UPDATE_OR_INSERT_FAILED, e);
            }
        }
    }

    private String getWmsUrl(String wmsUrl) {
        if(wmsUrl == null) {
            return null;
        }
        //check if comma separated urls
        if (wmsUrl.indexOf(",http:") > 0) {
            wmsUrl = wmsUrl.substring(0, wmsUrl.indexOf(",http:"));
        }
        return wmsUrl;

    }

    private void handleRequestToMapLayer(final ActionParameters params, UserWmsLayer ml) throws ActionException {

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

        ml.setBaseMap(ConversionHelper.getBoolean(params.getHttpParam("isBase"), false));

        if(ml.isCollection()) {
            // ulr is needed for permission mapping, name is updated after we get the layer id
            ml.setUrl(ml.getType());
            // the rest is not relevant for collection layers
            return;
        }

        ml.setName(params.getRequiredParam(PARAM_WMS_NAME, ERROR_MANDATORY_FIELD_MISSING + PARAM_WMS_NAME));
        final String url = validateUrl(params.getRequiredParam(PARAM_WMS_URL, ERROR_MANDATORY_FIELD_MISSING + PARAM_WMS_URL));
        ml.setUrl(url);

        ml.setOpacity(params.getHttpParam("opacity", ml.getOpacity()));
        ml.setStyle(params.getHttpParam("style", ml.getStyle()));
        ml.setMinScale(ConversionHelper.getDouble(params.getHttpParam("minScale"), ml.getMinScale()));
        ml.setMaxScale(ConversionHelper.getDouble(params.getHttpParam("maxScale"), ml.getMaxScale()));

        ml.setLegendImage(params.getHttpParam("legendImage", ml.getLegendImage()));
        ml.setMetadataId(params.getHttpParam("metadataId", ml.getMetadataId()));
        ml.setTileMatrixSetId(params.getHttpParam("tileMatrixSetId"));
        ml.setTileMatrixSetData(params.getHttpParam("tileMatrixSetData"));

        final String xslt = request.getParameter("xslt");
        if(xslt != null) {
            // TODO: some validation of XSLT data
            ml.setGfiXslt(xslt);
        }
        final String gfiContent = request.getParameter("gfiContent");
        if (gfiContent != null) {
            // TODO: some sanitation of content data
            ml.setGfiContent(gfiContent);
        }
        ml.setGfiType(params.getHttpParam("gfiType", ml.getGfiType()));

        ml.setRealtime(ConversionHelper.getBoolean(params.getHttpParam("realtime"), ml.getRealtime()));
        ml.setRefreshRate(ConversionHelper.getInt(params.getHttpParam("refreshRate"), ml.getRefreshRate()));
        
        ml.setDownloadServiceUrl(params.getHttpParam("downloadServiceUrl"));
        ml.setCopyrightInfo(params.getHttpParam("copyrightInfo"));
    }

    private String validateUrl(final String url) throws ActionParamsException {
        try {
            // check that it's a valid url by creating an URL object...
            new URL(getWmsUrl(url));
        } catch (MalformedURLException e) {
            throw new ActionParamsException(ERROR_INVALID_FIELD_VALUE + PARAM_WMS_URL);
        }
        return url;
    }
    
    private boolean updateCache(OskariLayer ml, final String version) throws ActionException {
        if(ml == null) {
            return false;
        }
        if(ml.isCollection()) {
            // just be happy for collection layers, nothing to do
            return true;
        }
        if(version == null) {
            // check this here since it's not always required (for collection layers)
            throw new ActionParamsException("Version is required!");
        }
        // retrieve capabilities
        final String wmsUrl = getWmsUrl(ml.getUrl());
        CapabilitiesCache cc = null;
        try {
        	CapabilitiesCache ccForSearch = new CapabilitiesCache();
        	ccForSearch.setLayerId(ml.getId());
        	ccForSearch.setUserWms(true);
        	
            cc = capabilitiesService.findByLayer(ccForSearch);
            boolean isNew = false;
            if (cc == null) {
                cc = new CapabilitiesCache();
                cc.setLayerId(ml.getId());
                cc.setUserWms(true);
                isNew = true;
            }
            cc.setVersion(version);

            final String capabilitiesXML = GetWMSCapabilities.getResponse(wmsUrl);
            cc.setData(capabilitiesXML);

            // update cache by updating db
            if (isNew) {
                capabilitiesService.insert(cc);
            } else {
                capabilitiesService.update(cc);
            }
            // flush cache, otherwise only db is updated but code retains the old cached version
            WebMapServiceFactory.flushCache(ml.getId(), true);
        } catch (Exception ex) {
            log.info(ex, "Error updating capabilities: ", cc, "from URL:", wmsUrl);
            return false;
        }
        return true;
    }

}
