package fi.nls.oskari.control.data;

import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.domain.map.userlayer.UserLayer;
import fi.nls.oskari.map.userlayer.service.UserLayerDbService;
import fi.nls.oskari.map.userlayer.service.UserLayerDbServiceIbatisImpl;
import fi.nls.oskari.map.userowndata.GisDataDbService;
import fi.nls.oskari.map.userowndata.GisDataDbServiceImpl;
import fi.nls.oskari.permission.domain.Resource;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;
import fi.nls.oskari.util.ServiceFactory;


/**
 * Deletes user layer and its style if it belongs to current user.
 * Expects to get layer id as http parameter "id".
 */
@OskariActionRoute("DeleteUserLayer")
public class DeleteUserLayerHandler extends ActionHandler {

    private final static String PARAM_ID = "id";
    //private final static Logger log = LogFactory.getLogger(DeleteAnalysisDataHandler.class);

    private UserLayerDbService userLayerDbService = null;
    private static final GisDataDbService gisDataService = new GisDataDbServiceImpl();

    public void setUserLayerDbService(final UserLayerDbService service) {
        userLayerDbService = service;
    }

    @Override
    public void init() {
        super.init();
        if(userLayerDbService == null) {
            setUserLayerDbService(new UserLayerDbServiceIbatisImpl());
        }
    }

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        if(params.getUser().isGuest()) {
            throw new ActionDeniedException("Session expired");
        }
        final long id = ConversionHelper.getLong(params.getHttpParam(PARAM_ID), -1);
        if(id == -1) {
            throw new ActionParamsException("Parameter missing or non-numeric: " + PARAM_ID + "=" + params.getHttpParam(PARAM_ID));
        }

        final UserLayer userLayer = userLayerDbService.getUserLayerById(id);
        if(userLayer == null) {
            throw new ActionParamsException("User layer id didn't match any user layer: " + id);
        }
        if(!userLayer.isOwnedBy(params.getUser().getUuid())) {
            throw new ActionDeniedException("User layer belongs to another user");
        }

        try {
            // remove userLayer
            userLayerDbService.deleteUserLayer(userLayer);
            
            String dataId = "userlayer_" + userLayer.getId();
            //remove rows related to this analysis in oskari_user_gis_data table
            gisDataService.deleteGisData(dataId, "IMPORTED_PLACES", params.getUser().getId(), "false");
            
            // write static response to notify success {"result" : "success"}
            ResponseHelper.writeResponse(params, JSONHelper.createJSONObject("result", "success"));
        } catch (ServiceException ex) {
            throw new ActionException("Error deleting userLayer", ex);
        }
    }
}