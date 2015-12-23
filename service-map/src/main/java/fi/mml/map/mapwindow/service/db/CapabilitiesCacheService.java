package fi.mml.map.mapwindow.service.db;

import fi.nls.oskari.domain.map.CapabilitiesCache;
import fi.nls.oskari.domain.map.UserWmsLayer;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseService;

public interface CapabilitiesCacheService extends BaseService<CapabilitiesCache> {
	
	public CapabilitiesCache findByLayer(CapabilitiesCache cc) throws ServiceException;

}
