package fi.nls.oskari.control.layer;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.service.capabilities.CapabilitiesCacheService;
import fi.nls.oskari.service.capabilities.OskariLayerCapabilities;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;
import fi.nls.oskari.wfs.GetGtWFSCapabilities;
import fi.nls.oskari.wms.GetGtWMSCapabilities;
import fi.nls.oskari.wmts.WMTSCapabilitiesParser;
import fi.nls.oskari.wmts.domain.WMTSCapabilities;
import org.json.JSONObject;

/**
 * Get capabilites for layer and returns JSON formatted as Oskari layers
 */
@OskariActionRoute("GetWSCapabilities")
public class GetWSCapabilitiesHandler extends ActionHandler {

    private static final Logger log = LogFactory.getLogger(GetWSCapabilitiesHandler.class);
    private static final String PARM_URL = "url";
    private static final String PARM_TYPE = "type";
    private static final String PARM_VERSION = "version";
    private static final String PARM_USER = "user";
    private static final String PARM_PW = "pw";
    private static final String PARM_CRS = "crs";

    private final CapabilitiesCacheService capabilitiesService = OskariComponentManager.getComponentOfType(CapabilitiesCacheService.class);
    private String[] permittedRoles = new String[0];

    @Override
    public void init() {
        super.init();
        permittedRoles = PropertyUtil.getCommaSeparatedList("actionhandler.GetWSCapabilitiesHandler.roles");
    }

    public void handleAction(ActionParameters params) throws ActionException {

        if (params.getUser().isGuest()) {
            throw new ActionDeniedException("Unauthorized user tried to proxy via capabilities");
        }
        final String url = params.getRequiredParam(PARM_URL).trim();
        final String layerType = params.getHttpParam(PARM_TYPE, OskariLayer.TYPE_WMS);
        final String version = params.getHttpParam(PARM_VERSION, "");
        final String user = params.getHttpParam(PARM_USER, "");
        final String pw = params.getHttpParam(PARM_PW, "");
        final String currentCrs = params.getHttpParam(PARM_CRS, "EPSG:3067");

        log.debug("Trying to get capabilities for type:", layerType, "with url:", url);
        try {
            if(OskariLayer.TYPE_WMS.equals(layerType)) {
                // New method for parsing WMSCetGapabilites to Oskari layers structure
                final JSONObject capabilities = GetGtWMSCapabilities.getWMSCapabilities(url, user, pw, version, currentCrs);
                ResponseHelper.writeResponse(params, capabilities);
            }
            else {
                if (OskariLayer.TYPE_WMTS.equals(layerType)) {
                    // setup capabilities URL
                    OskariLayerCapabilities caps  = capabilitiesService.getCapabilities(url, OskariLayer.TYPE_WMTS, user, pw, version);
                    String capabilitiesXML = caps.getData();
                    if(capabilitiesXML == null || capabilitiesXML.trim().isEmpty()) {
                        // retry from service - might get empty xml from db
                        caps = capabilitiesService.getCapabilities(url, OskariLayer.TYPE_WMTS, user, pw, version, true);
                        capabilitiesXML = caps.getData();
                    }
                    WMTSCapabilities wmtsCaps = WMTSCapabilitiesParser.parseCapabilities(capabilitiesXML);
                    JSONObject resultJSON = WMTSCapabilitiesParser.asJSON(wmtsCaps, url, currentCrs);
                    JSONHelper.putValue(resultJSON, "xml", caps.getData());
                    ResponseHelper.writeResponse(params, resultJSON);
                }
                else if(OskariLayer.TYPE_WFS.equals(layerType)) {
                    // New method for parsing WFSCetGapabilites to Oskari layers structure
                    final JSONObject capabilities = GetGtWFSCapabilities.getWFSCapabilities(url, version, user, pw, currentCrs);
                    ResponseHelper.writeResponse(params, capabilities);
                }
                else {
                    throw new ActionParamsException("Couldn't determine operation based on parameters");
                }
            }
        } catch (Exception ee) {
            throw new ActionException("WMS Capabilities parsing failed: ", ee);
        }
    }
}
