package fi.nls.oskari.control.layer;

import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceIbatisImpl;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.wfs.WFSLayerConfiguration;
import fi.nls.oskari.wfs.WFSLayerConfigurationService;
import fi.nls.oskari.wfs.WFSLayerConfigurationServiceIbatisImpl;

/**
 * Admin WMS layer delete for single layer, for base/group layers -> use DeleteOrganizationHandler
 * 
 * 
 */
@OskariActionRoute("DeleteLayer")
public class DeleteLayerHandler extends ActionHandler {

    private static final Logger log = LogFactory.getLogger(DeleteLayerHandler.class);
    private static final OskariLayerService mapLayerService = new OskariLayerServiceIbatisImpl();
    private PermissionsService permissionsService = new PermissionsServiceIbatisImpl();
    
    private final WFSLayerConfigurationService layerConfigurationService = new WFSLayerConfigurationServiceIbatisImpl();    

    private static final String PARAM_LAYER_ID = "layer_id";

    public void handleAction(ActionParameters params) throws ActionException {

        final String id = params.getRequiredParam(PARAM_LAYER_ID);
        final OskariLayer layer = mapLayerService.find(id);
        if(layer == null) {
            throw new ActionParamsException("Layer not found - id: " + id);
        }

        if(!permissionsService.hasEditPermissionForLayerByLayerId(params.getUser(), layer.getId())) {
            throw new ActionDeniedException("Unauthorized user tried to remove layer - id: " + layer.getId());
        }                

        try {
        	WFSLayerConfiguration configuration = layerConfigurationService.findConfiguration(layer.getId());
        	if (configuration != null) {
        		layerConfigurationService.delete(layer.getId());
        		configuration.destroy();            		
        	}
        	
            mapLayerService.delete(layer.getId());
        } catch (Exception e) {
            throw new ActionException("Couldn't delete map layer - id:" + layer.getId(), e);
        }           	
    	

    }

}
