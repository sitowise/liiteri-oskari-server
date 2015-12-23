package fi.nls.oskari.map.userowndata;

import java.io.File;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.UserGisDataRoleSettings;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseService;

public interface GisDataRoleSettingsDbService extends BaseService<UserGisDataRoleSettings>{
	
	public boolean canUserAddNewDataset(User user) throws ServiceException;
	
	public boolean canUserAddNewDataset(User user, float fileSize) throws ServiceException;
	
}