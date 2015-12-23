package fi.nls.oskari.workspaces.service;

import java.util.List;

import fi.nls.oskari.domain.workspaces.WorkSpaceRoleSettings;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseService;

public interface WorkSpaceRoleSettingsDbService extends
		BaseService<WorkSpaceRoleSettings> {
	public Integer getMaxWorkSpaceAmount(long userId) throws ServiceException;

	public Integer getMaxExpirationDateLimit(long userId) throws ServiceException;
}
