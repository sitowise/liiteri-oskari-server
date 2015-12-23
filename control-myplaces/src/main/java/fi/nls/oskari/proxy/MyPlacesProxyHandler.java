package fi.nls.oskari.proxy;

import fi.mml.map.mapwindow.service.db.MyPlacesService;
import fi.mml.map.mapwindow.service.db.MyPlacesServiceIbatisImpl;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.map.MyPlaceCategory;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.analysis.service.AnalysisDbService;
import fi.nls.oskari.map.analysis.service.AnalysisDbServiceIbatisImpl;
import fi.nls.oskari.service.ProxyServiceConfig;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.wfs.WFSLayerConfiguration;
import fi.nls.oskari.wfs.WFSLayerConfigurationService;
import fi.nls.oskari.wfs.WFSLayerConfigurationServiceIbatisImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Custom modifier for wfsquery proxy service. Overrides getConfig and returns proxy config
 * based on wfs layer mapping to given id parameter (requires parameter wfs_layer_id to work).
 */
public class MyPlacesProxyHandler extends ProxyServiceConfig {
    private static final Logger log = LogFactory.getLogger(MyPlacesProxyHandler.class);
    private static final MyPlacesService myPlacesService = new MyPlacesServiceIbatisImpl();
    private static final String PARAM_CATEGORY_ID = "myCat";

    /**
     * Returns version of the config populated by wfs service info based on given id parameter.
     * @return
     */
    public ProxyServiceConfig getConfig(final ActionParameters params) {
        ProxyServiceConfig config = new ProxyServiceConfig();
        config.setEncoding(getEncoding());

        // passing all params - TODO: list allowed params in props
        final Enumeration<String> parmNames = params.getRequest().getParameterNames();
        final ArrayList<String> aList = Collections.list(parmNames);
        config.setParamNames(aList.toArray(new String[0]));
        config.setHeaders(getHeaders());

        final String requestedCategory = params.getHttpParam(PARAM_CATEGORY_ID);
        
        //check if data is shared with the user (then uuid filter is not needed)
        List<UserGisData> sharedMyPlaceCategories = myPlacesService.getSharedMyPlaceLayers(params.getUser().getId());
        String authenticationFilter = "(uuid='" + params.getUser().getUuid() + "'+OR+publisher_name+IS+NOT+NULL)+AND+";
        for (UserGisData mpc : sharedMyPlaceCategories) {
        	if (mpc.getDataId().equals(requestedCategory)) {
        		authenticationFilter = "";
        		break;
        	}
        }
        
        //(uuid='d3a216dd-077d-44ce-b79a-adf20ca88367'+OR+publisher_name+IS+NOT+NULL)
        final String userSpecificURL = getUrl() + authenticationFilter + "category_id=" + requestedCategory;
        // setup user specific base url
        config.setUrl(userSpecificURL);
        return config;
    }
}
