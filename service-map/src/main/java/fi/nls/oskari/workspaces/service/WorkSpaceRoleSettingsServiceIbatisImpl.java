package fi.nls.oskari.workspaces.service;

import fi.nls.oskari.domain.workspaces.WorkSpaceRoleSettings;
import fi.nls.oskari.domain.workspaces.WorkSpaceSharing;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseIbatisService;

public class WorkSpaceRoleSettingsServiceIbatisImpl extends
		BaseIbatisService<WorkSpaceRoleSettings> implements
		WorkSpaceRoleSettingsDbService {

	@Override
	public Integer getMaxWorkSpaceAmount(long userId) throws ServiceException {
		Integer result = null;
				
		try {
			result = ((Integer) queryForObject(getNameSpace()
					+ ".getMaxWorkSpaceAmount", userId));
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting maximum workspaces amount for user:"
							+ userId, e);
		}
		
		if (result == null) {
			result = Integer.MAX_VALUE;
		}
		
		return result;
	}

	@Override
	public Integer getMaxExpirationDateLimit(long userId) throws ServiceException {
		try {
			return ((Integer) queryForObject(getNameSpace()
					+ ".getMaxExpirationDateLimit", userId));
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting maximum epiration date days limit  for user:"
							+ userId, e);
		}
	}

	@Override
	protected String getNameSpace() {
		return "WorkSpaceRoleSettings";
	}

}
