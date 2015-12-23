package fi.mml.map.mapwindow.service.db;

import fi.nls.oskari.domain.map.CapabilitiesCache;
import fi.nls.oskari.domain.map.UserWmsLayer;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseIbatisService;

public class CapabilitiesCacheServiceIbatisImpl  extends BaseIbatisService<CapabilitiesCache> implements CapabilitiesCacheService {

	@Override
	protected String getNameSpace() {
		
		return "CapabilitiesCache";
	}

	@Override
	public CapabilitiesCache findByLayer(CapabilitiesCache cc) throws ServiceException {
		try {
			return (CapabilitiesCache)queryForObject(getNameSpace() + ".findByLayer", cc);
		} catch (Exception e) {
			throw new ServiceException("Error when getting user's own WMS layer", e);
		}
	}

}
