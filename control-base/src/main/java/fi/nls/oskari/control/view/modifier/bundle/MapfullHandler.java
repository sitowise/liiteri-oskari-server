package fi.nls.oskari.control.view.modifier.bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fi.mml.map.mapwindow.util.OskariLayerWorker;
import fi.mml.portti.domain.permissions.Permissions;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.analysis.AnalysisHelper;
import fi.nls.oskari.annotation.OskariViewModifier;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.map.analysis.Analysis;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.map.analysis.domain.AnalysisLayer;
import fi.nls.oskari.map.analysis.service.AnalysisDbService;
import fi.nls.oskari.map.analysis.service.AnalysisDbServiceIbatisImpl;
import fi.nls.oskari.map.layer.UserWmsLayerService;
import fi.nls.oskari.map.layer.UserWmsLayerServiceIbatisImpl;
import fi.nls.oskari.map.layer.formatters.LayerJSONFormatter;
import fi.nls.oskari.view.modifier.ModifierException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.mml.map.mapwindow.service.db.MyPlacesService;
import fi.mml.map.mapwindow.service.db.MyPlacesServiceIbatisImpl;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.MyPlaceCategory;
import fi.nls.oskari.domain.map.UserWmsLayer;
import fi.nls.oskari.domain.map.view.ViewTypes;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.view.modifier.ModifierParams;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.util.JSONHelper;

@OskariViewModifier("mapfull")
public class MapfullHandler extends BundleHandler {

    private static final Logger log = LogFactory.getLogger(MapfullHandler.class);
    private static PermissionsService permissionsService = new PermissionsServiceIbatisImpl();

    private static final String KEY_LAYERS = "layers";
    private static final String KEY_SEL_LAYERS = "selectedLayers";
    private static final String KEY_ID = "id";

    private static final String KEY_USER = "user";
    private static final String KEY_FIRSTNAME = "firstName";
    private static final String KEY_LASTNAME = "lastName";
    private static final String KEY_LOGINNAME = "loginName";
    private static final String KEY_NICKNAME = "nickName";
    private static final String KEY_USERUUID = "userUUID";
    private static final String KEY_USERID = "userID";
    private static final String KEY_TOSACCEPTED = "tosAccepted";


    private final static String KEY_ROLE_ID = "id";
    private final static String KEY_ROLE_NAME = "name";
    private final static String KEY_ROLES = "roles";

    
    private static final String KEY_PLUGINS = "plugins";
    public static final String KEY_CONFIG = "config";
    private static final String KEY_BASELAYERS = "baseLayers";

    private static final String PREFIX_MYPLACES = "myplaces_";
    private static final String PREFIX_ANALYSIS = "analysis_";
    private static final String PREFIX_USERWMS = "userwms_";

    private static final String PLUGIN_LAYERSELECTION = "Oskari.mapframework.bundle.mapmodule.plugin.LayerSelectionPlugin";
    private static final String PLUGIN_GEOLOCATION = "Oskari.mapframework.bundle.mapmodule.plugin.GeoLocationPlugin";
    public static final String PLUGIN_SEARCH = "Oskari.mapframework.bundle.mapmodule.plugin.SearchPlugin";

    private static final MyPlacesService myPlaceService = new MyPlacesServiceIbatisImpl();
    private static final AnalysisDbService analysisService = new AnalysisDbServiceIbatisImpl();
    private static final UserWmsLayerService userWmsLayerService = new UserWmsLayerServiceIbatisImpl();

    private final static LayerJSONFormatter FORMATTER = new LayerJSONFormatter();
    
    public boolean modifyBundle(final ModifierParams params) throws ModifierException {

        final JSONObject mapfullConfig = getBundleConfig(params.getConfig());
        final JSONObject mapfullState = getBundleState(params.getConfig());
        
        if(mapfullConfig == null) {
            return false;
        }
        // setup correct ajax url
        final String ajaxUrl = mapfullConfig.optString("globalMapAjaxUrl");
        try {
            // fix ajaxurl to current community if possible 
            // (required to show correct help articles)
            mapfullConfig.put("globalMapAjaxUrl", params.getBaseAjaxUrl());
            log.debug("Replaced ajax url: ", ajaxUrl, "->", params.getBaseAjaxUrl());
        } catch (Exception e) {
            log.error(e, "Replacing ajax url failed: ", ajaxUrl, "- Parsed:",
                    params.getBaseAjaxUrl());
        }

        // setup user data
        JSONHelper.putValue(mapfullConfig, KEY_USER, getUserJSON(params.getUser()));

        // Any layer referenced in state.selectedLayers array NEEDS to 
        // be in conf.layers otherwise it cant be added to map on startup
        final JSONArray mfConfigLayers = JSONHelper.getEmptyIfNull(mapfullConfig.optJSONArray(KEY_LAYERS));
        final JSONArray mfStateLayers = JSONHelper.getEmptyIfNull(mapfullState.optJSONArray(KEY_SEL_LAYERS));
        copySelectedLayersToConfigLayers(mfConfigLayers, mfStateLayers);
        final Set<String> bundleIds = getBundleIds(params.getStartupSequence());
        final boolean useDirectURLForMyplaces = false;
        final JSONArray fullConfigLayers = getFullLayerConfig(mfConfigLayers,
        		params.getUser(), 
        		params.getLocale().getLanguage(),
        		params.getViewId(), 
        		params.getViewType(),
                bundleIds,
                useDirectURLForMyplaces,
        		params.isModifyURLs());
        
        // overwrite layers
        try {
            mapfullConfig.put(KEY_LAYERS, fullConfigLayers);
        } catch (Exception e) {
            log.error(e, "Unable to overwrite layers");
        }
        
        // dummyfix: because migration tool added layer selection to all migrated maps
        // remove it from old published maps if only one layer is selected
        if(params.isOldPublishedMap()) {
            this.killLayerSelectionPlugin(mapfullConfig);
        }

        if (params.isLocationModified()) {
            log.info("locationModifiedByParams -> disabling GeoLocationPlugin");
            removePlugin(PLUGIN_GEOLOCATION, mapfullConfig);
        }
        
        //Add statsgrid permissions here
        //Ugly, but we can't make BundleHandler for it because there already is a ParamHandler with same name
		
		final List<String> permissionsList = permissionsService.getResourcesWithGrantedPermissions(
                "operation", params.getUser(), Permissions.PERMISSION_TYPE_EXECUTE);

		boolean functionalIntersectionAllowed = permissionsList.contains("statistics+functional_intersection");
		boolean gridDataAllowed = permissionsList.contains("statistics+grid");
		
		try {
			if(params.getConfig().has(BUNDLE_STATSGRID)) {
				final JSONObject config = getBundleConfig(params.getConfig(), BUNDLE_STATSGRID);
				config.put("functionalIntersectionAllowed", functionalIntersectionAllowed);
                config.put("gridDataAllowed", gridDataAllowed);
			}
		} catch (JSONException e) {
			log.error(e, "Adding extra permissions failed");
		}
        
        return false;
    }

    public static JSONArray getFullLayerConfig(final JSONArray layersArray,
                                               final User user, final String lang, final long viewID,
                                               final String viewType, final Set<String> bundleIds,
                                               final boolean useDirectURLForMyplaces) {
        return getFullLayerConfig(layersArray, user, lang, viewID, viewType, bundleIds, useDirectURLForMyplaces, false);
    }

    /**
     * Creates JSON array of layer configurations.
     * @param layersArray
     * @param user
     * @param lang
     * @param viewID
     * @param viewType
     * @param bundleIds
     * @param useDirectURLForMyplaces
     * @param modifyURLs false to keep urls as is, true to modify them for easier proxy forwards
     * @return
     */
    public static JSONArray getFullLayerConfig(final JSONArray layersArray,
    		final User user, final String lang, final long viewID,
    		final String viewType, final Set<String> bundleIds,
            final boolean useDirectURLForMyplaces,
            final boolean modifyURLs) {

        // Create a list of layer ids
        final List<String> layerIdList = new ArrayList<String>();
        final List<Long> publishedMyPlaces = new ArrayList<Long>();
        final List<Long> publishedAnalysis = new ArrayList<Long>();
        final List<Long> publishedUserWms = new ArrayList<Long>();

        for (int i = 0; i < layersArray.length(); i++) {
            String layerId = null;
            try {
                final JSONObject layer = layersArray.getJSONObject(i);
                layerId = layer.getString(KEY_ID);
                if (layerId == null || layerIdList.contains(layerId)) {
                    continue;
                }
                // special handling for myplaces and analysis layers
                if (layerId.startsWith(PREFIX_MYPLACES)) {
                    final long categoryId =
                            ConversionHelper.getLong(layerId.substring(PREFIX_MYPLACES.length()), -1);
                    if (categoryId != -1) {
                        publishedMyPlaces.add(categoryId);
                    } else {
                        log.warn("Found my places layer in selected. Error parsing id with category id: ", layerId);
                    }
                } else if (layerId.startsWith(PREFIX_ANALYSIS)) {
                    final long categoryId = AnalysisHelper.getAnalysisIdFromLayerId(layerId);
                    if (categoryId != -1) {
                        publishedAnalysis.add(categoryId);
                    } else {
                        log.warn("Found analysis layer in selected. Error parsing id with category id: ", layerId);
                    }
                } else if (layerId.startsWith(PREFIX_USERWMS)) {
                    final long categoryId =
                            ConversionHelper.getLong(layerId.substring(PREFIX_USERWMS.length()), -1);
                    if (categoryId != -1) {
                        publishedUserWms.add(categoryId);
                    } else {
                        log.warn("Found analysis layer in selected. Error parsing id with category id: ", layerId);
                    }
                } else {
                    // these should all be pointing at a layer in oskari_maplayer
                    layerIdList.add(layerId);
                }
            } catch (JSONException je) {
                log.error(je, "Problem handling layer id:", layerId, "skipping it!.");
            }
        }

        final JSONObject struct = OskariLayerWorker.getListOfMapLayersById(
                layerIdList,user, lang, ViewTypes.PUBLISHED.equals(viewType), modifyURLs);

        if (struct.isNull(KEY_LAYERS)) {
            log.warn("getSelectedLayersStructure did not return layers when expanding:",
                    layerIdList);
        }

        // construct layers JSON
        final JSONArray prefetch = getLayersArray(struct);
        appendMyPlacesLayers(prefetch, publishedMyPlaces, user, viewID, lang, bundleIds, useDirectURLForMyplaces, modifyURLs);
        appendAnalysisLayers(prefetch, publishedAnalysis, user, viewID, lang, bundleIds, useDirectURLForMyplaces, modifyURLs);
        appendUserWmsLayers(prefetch, publishedUserWms, user, viewID, lang, bundleIds, useDirectURLForMyplaces, modifyURLs);
        return prefetch;
    }

    private static void appendUserWmsLayers(final JSONArray layerList,
                                             final List<Long> publishedUserWms,
                                             final User user,
                                             final long viewID,
                                             final String lang,
                                             final Set<String> bundleIds,
                                             final boolean useDirectURL,
                                             final boolean modifyURLs) {
        final boolean userLayerBundlePresent = bundleIds.contains(BUNDLE_USERWMS);
        if(userLayerBundlePresent) {
            // skip it's an own bundle and bundle is present -> will be loaded via bundle
            return;
        }
        
        for(Long id : publishedUserWms) {
            final UserWmsLayer userwms = userWmsLayerService.find(id);
            if (userwms.getUserId() != user.getId()) {
                log.info("Found user wms layer in selected that is no longer published. ViewID:",
                        viewID, "User wms id:", id);
                continue;
            }
            
            final JSONObject json = FORMATTER.getJSON(userwms, lang, false);
            try {
                long layerId = json.getLong("id");
                json.remove("id");
                json.put("id", "userwms_" + layerId);
            } catch (JSONException e) {
                log.info(e, "Can't modify user wms layer id, User wms id:", id);
                continue;
            }
            if(json != null) {
                layerList.put(json);
            }
        }
    }
    
    private static void appendAnalysisLayers(final JSONArray layerList,
                                             final List<Long> publishedAnalysis,
                                             final User user,
                                             final long viewID,
                                             final String lang,
                                             final Set<String> bundleIds,
                                             final boolean useDirectURL,
                                             final boolean modifyURLs) {

        final boolean analyseBundlePresent = bundleIds.contains(BUNDLE_ANALYSE);
        final List<String> permissionsList = permissionsService.getResourcesWithGrantedPermissions(
                AnalysisLayer.TYPE, user, Permissions.PERMISSION_TYPE_VIEW_PUBLISHED);
        log.debug("Analysis layer permissions for published view", permissionsList);

        for(Long id : publishedAnalysis) {
            final Analysis analysis = analysisService.getAnalysisById(id);
            if(analyseBundlePresent && analysis.isOwnedBy(user.getUuid())) {
                // skip it's an own bundle and analysis bundle is present -> will be loaded via analysisbundle
                continue;
            }
            final String permissionKey = "analysis+"+id;
            boolean containsKey = permissionsList.contains(permissionKey);
            if (!containsKey) {
                log.info("Found analysis layer in selected that is no longer published. ViewID:",
                        viewID, "Analysis id:", id);
                continue;
            }
            final JSONObject json = AnalysisHelper.getlayerJSON(analysis, lang,
                    useDirectURL, user.getUuid(), modifyURLs);
            if(json != null) {
                layerList.put(json);
            }
        }
    }

    
    private static void appendMyPlacesLayers(final JSONArray layerList,
            final List<Long> publishedMyPlaces,
            final User user,
            final long viewID,
            final String lang,
            final Set<String> bundleIds,
            final boolean useDirectURL,
            final boolean modifyURLs) {
        if (publishedMyPlaces.isEmpty()) {
            return;
        }
        final boolean myPlacesBundlePresent = bundleIds.contains(BUNDLE_MYPLACES2);
        // get myplaces categories from service and generate layer jsons
        final String uuid = user.getUuid();
        final List<MyPlaceCategory> myPlacesLayers = myPlaceService
                .getMyPlaceLayersById(publishedMyPlaces);

        for (MyPlaceCategory mpLayer : myPlacesLayers) {
            if (!mpLayer.isPublished() && !mpLayer.isOwnedBy(uuid)) {
                log.info("Found my places layer in selected that is no longer published. ViewID:",
                		viewID, "Myplaces layerId:", mpLayer.getId());
                // no longer published -> skip if isn't current users layer
                continue;
            }
            if (myPlacesBundlePresent && mpLayer.isOwnedBy(uuid)) {
                // if the layer is users own -> myplaces2 bundle handles it
                // so if myplaces2 is present we must skip the users layers
                continue;
            }

            final JSONObject myPlaceLayer = myPlaceService.getCategoryAsWmsLayerJSON(
                    mpLayer, lang, useDirectURL, user.getUuid(), modifyURLs);
            if(myPlaceLayer != null) {
                layerList.put(myPlaceLayer);
            }
        }
    }

    private static JSONArray getLayersArray(final JSONObject struct) {
        try {
            final Object layers = struct.get(KEY_LAYERS);
            if (layers instanceof JSONArray) {
                return (JSONArray) layers;
            } else if (layers instanceof JSONObject) {
                final JSONArray list = new JSONArray();
                list.put(layers);
                return list;
            } else {
                log.error("getSelectedLayersStructure returned garbage layers.");
            }
        } catch (JSONException jsonex) {
            log.error("Could not set prefetch layers.");
        }
        return new JSONArray();
    }

    private void copySelectedLayersToConfigLayers(final JSONArray mfConfigLayers,
            final JSONArray mfStateLayers) {
        for (int i = 0; i < mfStateLayers.length(); i++) {
            String stateLayerId = null;
            String confLayerId = null;
            JSONObject stateLayer = null;
            JSONObject confLayer = null;
            try {
                boolean inConfigLayers = false;
                stateLayer = mfStateLayers.getJSONObject(i);
                stateLayerId = stateLayer.getString(KEY_ID);

                for (int j = 0; j < mfConfigLayers.length(); j++) {
                    confLayer = mfConfigLayers.getJSONObject(j);
                    confLayerId = confLayer.getString(KEY_ID);
                    if (stateLayerId.equals(confLayerId)) {
                        inConfigLayers = true;
                    }
                }
                if (!inConfigLayers) {
                    mfConfigLayers.put(stateLayer);
                }
            } catch (JSONException je) {
                log.error(je, "Problem comparing layers - StateLayerId:",
                        stateLayerId, "vs confLayerId:", confLayerId);
            }
        }
    }

    private JSONObject getUserJSON(final User user) {
        try {
            JSONObject userData = new JSONObject();
            userData.put(KEY_FIRSTNAME, user.getFirstname());
            userData.put(KEY_LASTNAME, user.getLastname());
            userData.put(KEY_LOGINNAME, user.getEmail());
            userData.put(KEY_NICKNAME, user.getScreenname());
            userData.put(KEY_USERUUID, user.getUuid());
            userData.put(KEY_USERID, user.getId());
            userData.put(KEY_TOSACCEPTED, user.getTosAccepted());

            JSONArray roles = getUserRolesJSON(user);
            userData.put(KEY_ROLES, roles);
            return userData;
        } catch (JSONException jsonex) {
            log.warn("Unable to populate user data:", user);
        }
        return null;
    }

    private JSONArray getUserRolesJSON(final User user) throws JSONException {
        JSONArray userRoles = new JSONArray();
        for (Role role: user.getRoles()) {
            JSONObject roleData = new JSONObject();
            roleData.put(KEY_ROLE_ID, role.getId());
            roleData.put(KEY_ROLE_NAME, role.getName());
            userRoles.put(roleData);
        }
        return userRoles;
    }

    public static JSONObject getPlugin(final String pluginClassName,
                              final JSONObject mapfullConfig) {

        if (mapfullConfig == null || !mapfullConfig.has(KEY_PLUGINS)) {
            return null;
        }
        final JSONArray plugins = mapfullConfig.optJSONArray(KEY_PLUGINS);
        for (int i = 0; i < plugins.length(); i++) {
            final JSONObject plugin = plugins.optJSONObject(i);
            if (plugin == null || !plugin.has(KEY_ID)) {
                continue;
            }
            if (pluginClassName.equals(plugin.optString(KEY_ID))) {
                log.debug(pluginClassName, "plugin found at index:", i);
                return plugin;
            }
        }
        return null;
    }

    private void removePlugin(final String pluginClassName,
            final JSONObject mapfullConfig) {

        if (mapfullConfig == null || !mapfullConfig.has(KEY_PLUGINS)) {
            return;
        }
        final JSONArray plugins = mapfullConfig.optJSONArray(KEY_PLUGINS);
        for (int i = 0; i < plugins.length(); i++) {
            final JSONObject plugin = plugins.optJSONObject(i);
            if (plugin == null || !plugin.has(KEY_ID)) {
                continue;
            }
            if (pluginClassName.equals(plugin.optString(KEY_ID))) {
                log.debug(pluginClassName, "plugin found at index:", i, "- removing it");
                plugins.remove(i);
                break;
            }
        }
    }

    private void killLayerSelectionPlugin(final JSONObject mapfullConfig) {
        log.debug("[killLayerSelectionPlugin] removing layer selection plugin");
        try {
            final JSONArray plugins = mapfullConfig.getJSONArray(KEY_PLUGINS);
            for (int i = 0; i < plugins.length(); i++) {
                JSONObject plugin = plugins.getJSONObject(i);
                if (!plugin.has(KEY_ID) || !plugin.has(KEY_CONFIG)) {
                    continue;
                }
                String id = plugin.getString(KEY_ID);
                log.debug("[killLayerSelectionPlugin] got plugin " + id);
                if (!id.equals(PLUGIN_LAYERSELECTION)) {
                    continue;
                }
                JSONObject config = plugin.getJSONObject(KEY_CONFIG);
                log.debug("[killLayerSelectionPlugin] got config");
                if (!config.has(KEY_BASELAYERS)) {
                    continue;
                }
                JSONArray bl = config.getJSONArray(KEY_BASELAYERS);
                if (bl.length() < 2) {
                    log.debug("[killLayerSelectionPlugin] "
                            + "layercount < 2, removing plugin");
                    plugins.remove(i--);
                    log.info("[killLayerSelectionPlugin] " + "Removed "
                            + PLUGIN_LAYERSELECTION
                            + "as layercount < 2 and oldId > 0");

                }
            }
        } catch (JSONException jsonex) {
            log.error("Problem trying to figure out whether "
                    + PLUGIN_LAYERSELECTION + " should be removed.", jsonex);
        }
    }    
}
