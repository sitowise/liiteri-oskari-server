package fi.nls.oskari.control.layer;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.wms.GetGtWMSCapabilities;
import fi.nls.oskari.util.ResponseHelper;
import org.json.JSONObject;


/**
 * Get WMS capabilites and return JSON
 */
@OskariActionRoute("GetWSCapabilities")
public class GetWSCapabilitiesHandler extends ActionHandler {

    private static final Logger log = LogFactory.getLogger(GetWSCapabilitiesHandler.class);
    private static final String PARM_WMSURL = "wmsurl";

    public void handleAction(ActionParameters params) throws ActionException {

        final String wmsurl = params.getRequiredParam(PARM_WMSURL);

        if (params.getUser().isGuest()) {
            throw new ActionDeniedException("Unauthorized user tried to get wmsservices");
        }
        try {
            // New method for parsing WMSCetGapabilites to Oskari layers structure
            final JSONObject capabilities = GetGtWMSCapabilities.getWMSCapabilities(wmsurl);
            ResponseHelper.writeResponse(params, capabilities);
        } catch (Exception ee) {
            throw new ActionException("WMS Capabilities parsing failed: ", ee);
        }
    }
}
