package fi.nls.oskari.proxy;

import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.analysis.service.AnalysisDbService;
import fi.nls.oskari.map.analysis.service.AnalysisDbServiceMybatisImpl;
import fi.nls.oskari.service.ProxyServiceConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Custom modifier for wfsquery proxy service. Overrides getConfig and returns proxy config
 * based on wfs layer mapping to given id parameter (requires parameter wfs_layer_id to work).
 */
public class AnalysisProxyHandler extends ProxyServiceConfig {
    private static final Logger log = LogFactory.getLogger(AnalysisProxyHandler.class);
    private static final AnalysisDbService analysisService = new AnalysisDbServiceMybatisImpl();
    private static final String PARAM_ANALYSIS_ID = "wpsLayerId";

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

        final String requestedAnalysisId = params.getHttpParam(PARAM_ANALYSIS_ID);
        
        //check if data is shared with the user (then uuid filter is not needed)
        List<String> sharedAnalysisIds = getSharedAnalysisWpsLayerIds(params.getUser().getId());
        String authenticationFilter = "";
        if (!sharedAnalysisIds.contains(requestedAnalysisId))
        {
        	authenticationFilter = "(uuid='" + params.getUser().getUuid() + "'+OR+publisher_name+IS+NOT+NULL)+AND+";
        }
        
        //(uuid='d3a216dd-077d-44ce-b79a-adf20ca88367')
        final String userSpecificURL = getUrl() + authenticationFilter + "analysis_id=" + requestedAnalysisId;
        // setup user specific base url
        config.setUrl(userSpecificURL);
        return config;
    }

    /**
     * Converts result of getSharedAnalysisWpsLayerIds in format:
     * "analysis_string1_..._stringN_number1_..._numberN" (oskari_user_gis_data.data_id)
     * to format "numberN" to match requestedAnalysisId format that is also "number" (analysis.id)
     * @return
     */
    private List<String> getSharedAnalysisWpsLayerIds(long userId) {

        List<String> sharedAnalysisIds = analysisService.getSharedAnalysisIds(userId);
        List<String> getSharedAnalysisWpsLayerIds = new ArrayList<String>();

        for (String id : sharedAnalysisIds) {
            getSharedAnalysisWpsLayerIds.add(getIdFromDataIdString(id));
        }
        return getSharedAnalysisWpsLayerIds;
    }

    private String getIdFromDataIdString(String dataId) {
        int position = dataId.lastIndexOf('_');
        return dataId.substring(position + 1);
    }
}
