package fi.nls.oskari.control.layer;

import fi.mml.map.mapwindow.service.db.InspireThemeService;
import fi.mml.map.mapwindow.service.wms.WebMapService;
import fi.mml.map.mapwindow.service.wms.WebMapServiceFactory;
import fi.mml.map.mapwindow.util.OskariLayerWorker;
import fi.mml.portti.domain.permissions.Permissions;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.InspireTheme;
import fi.nls.oskari.domain.map.LayerGroup;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.domain.map.wfs.WFSLayerConfiguration;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.wfs.WFSSLDStyle;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.data.domain.OskariLayerResource;
import fi.nls.oskari.map.layer.LayerGroupService;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.formatters.LayerJSONFormatterWMS;
import fi.nls.oskari.permission.domain.Permission;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.capabilities.CapabilitiesCacheService;
import fi.nls.oskari.service.capabilities.OskariLayerCapabilities;
import fi.nls.oskari.map.userowndata.GisDataDbService;
import fi.nls.oskari.map.userowndata.GisDataDbServiceImpl;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.*;
import fi.nls.oskari.wfs.WFSLayerConfigurationService;
import fi.nls.oskari.wfs.util.WFSParserConfigs;
import fi.nls.oskari.wmts.WMTSCapabilitiesParser;
import fi.nls.oskari.wmts.domain.ResourceUrl;
import fi.nls.oskari.wmts.domain.WMTSCapabilities;
import fi.nls.oskari.wmts.domain.WMTSCapabilitiesLayer;
import org.json.JSONArray;
import fi.nls.oskari.wfs.WFSLayerConfigurationService;
import fi.nls.oskari.wfs.WFSLayerConfigurationServiceIbatisImpl;
import fi.nls.oskari.wfs.WFSLayerConfiguration;

import org.json.JSONObject;

import pl.sito.liiteri.groupings.service.GroupingsService;

import javax.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * Admin insert/update of WMS map layer
 */
@OskariActionRoute("SaveLayer")
public class SaveLayerHandler extends ActionHandler {

    private class SaveResult {
        long layerId = -1;
        boolean capabilitiesUpdated = false;
    }

    private OskariLayerService mapLayerService = ServiceFactory.getMapLayerService();
    private WFSLayerConfigurationService wfsLayerService = ServiceFactory.getWfsLayerService();
    private PermissionsService permissionsService = ServiceFactory.getPermissionsService();
    private LayerGroupService layerGroupService = ServiceFactory.getLayerGroupService();
    private InspireThemeService inspireThemeService = ServiceFactory.getInspireThemeService();
    private CapabilitiesCacheService capabilitiesService = ServiceFactory.getCapabilitiesCacheService();
    private WFSParserConfigs wfsParserConfigs = new WFSParserConfigs();

    private final WFSLayerConfigurationService layerConfigurationService = new WFSLayerConfigurationServiceIbatisImpl();    
    private final GroupingsService groupingsService = GroupingsService.getInstance();
    private static final GisDataDbService gisDataService = new GisDataDbServiceImpl();
    private static final Logger LOG = LogFactory.getLogger(SaveLayerHandler.class);
    private static final String PARAM_LAYER_ID = "layer_id";
    private static final String PARAM_LAYER_NAME = "layerName";
    private static final String PARAM_LAYER_URL = "layerUrl";
    
    private static final String PARAM_WFS_USERNAME = "username";
    private static final String PARAM_WFS_PASSWORD = "password";
    private static final String PARAM_WFS_GMLVERSION = "GMLVersion";
    private static final String PARAM_WFS_GMLGeometryProperty = "GMLGeometryProperty";
    private static final String PARAM_WFS_FeatureNamespaceURI = "featureNamespaceURI";
    private static final String PARAM_WFS_FeatureNamespace = "featureNamespace";
    private static final String PARAM_WFS_FeatureElement = "featureElement";
    private static final String PARAM_WFS_WFSVersion = "WFSVersion";
    private static final String PARAM_WFS_GeometryNamespaceURI = "geometryNamespaceURI";
    
    private static final String PARAM_SRS_NAME = "srsName";    

    private static final String LAYER_NAME_PREFIX = "name_";
    private static final String LAYER_TITLE_PREFIX = "title_";

    private static final String ERROR_UPDATE_OR_INSERT_FAILED = "update_or_insert_failed";
    private static final String ERROR_NO_LAYER_WITH_ID = "no_layer_with_id:";
    private static final String ERROR_OPERATION_NOT_PERMITTED = "operation_not_permitted_for_layer_id:";
    private static final String ERROR_MANDATORY_FIELD_MISSING = "mandatory_field_missing:";
    private static final String ERROR_INVALID_FIELD_VALUE = "invalid_field_value:";    
    
    
    
    private static final String ERROR_FE_PARSER_CONFIG_MISSING = "FE WFS feature parser config missing";

    private static final String OSKARI_FEATURE_ENGINE = "oskari-feature-engine";
    private static final String WFS1_1_0_VERSION = "1.1.0";

    @Override
    public void handleAction(ActionParameters params) throws ActionException {

    	final String layerType = params.getHttpParam("layerType");
    	final String layerIdStr = params.getHttpParam("layer_id");
    	if (layerType.equals("myplaces") || layerType.equals("analysis") || layerType.equals("userlayer")) {
    		//find dataset by data_id
    		UserGisData ugd = null;
    		
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date expirationDate = null;
			try {
				expirationDate = sdf.parse(sdf.format(new Date()));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
			
    		List<UserGisData> ugds = null;
			try {
				ugds = gisDataService.getGisData(params.getUser().getId(), expirationDate);
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		for (UserGisData u : ugds) {
    			if (u.getDataId().equals(layerIdStr)) {
    				ugd = u;
    				break;
    			}
    		}
    		if (ugd != null) {
	    		String downloadServiceUrl = params.getHttpParam("downloadServiceUrl");
	    		ugd.setDownloadServiceUrl(downloadServiceUrl);
	    		gisDataService.update(ugd);			
	    		//TODO create response to frontend
	    		//ResponseHelper.writeResponse(params, "You successfully saved the layer.");
    		}
    	}
        final int layerId = saveLayer(params);
        final OskariLayer ml = mapLayerService.find(layerId);
        if(ml == null) {
            throw new ActionParamsException("Couldn't get the saved layer from DB - id:" + layerId);
        }

        // update cache - do this before creating json!
        boolean cacheUpdated = ml.isCollection();
        // skip cache update for collections since they don't have the info
        if(!ml.isCollection() && ml.getType().equals(OskariLayer.TYPE_WMS)) {
            cacheUpdated = updateCache(ml, params.getRequiredParam("version"));
        }
        
        if (!ml.isCollection() && 
        		(ml.getType().equals(OskariLayer.TYPE_ARCGISLAYER) || ml.getType().equals(OskariLayer.TYPE_WFS))) {
        	updateFeatureLayer(ml, params);
        }               

        // construct response as layer json
        final JSONObject layerJSON = OskariLayerWorker.getMapLayerJSON(ml, params.getUser(), params.getLocale().getLanguage());
        if (layerJSON == null) {
            // handle error getting JSON failed
            throw new ActionException("Error constructing JSON for layer");
        }
        if(!result.capabilitiesUpdated && !ml.isCollection() && ml.getType().equals(OskariLayer.TYPE_WMS)) {
            // Cache update failed, no biggie
            JSONHelper.putValue(layerJSON, "warn", "metadataReadFailure");
            LOG.debug("Metadata read failure");
        }
        ResponseHelper.writeResponse(params, layerJSON);
    }   
    
    private void updateFeatureLayer(final OskariLayer oskariLayer, final ActionParameters params) {
    	WFSLayerConfiguration configuration = layerConfigurationService.findConfiguration(oskariLayer.getId());
    	
    	if (configuration == null) {
    		configuration = mapConfiguration(oskariLayer, params);    		
    		layerConfigurationService.insert(configuration);
    	} else {
    		configuration.destroy();
    		configuration = mapConfiguration(oskariLayer, params);
    		layerConfigurationService.update(configuration);
    	}
    }
    
    private WFSLayerConfiguration mapConfiguration(final OskariLayer oskariLayer, final ActionParameters params) {
    	WFSLayerConfiguration configuration = new WFSLayerConfiguration();
		configuration.setLayerId("" + oskariLayer.getId());
		configuration.setLayerName(oskariLayer.getName());
		configuration.setURL(oskariLayer.getUrl());
		
		configuration.setGML2Separator(false);
		configuration.setGetHighlightImage(true);		
		configuration.setMaxFeatures(1000);
		
		if (oskariLayer.getType().equals(OskariLayer.TYPE_ARCGISLAYER)) {
			configuration.setGMLVersion("3.1.1");
			configuration.setGMLGeometryProperty("geom");
			configuration.setFeatureNamespaceURI("''");
			configuration.setFeatureNamespace("arcgis");
			configuration.setFeatureElement("''");
			configuration.setWFSVersion("1.0.0");
			configuration.setGeometryNamespaceURI("''");
		}			
		else if (oskariLayer.getType().equals(OskariLayer.TYPE_WFS)) {
			configuration.setUsername(params.getHttpParam(PARAM_WFS_USERNAME, null));
			configuration.setPassword(params.getHttpParam(PARAM_WFS_PASSWORD, null));
			configuration.setGMLVersion(params.getHttpParam(PARAM_WFS_GMLVERSION, "3.1.1"));
			configuration.setGMLGeometryProperty(params.getHttpParam(PARAM_WFS_GMLGeometryProperty, "geom"));
			configuration.setFeatureNamespaceURI(params.getHttpParam(PARAM_WFS_FeatureNamespaceURI, null));
			configuration.setFeatureNamespace(params.getHttpParam(PARAM_WFS_FeatureNamespace, null));
			configuration.setFeatureElement(params.getHttpParam(PARAM_WFS_FeatureElement, null));
			configuration.setWFSVersion(params.getHttpParam(PARAM_WFS_WFSVersion, "1.1.0"));
			configuration.setGeometryNamespaceURI(params.getHttpParam(PARAM_WFS_GeometryNamespaceURI, "''"));
		}
			
		configuration.setFeatureType("{}");
		configuration.setSelectedFeatureParams("{}");
		configuration.setFeatureParamsLocales("{}");
		configuration.setGeometryType("2d");
		configuration.setGetMapTiles(true);
		configuration.setGetFeatureInfo(true);
		configuration.setTileRequest(false);
		configuration.setTileBuffer("{}");
		configuration.setWps_params("{}");
		configuration.setSRSName(params.getHttpParam(PARAM_SRS_NAME, "EPSG:3067"));
		configuration.setCustomParser(false);
		configuration.setTestLocation(null);
		configuration.setTestZoom(9);   
		configuration.setSLDStyles(new ArrayList<WFSSLDStyle>());
		configuration.setMaxScale(oskariLayer.getMaxScale());
		configuration.setMinScale(oskariLayer.getMinScale());		
		
		return configuration;
    }

    private SaveResult saveLayer(final ActionParameters params) throws ActionException {

        // layer_id can be string -> external id!
        final String layer_id = params.getHttpParam(PARAM_LAYER_ID);
        SaveResult result = new SaveResult();

        try {
            // ************** UPDATE ************************
            if (layer_id != null) {

                final OskariLayer ml = mapLayerService.find(layer_id);
                if (ml == null) {
                    // layer wasn't found
                    throw new ActionException(ERROR_NO_LAYER_WITH_ID + layer_id);
                }
                if (!permissionsService.hasEditPermissionForLayerByLayerId(params.getUser(), ml.getId())) {
                    throw new ActionDeniedException(ERROR_OPERATION_NOT_PERMITTED + layer_id);
                }

                result.capabilitiesUpdated = handleRequestToMapLayer(params, ml);

                ml.setUpdated(new Date(System.currentTimeMillis()));
                mapLayerService.update(ml);
                //TODO: WFS spesific property update

                LOG.debug(ml);
                result.layerId = ml.getId();
                return result;
            }

            // ************** INSERT ************************
            else {

                if (!permissionsService.hasAddLayerPermission(params.getUser())) {
                    throw new ActionDeniedException(ERROR_OPERATION_NOT_PERMITTED + layer_id);
                }

                final OskariLayer ml = new OskariLayer();
                final Date currentDate = new Date(System.currentTimeMillis());
                ml.setCreated(currentDate);
                ml.setUpdated(currentDate);
                result.capabilitiesUpdated = handleRequestToMapLayer(params, ml);
                validateInsertLayer(params, ml);

                int id = mapLayerService.insert(ml);
                ml.setId(id);

                if(ml.isCollection()) {
                    // update the name with the id for permission mapping
                    ml.setName(ml.getId() + "_group");
                    mapLayerService.update(ml);
                }
                // Wfs
                if(OskariLayer.TYPE_WFS.equals(ml.getType())) {
                    final WFSLayerConfiguration wfsl = new WFSLayerConfiguration();
                    wfsl.setDefaults();
                    wfsl.setLayerId(Integer.toString(id));
                    wfsl.setAttributes(ml.getAttributes());
                    handleRequestToWfsLayer(params, wfsl);
                    if(wfsl.getJobType() != null && wfsl.getJobType().equals(OSKARI_FEATURE_ENGINE)){
                        handleFESpesificToWfsLayer(params, wfsl);
                    }
                    int idwfsl = wfsLayerService.insert(wfsl);
                    wfsl.setId(idwfsl);

                }

                addPermissionsForRoles(ml,
                        getPermissionSet(params.getHttpParam("viewPermissions")),
                        getPermissionSet(params.getHttpParam("publishPermissions")),
                        getPermissionSet(params.getHttpParam("downloadPermissions")),
                        getPermissionSet(params.getHttpParam("enbeddedPermissions")));
                
                int userThemeId = params.getHttpParam("userThemeId", -1);
                if (userThemeId != -1)
                	mapToUserTheme(ml, params.getUser(), userThemeId);

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

    private void mapToUserTheme(OskariLayer ml, User user, int userThemeId)
	{
    	groupingsService.AddLayerToUserTheme(ml.getId(), userThemeId);		
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
        	ccForSearch.setUserWms(false);
        	
            cc = capabilitiesService.findByLayer(ccForSearch);
            boolean isNew = false;
            if (cc == null) {
                cc = new CapabilitiesCache();
                cc.setLayerId(ml.getId());
                cc.setUserWms(false);
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
            WebMapServiceFactory.flushCache(ml.getId(), false);
        } catch (Exception ex) {
            log.info(ex, "Error updating capabilities: ", cc, "from URL:", wmsUrl);
            return false;
        }
        return true;
    }
    /**
     * Treats the param as comma-separated list. Splits to individual values and
     * returns a set of values that could be converted to Long
     * @param param
     * @return
     */
    private Set<Long> getPermissionSet(final String param) {
        if(param == null) {
            return Collections.emptySet();
		}
		final Set<Long> set = new HashSet<Long>();
        final String[] roleIds = param.split(",");
        for (String externalId : roleIds) {
            final long extId = ConversionHelper.getLong(externalId, -1);
            if (extId != -1) {
                set.add(extId);
			}
		}
		return set;
	}

    private boolean handleRequestToMapLayer(final ActionParameters params, OskariLayer ml) throws ActionException {

        HttpServletRequest request = params.getRequest();

        if(ml.getId() == -1) {
            // setup type and parent for new layers only
            ml.setType(params.getHttpParam("layerType"));
            ml.setParentId(params.getHttpParam("parentId", -1));
        }

        // organization id
        final LayerGroup group = layerGroupService.find(params.getHttpParam("groupId", PropertyUtil.getOptional("oskari.defaultLayerGroup", -1)));
        ml.addGroup(group);

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

        InspireTheme theme = inspireThemeService.find(params.getHttpParam("inspireTheme", -1));
        ml.addInspireTheme(theme);

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

        if(OskariLayer.TYPE_WMS.equals(ml.getType())) {
            return handleWMSSpecific(params, ml);
        }
        else if(OskariLayer.TYPE_WFS.equals(ml.getType())) {
            handleWFSSpecific(params, ml);
            return true;
        }
        else if(OskariLayer.TYPE_WMTS.equals(ml.getType())) {
            return handleWMTSSpecific(params, ml);
        }
        // no capabilities to update, return true
        return true;
    }

    private void handleRequestToWfsLayer(final ActionParameters params, WFSLayerConfiguration wfsl) throws ActionException {

        wfsl.setGML2Separator(ConversionHelper.getBoolean(params.getHttpParam("GML2Separator"), wfsl.isGML2Separator()));
        wfsl.setGMLGeometryProperty(params.getHttpParam("GMLGeometryProperty"));
        wfsl.setGMLVersion(params.getHttpParam("GMLVersion"));
        wfsl.setSRSName(params.getHttpParam("srs_name"));
        wfsl.setWFSVersion(params.getHttpParam("WFSVersion"));
        wfsl.setFeatureElement(params.getHttpParam("featureElement"));
        wfsl.setFeatureNamespace(params.getHttpParam("featureNamespace"));
        wfsl.setFeatureNamespaceURI(params.getHttpParam("featureNamespaceURI"));
        wfsl.setFeatureParamsLocales(params.getHttpParam("featureParamsLocales"));
        wfsl.setFeatureType(params.getHttpParam("featureType"));
        wfsl.setGeometryNamespaceURI(params.getHttpParam("geometryNamespaceURI"));
        wfsl.setGeometryType(params.getHttpParam("geometryType"));
        wfsl.setGetFeatureInfo(ConversionHelper.getBoolean(params.getHttpParam("getFeatureInfo"), wfsl.isGetFeatureInfo()));
        wfsl.setGetHighlightImage(ConversionHelper.getBoolean(params.getHttpParam("getHighlightImage"), wfsl.isGetHighlightImage()));
        wfsl.setGetMapTiles(ConversionHelper.getBoolean(params.getHttpParam("getMapTiles"), wfsl.isGetMapTiles()));

        wfsl.setLayerName(params.getHttpParam("layerName"));
        wfsl.setMaxFeatures(ConversionHelper.getInt(params.getHttpParam("maxFeatures"), wfsl.getMaxFeatures()));
        wfsl.setOutputFormat(params.getHttpParam("outputFormat"));
        wfsl.setSelectedFeatureParams(params.getHttpParam("selectedFeatureParams"));
        wfsl.setTileBuffer(params.getHttpParam("tileBuffer"));
        wfsl.setTileRequest(ConversionHelper.getBoolean(params.getHttpParam("tileRequest"), wfsl.isTileRequest()));
        wfsl.setJobType(params.getHttpParam("jobType"));

    }

    private void handleFESpesificToWfsLayer(  final ActionParameters params, WFSLayerConfiguration wfsl) throws ActionException, ServiceException {


        if (wfsl != null) {
            if(!wfsl.getWFSVersion().equals(WFS1_1_0_VERSION)) {
                wfsl.setRequestTemplate(params.getHttpParam("requestTemplate"));
                wfsl.setResponseTemplate(params.getHttpParam("responseTemplate"));
                wfsl.setParseConfig(params.getHttpParam("parseConfig"));
                wfsl.setTemplateName(params.getHttpParam("templateName"));
                wfsl.setTemplateType(params.getHttpParam("templateType"));
                wfsl.setTemplateDescription("FE parser model - wfs version : " + wfsl.getWFSVersion());
                Map<String, String> model = new HashMap<String, String>();

                model.put("name", wfsl.getFeatureNamespace() + ":" + wfsl.getFeatureElement());
                model.put("description", wfsl.getTemplateDescription());
                model.put("type", wfsl.getTemplateType());
                model.put("request_template", wfsl.getRequestTemplate());
                model.put("response_template", wfsl.getResponseTemplate());
                model.put("parse_config", wfsl.getParseConfig().toString());

                int model_id = wfsLayerService.insertTemplateModel(model);

                wfsl.setTemplateModelId(model_id);
            }
            else {
                //TODO: fe save config support for wfs 1.1.0
                wfsl.setJobType(OSKARI_FEATURE_ENGINE);
                wfsl.setTileBuffer("{ \"default\" : 1, \"oskari_custom\" : 1}");;
            }

        }

    }

    /**
     * Check before maplayer insert that all data is valid for specific layer types
     *
     * @param params
     * @param ml
     * @throws ActionException
     */
    private void validateInsertLayer(final ActionParameters params, OskariLayer ml) throws ActionException {

        if (!OskariLayer.TYPE_WFS.equals(ml.getType())) {
            return;
        }
        if(params.getHttpParam("jobType") != null && params.getHttpParam("jobType").equals(OSKARI_FEATURE_ENGINE) ) {
            try {

                JSONArray feaconf = wfsParserConfigs.getFeatureTypeConfig(params.getHttpParam("featureNamespace") + ":" + params.getHttpParam("featureElement"));
                if (feaconf == null) {
                    feaconf = wfsParserConfigs.getFeatureTypeConfig("default");
                }

                if (feaconf == null) {
                    throw new ActionException(ERROR_FE_PARSER_CONFIG_MISSING);
                }
            } catch (Exception e) {
                if (e instanceof ActionException) {
                    throw (ActionException) e;
                } else {
                    throw new ActionException(ERROR_FE_PARSER_CONFIG_MISSING, e);
                }

            }
        }

    }
    private boolean handleWMSSpecific(final ActionParameters params, OskariLayer ml) throws ActionException {

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
        
        ml.setDownloadServiceUrl(params.getHttpParam("downloadServiceUrl"));
        ml.setCopyrightInfo(params.getHttpParam("copyrightInfo"));
    }

    private boolean handleWMTSSpecific(final ActionParameters params, OskariLayer ml) throws ActionException {
        ml.setTileMatrixSetId(params.getHttpParam("matrixSetId", ml.getTileMatrixSetId()));

        try {
            OskariLayerCapabilities capabilities = capabilitiesService.getCapabilities(ml, true);
            // flush cache, otherwise only db is updated but code retains the old cached version
            WebMapServiceFactory.flushCache(ml.getId());
            // parse capabilities
            WMTSCapabilities caps = new WMTSCapabilitiesParser().parseCapabilities(capabilities.getData());
            if (caps == null) {
                throw new ServiceException("Couldn't parse capabilities for service!");
            }
            WMTSCapabilitiesLayer layer = caps.getLayer(ml.getName());
            ResourceUrl resUrl = layer.getResourceUrlByType("tile");
            if(resUrl != null) {
                JSONHelper.putValue(ml.getOptions(), "requestEncoding", "REST");
                JSONHelper.putValue(ml.getOptions(), "format", resUrl.getFormat());
                JSONHelper.putValue(ml.getOptions(), "urlTemplate", resUrl.getTemplate());
            }
            return true;

        } catch (Exception ex) {
            LOG.error(ex, "Couldn't update capabilities for layer", ml);
            return false;
        }
    }

    private void handleWFSSpecific(final ActionParameters params, OskariLayer ml) throws ActionException {
        // these are only in insert
        ml.setSrs_name(params.getHttpParam("srs_name", ml.getSrs_name()));
        ml.setVersion(params.getHttpParam("WFSVersion",ml.getVersion()));

        // Put manual Refresh mode to attributes if true
        JSONObject attributes = ml.getAttributes();
        attributes.remove("manualRefresh");
        if(ConversionHelper.getOnOffBoolean(params.getHttpParam("manualRefresh", "off"), false)){
            JSONHelper.putValue(attributes, "manualRefresh", true);
            ml.setAttributes(attributes);
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

    private void addPermissionsForRoles(final OskariLayer ml, final User user, final String[] externalIds) {

        OskariLayerResource res = new OskariLayerResource(ml);
        // insert permissions
        for (String externalId : externalIds) {
            final long extId = ConversionHelper.getLong(externalId, -1);
            if (extId != -1 && user.hasRoleWithId(extId)) {
                Permission permission = new Permission();
                permission.setExternalType(Permissions.EXTERNAL_TYPE_ROLE);
                permission.setExternalId(externalId);
                permission.setType(Permissions.PERMISSION_TYPE_VIEW_LAYER);
                res.addPermission(permission);

                permission = new Permission();
                permission.setExternalType(Permissions.EXTERNAL_TYPE_ROLE);
                permission.setExternalId(externalId);
                permission.setType(Permissions.PERMISSION_TYPE_EDIT_LAYER);
                res.addPermission(permission);
            }
        }
        permissionsService.saveResourcePermissions(res);

    }

    private void addPermissionsForRoles(final OskariLayer ml,
                                        final Set<Long> externalIds,
                                        final Set<Long> publishRoleIds,
                                        final Set<Long> downloadRoleIds,
                                        final Set<Long> viewEmbeddedRoleIds) {

        OskariLayerResource res = new OskariLayerResource(ml);
        // insert permissions
        LOG.debug("Adding permission", Permissions.PERMISSION_TYPE_VIEW_LAYER, "for roles:", externalIds);
        for (long externalId : externalIds) {
            Permission permission = new Permission();
            permission.setExternalType(Permissions.EXTERNAL_TYPE_ROLE);
            permission.setExternalId(Long.toString(externalId));
            permission.setType(Permissions.PERMISSION_TYPE_VIEW_LAYER);
            res.addPermission(permission);

            permission = new Permission();
            permission.setExternalType(Permissions.EXTERNAL_TYPE_ROLE);
            permission.setExternalId(Long.toString(externalId));
            permission.setType(Permissions.PERMISSION_TYPE_EDIT_LAYER);
            res.addPermission(permission);
        }

        LOG.debug("Adding permission", Permissions.PERMISSION_TYPE_PUBLISH, "for roles:", publishRoleIds);
        for (long externalId : publishRoleIds) {
            Permission permission = new Permission();
            permission.setExternalType(Permissions.EXTERNAL_TYPE_ROLE);
            permission.setExternalId(Long.toString(externalId));
            permission.setType(Permissions.PERMISSION_TYPE_PUBLISH);
            res.addPermission(permission);
        }

        LOG.debug("Adding permission", Permissions.PERMISSION_TYPE_DOWNLOAD, "for roles:", downloadRoleIds);
        for (long externalId : downloadRoleIds) {
            Permission permission = new Permission();
            permission.setExternalType(Permissions.EXTERNAL_TYPE_ROLE);
            permission.setExternalId(Long.toString(externalId));
            permission.setType(Permissions.PERMISSION_TYPE_DOWNLOAD);
            res.addPermission(permission);
        }

        LOG.debug("Adding permission", Permissions.PERMISSION_TYPE_VIEW_PUBLISHED, "for roles:", viewEmbeddedRoleIds);
        for (long externalId : viewEmbeddedRoleIds) {
            Permission permission = new Permission();
            permission.setExternalType(Permissions.EXTERNAL_TYPE_ROLE);
            permission.setExternalId(Long.toString(externalId));
            permission.setType(Permissions.PERMISSION_TYPE_VIEW_PUBLISHED);
            res.addPermission(permission);
        }

        permissionsService.saveResourcePermissions(res);
    }
}
