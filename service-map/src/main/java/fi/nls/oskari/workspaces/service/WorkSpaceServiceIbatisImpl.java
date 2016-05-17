package fi.nls.oskari.workspaces.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseIbatisService;

public class WorkSpaceServiceIbatisImpl extends BaseIbatisService<WorkSpace>
		implements WorkSpaceDbService {

	@Override
	protected String getNameSpace() {
		return "WorkSpaces";
	}
	
	@Override
	public WorkSpace getHiddenWorkSpaceById(
			long workSpaceId) throws ServiceException {

		try {
			WorkSpace workSpace = (WorkSpace)queryForObject(getNameSpace()
					+ ".getHiddenWorkSpaceById", workSpaceId);

			return workSpace;

		} catch (Exception e) {
			throw new ServiceException("Error getting workspaces", e);
		}
	}
	
	@Override
	public List<WorkSpace> getWorkSpacesAfterExpirationDate(
			Date expirationDate, String status) throws ServiceException {

		try {
			List<WorkSpace> workSpaces = queryForList(getNameSpace()
					+ ".getAllWorkSpaces");

			List<WorkSpace> workSpaceToReturn = new ArrayList<WorkSpace>();
			for (WorkSpace w : workSpaces) {
				if (status.equals(w.getStatus())
						&& (expirationDate.after(w.getExpirationDate()) || expirationDate
								.equals(w.getExpirationDate()))) {
					workSpaceToReturn.add(w);
				}
			}
			return workSpaceToReturn;

		} catch (Exception e) {
			throw new ServiceException("Error getting workspaces", e);
		}
	}

	@Override
	public List<WorkSpace> getUserWorkSpaces(long userId, Date expirationDate)
			throws ServiceException {

		try {
			List<WorkSpace> workSpaces = queryForList(getNameSpace()
					+ ".getUserWorkSpaces", userId);

			List<WorkSpace> workSpaceToReturn = new ArrayList<WorkSpace>();
			for (WorkSpace w : workSpaces) {
				if (expirationDate.before(w.getExpirationDate())) {
					workSpaceToReturn.add(w);
				}
			}
			return workSpaceToReturn;

		} catch (Exception e) {
			throw new ServiceException("Error getting workspaces for user:"
					+ userId, e);
		}
	}

	public List<WorkSpace> getUserVisibleWorkSpaces(long userId, Date expirationDate)
			throws ServiceException {

		try {
			List<WorkSpace> workSpaces = queryForList(getNameSpace()
					+ ".getUserVisibleWorkSpaces", userId);

			List<WorkSpace> workSpaceToReturn = new ArrayList<WorkSpace>();
			for (WorkSpace w : workSpaces) {
				if (expirationDate.before(w.getExpirationDate())) {
					workSpaceToReturn.add(w);
				}
			}
			return workSpaceToReturn;

		} catch (Exception e) {
			throw new ServiceException("Error getting workspaces for user:"
					+ userId, e);
		}
	}
	
	@Override
	public List<WorkSpace> getUserExternalWorkSpaces(long userId,
			Date expirationDate) throws ServiceException {
		try {
			List<WorkSpace> workSpaces = queryForList(getNameSpace()
					+ ".getUserExternalWorkSpaces", userId);

			List<WorkSpace> workSpacesToReturn = new ArrayList<WorkSpace>();
			for (WorkSpace w : workSpaces) {
				if (expirationDate.before(w.getExpirationDate())) {
					workSpacesToReturn.add(w);
				}
			}
			return workSpacesToReturn;

		} catch (Exception e) {
			throw new ServiceException("Error getting workspaces for user:"
					+ userId, e);
		}
	}
	
	@Override
	public long getVisibleWorkSpacesAmount(long userId) throws ServiceException {
		try {
			return ((Long) queryForObject(getNameSpace()
					+ ".getVisibleWorkSpacesAmount", userId));
		} catch (Exception e) {
			throw new ServiceException("Error workspace amount for user:"
					+ userId, e);
		}
	}
}
