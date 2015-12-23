package fi.nls.oskari.map.userowndata;

import java.io.File;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.UserGisDataRoleSettings;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseIbatisService;

public class GisDataRoleSettingsDbServiceImpl extends BaseIbatisService<UserGisDataRoleSettings> implements GisDataRoleSettingsDbService{

	private static final Logger log = LogFactory
            .getLogger(GisDataRoleSettingsDbServiceImpl.class);
	private final GisDataDbService gisDataService = new GisDataDbServiceImpl();
	
	@Override
	protected String getNameSpace() {
		return "UserGisDataRoleSettings";
	}
	
	/**
     * Check if user has not reached the limits and can add new dataset.
     * @param user current logged user
     * @return true - if user can add new dataset; false - otherwise
     */
	@Override
    public boolean canUserAddNewDataset(User user) {
    	try {
			Integer amountLimit = getDatasetAmountLimit(user.getId());
			if (amountLimit == null) {
				amountLimit = 0; //without limit
			}
			
			long allVisibleDatasetsAmount = gisDataService.getVisibleGisDatasetsAmount(user.getId());
			if (allVisibleDatasetsAmount < amountLimit || amountLimit == 0) {
				return true;
			}
		} catch (ServiceException e) {
			log.error("Could not check limitations", e);
		}
    	return false;
    }
	
	private Integer getDatasetMaxSizeInMB(long userId) throws ServiceException {
		try {
			return (Integer) queryForObject(getNameSpace() + ".getDatasetMaxSizeInMB", userId);
		} catch (Exception e) {
			throw new ServiceException("Error during getting user GIS dataset max size in MB for user " + userId, e);
		}
	}

	private Integer getDatasetAmountLimit(long userId) throws ServiceException {
		try {
			return (Integer) queryForObject(getNameSpace() + ".getDatasetAmountLimit", userId);
		} catch (Exception e) {
			throw new ServiceException("Error during getting user GIS dataset amount limit for user " + userId, e);
		}
	}

	@Override
	public boolean canUserAddNewDataset(User user, float fileSize)
			throws ServiceException {
		if (canUserAddNewDataset(user)) {
			Integer maxSizeMB = getDatasetMaxSizeInMB(user.getId());
			
			if (maxSizeMB != null && fileSize > maxSizeMB) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}


}
