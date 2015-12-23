package fi.nls.oskari.groupings.db;

import java.util.List;

import fi.nls.oskari.domain.groupings.GroupingThemeData;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseIbatisService;

public class GroupingThemeDataServiceIbatisImpl extends
		BaseIbatisService<GroupingThemeData> {
	@Override
	protected String getNameSpace() {
		return "GroupingThemeData";
	}

	public List<GroupingThemeData> getDataForThemeId(Long id)
			throws ServiceException {
		try {
			return queryForList(
					getNameSpace() + ".getDataForThemeId", id);
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting statistics themes", e);
		}
	}

    public List<GroupingThemeData> getAllIndicatorsForStatisticsThemes()
            throws ServiceException {
        try {
            return queryForList(
                    getNameSpace() + ".getAllIndicatorsForStatisticsThemes");
        } catch (Exception e) {
            throw new ServiceException(
                    "Error getting statistics themes", e);
        }
    }

}