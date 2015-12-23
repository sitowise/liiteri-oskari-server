package fi.nls.oskari.control.layer;

import pl.sito.liiteri.arcgis.domain.ArcgisMapServerConfiguration;
import pl.sito.liiteri.map.arcgislayer.service.ArcgisLayerConfigurationService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.AuthorizedActionHandler;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetArcgisMapServerConfiguration")
public class GetArcgisMapServerConfigurationHandler extends AuthorizedActionHandler {

    private static final Logger log = LogFactory.getLogger(GetArcgisMapServerConfigurationHandler.class);
    private static final String PARM_SERVERURL = "mapServerUrl";
    private final ArcgisLayerConfigurationService arcgisConfService = ArcgisLayerConfigurationService.getInstance();

    @Override
    public void handleAuthorizedAction(ActionParameters params) throws ActionException {

        final String serverUrl = params.getRequiredParam(PARM_SERVERURL);
                
        try {        	
        	ArcgisMapServerConfiguration conf = arcgisConfService.getMapServerConfiguration(serverUrl);        
        	
            ResponseHelper.writeResponseAsJson(params, conf.getAsJSON());
        } catch (Exception ee) {
            throw new ActionException("Cannot get Arcgis Map Server configuration: ", ee);
        }
    }
}
