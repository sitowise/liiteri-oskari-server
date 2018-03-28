package fi.nls.oskari.proxy;

import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.userlayer.service.UserLayerDbService;
import fi.nls.oskari.map.userlayer.service.UserLayerDbServiceMybatisImpl;
import fi.nls.oskari.service.ProxyServiceConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Custom modifier for userlayer (user_data_store.vuser_data) proxy service. Overrides getConfig and returns proxy config
 * based on wfs layer mapping to given id parameter (requires parameter wfs_layer_id to work).
 */
public class UserLayerProxyHandler extends ProxyServiceConfig {
    private static final Logger log = LogFactory.getLogger(UserLayerProxyHandler.class);
    private static final UserLayerDbService userLayerService = new UserLayerDbServiceMybatisImpl();
    private static final String PARAM_USERLAYER_ID = "id";

    /**
     * Returns version of the config populated by wfs service info based on given id parameter.
     * @return
     */
    public ProxyServiceConfig getConfig(final ActionParameters params) {
        ProxyServiceConfig config = new ProxyServiceConfig();
        config.setEncoding(getEncoding());
        config.setUsername(getUsername());
        config.setPassword(getPassword());

        // passing all params - TODO: list allowed params in props
        final Enumeration<String> parmNames = params.getRequest().getParameterNames();
        final ArrayList<String> aList = Collections.list(parmNames);
        config.setParamNames(aList.toArray(new String[0]));
        config.setHeaders(getHeaders());

        final String requestedUserlayer = params.getHttpParam(PARAM_USERLAYER_ID);
        
        //check if data is shared with the user (then uuid filter is not needed)
        List<Long> sharedUserLayerIds = userLayerService.getSharedUserLayerIds(params.getUser().getId());
        String authenticationFilter = "";
        if (!sharedUserLayerIds.contains(Long.parseLong(requestedUserlayer)))
        {
        	authenticationFilter = "(uuid='" + params.getUser().getUuid() + "'+OR+publisher_name+IS+NOT+NULL)+AND+";
        }
        
        //(uuid='d3a216dd-077d-44ce-b79a-adf20ca88367')
        final String userSpecificURL = getUrl() + authenticationFilter + "user_layer_id=" + requestedUserlayer;
        // setup user specific base url
        config.setUrl(userSpecificURL);
        return config;
    }
}