package fi.nls.oskari.workspaces.service;

import java.util.Date;
import java.util.List;

import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseService;

public interface WorkSpaceDbService extends BaseService<WorkSpace> {
	
	public WorkSpace getHiddenWorkSpaceById(
			long workSpaceId) throws ServiceException;
	
	public List<WorkSpace> getWorkSpacesAfterExpirationDate(
			Date expirationDate, String status) throws ServiceException;

	public List<WorkSpace> getUserWorkSpaces(long userId, Date expirationDate)
			throws ServiceException;
	
	public List<WorkSpace> getUserVisibleWorkSpaces(long userId, Date expirationDate)
			throws ServiceException;

	public List<WorkSpace> getUserExternalWorkSpaces(long userId,
			Date expirationDate) throws ServiceException;
	
	public long getVisibleWorkSpacesAmount(long userId) throws ServiceException;

}
