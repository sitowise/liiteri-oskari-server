package fi.nls.oskari.control.layer;

import fi.mml.map.mapwindow.service.db.CapabilitiesCacheService;
import fi.mml.map.mapwindow.service.db.InspireThemeService;
import fi.mml.map.mapwindow.service.wms.WebMapServiceFactory;
import fi.mml.map.mapwindow.util.OskariLayerWorker;
import fi.mml.portti.domain.permissions.Permissions;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.CapabilitiesCache;
import fi.nls.oskari.domain.map.InspireTheme;
import fi.nls.oskari.domain.map.LayerGroup;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.wfs.WFSSLDStyle;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.LayerGroupService;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.userowndata.GisDataDbService;
import fi.nls.oskari.map.userowndata.GisDataDbServiceImpl;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.*;
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

    private OskariLayerService mapLayerService = ServiceFactory.getMapLayerService();
    private PermissionsService permissionsService = ServiceFactory.getPermissionsService();
    private LayerGroupService layerGroupService = ServiceFactory.getLayerGroupService();
    private InspireThemeService inspireThemeService = ServiceFactory.getInspireThemeService();
    private CapabilitiesCacheService capabilitiesService = ServiceFactory.getCapabilitiesCacheService();
    private final WFSLayerConfigurationService layerConfigurationService = new WFSLayerConfigurationServiceIbatisImpl();    
    private final GroupingsService groupingsService = GroupingsService.getInstance();
    private static final GisDataDbService gisDataService = new GisDataDbServiceImpl();
    private static final Logger log = LogFactory.getLogger(SaveLayerHandler.class);
    private static final String PARAM_LAYER_ID = "layer_id";
    private static final String PARAM_WMS_NAME = "wmsName";
    private static final String PARAM_WMS_URL = "wmsUrl";
    
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
            throw new ActionException("Couldn't get the saved layer from DB - id:" + layerId);
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
        if(!cacheUpdated && !ml.isCollection() && ml.getType().equals(OskariLayer.TYPE_WMS)) {
            // Cache update failed, no biggie
            JSONHelper.putValue(layerJSON, "warn", "metadataReadFailure");
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

    private int saveLayer(final ActionParameters params) throws ActionException {

        // layer_id can be string -> external id!
        final String layer_id = params.getHttpParam(PARAM_LAYER_ID);

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

                handleRequestToMapLayer(params, ml);

                ml.setUpdated(new Date(System.currentTimeMillis()));
                mapLayerService.update(ml);

                log.debug(ml);

                return ml.getId();
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
                handleRequestToMapLayer(params, ml);
                int id = mapLayerService.insert(ml);
                ml.setId(id);

                if(ml.isCollection()) {
                    // update the name with the id for permission mapping
                    ml.setName(ml.getId() + "_group");
                    mapLayerService.update(ml);
                }

                final String[] externalIds = params.getHttpParam("viewPermissions", "").split(",");

                addPermissionsForRoles(ml, params.getUser(), externalIds);
                
                int userThemeId = params.getHttpParam("userThemeId", -1);
                if (userThemeId != -1)
                	mapToUserTheme(ml, params.getUser(), userThemeId);

                // update keywords
                GetLayerKeywords glk = new GetLayerKeywords();
                glk.updateLayerKeywords(id, ml.getMetadataId());

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

    private void handleRequestToMapLayer(final ActionParameters params, OskariLayer ml) throws ActionException {

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

    private void addPermissionsForRoles(final OskariLayer ml, final User user, final String[] externalIds) {

        final Permissions permissions = new Permissions();
        permissions.getUniqueResourceName().setType(Permissions.RESOURCE_TYPE_MAP_LAYER);
        permissions.getUniqueResourceName().setNamespace(ml.getUrl());
        permissions.getUniqueResourceName().setName(ml.getName());
        // insert permissions
        for (String externalId : externalIds) {
            final long extId = ConversionHelper.getLong(externalId, -1);
            if (extId != -1 && user.hasRoleWithId(extId)) {
                permissionsService.insertPermissions(permissions.getUniqueResourceName(), externalId, Permissions.EXTERNAL_TYPE_ROLE, Permissions.PERMISSION_TYPE_VIEW_LAYER);
                permissionsService.insertPermissions(permissions.getUniqueResourceName(), externalId, Permissions.EXTERNAL_TYPE_ROLE, Permissions.PERMISSION_TYPE_EDIT_LAYER);
            }
        }

    }
}
