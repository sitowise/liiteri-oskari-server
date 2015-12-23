package fi.nls.oskari.map.userowndata;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ibatis.sqlmap.client.SqlMapSession;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.UserGisDataSharing;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.domain.workspaces.WorkSpaceSharing;
import fi.nls.oskari.groupings.utils.EmailSender;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.service.db.BaseIbatisService;
import fi.nls.oskari.util.PropertyUtil;

public class GisDataDbServiceImpl extends BaseIbatisService<UserGisData> implements GisDataDbService{	
	
	public GisDataDbServiceImpl() {
	}

	@Override
	public List<UserGisData> getGisData(long userId, Date expirationDate)
			throws ServiceException {
		try {
			List<UserGisData> gisDataList = queryForList(getNameSpace() + ".getUserGisDataList", userId);
			
			List<UserGisData> userGisDataToReturn = new ArrayList<UserGisData>();
			
			for (UserGisData u: gisDataList) {
				if (expirationDate.before(u.getExpirationDate())) {
					userGisDataToReturn.add(u);
				}
			}
			
			return userGisDataToReturn;
			
		} catch (Exception e) {
			throw new ServiceException("Error getting GIS data for user" + userId, e);
		}
	}

	@Override
	protected String getNameSpace() {
		return "UserGisData";
	}

	@Override
	public List<UserGisData> getUserGisDataAfterExpirationDate(
			Date expirationDate, String status) throws ServiceException {
		try {
			List<UserGisData> userGisDataList = queryForList(getNameSpace()
					+ ".getAllUserGisData");

			List<UserGisData> userGisDataListToReturn = new ArrayList<UserGisData>();
			for (UserGisData u : userGisDataList) {
				if (status.equals(u.getStatus())
						&& (expirationDate.after(u.getExpirationDate()) || expirationDate
								.equals(u.getExpirationDate()))) {
					userGisDataListToReturn.add(u);
				}
			}
			return userGisDataListToReturn;

		} catch (Exception e) {
			throw new ServiceException("Error getting GIS data", e);
		}
	}

	@Override
	public long getVisibleGisDatasetsAmount(long userId)
			throws ServiceException {
		try {
			return ((Long) queryForObject(getNameSpace() + ".getVisibleGisDatasetsAmount", userId));
		} catch (Exception e) {
			throw new ServiceException("Error during getting user gis datasets amount for user " + userId, e);
		}
	}

	@Override
	public int deleteGisData(String dataId, String dataType, long userId,
			String deletetOnlySharing) throws ServiceException {
		int rowsCount = 0;
		final SqlMapSession session = openSession();
		try {
			if (deletetOnlySharing != null && deletetOnlySharing.equals("true")) {
				//UserGisDataSharing ugds = new UserGisDataSharing();
				//ugds.setExternalId(userId);
				//ugds.setDatasetId(userGisDataId);
				
				//rowsCount = (int) session.delete(getNameSpace() + ".deleteUserGisDataSharing", ugds);
			} else {
				UserGisData ugd = new UserGisData();
				ugd.setDataId(dataId);
				ugd.setDataType(dataType);
				rowsCount = (int) session.delete(getNameSpace() + ".deleteUserGisData", ugd);
			}
		} catch (Exception e) {
			throw new ServiceException("Error during deleting the user gis data", e);
		} finally {
			session.close();
		}
		
		return rowsCount;
	}

	@Override
	public UserGisData findGisDataByDataId(String dataId, String dataType)
			throws ServiceException {
		UserGisData userGisData = new UserGisData();
		userGisData.setDataId(dataId);
		userGisData.setDataType(dataType);
		
		return queryForObject(getNameSpace() + ".findGisData", userGisData);
	}
}
