package fi.nls.oskari.map.userowndata;

import java.util.Date;
import java.util.List;

import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseService;

public interface GisDataDbService extends BaseService<UserGisData>{
	
	public List<UserGisData> getGisData(long userId, Date expirationDate) throws ServiceException;

	public int deleteGisData(String dataId, String dataType, long userId, String deletetOnlySharing) throws ServiceException;
	
	public List<UserGisData> getUserGisDataAfterExpirationDate(
			Date expirationDate, String status) throws ServiceException;
	
	public long getVisibleGisDatasetsAmount(long userId) throws ServiceException;
	
	public UserGisData findGisDataByDataId(String dataId, String dataType) throws ServiceException;
}
