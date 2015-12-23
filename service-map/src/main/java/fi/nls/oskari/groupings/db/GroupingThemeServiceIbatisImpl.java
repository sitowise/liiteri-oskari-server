package fi.nls.oskari.groupings.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import fi.nls.oskari.domain.groupings.*;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseIbatisService;

public class GroupingThemeServiceIbatisImpl extends
		BaseIbatisService<GroupingTheme> implements GroupingThemeDbService {
	@Override
	protected String getNameSpace() {
		return "GroupingThemes";
	}

	@Override
	public List<GroupingTheme> getAllGroupingThemesForRole(long id)
			throws ServiceException {
		try {
			return queryForList(
					getNameSpace() + ".getAllGroupingThemesForRole", id);
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting grouping themes for role:" + id, e);
		}
	}

	@Override
	public List<GroupingTheme> getAllGroupingThemesForUser(long id)
			throws ServiceException {
		try {
			return queryForList(
					getNameSpace() + ".getAllGroupingThemesForUser", id);
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting grouping themes for user:" + id, e);
		}
	}

	@Override
	public List<GroupingTheme> getStatisticsThemesForParentId(Long id)
			throws ServiceException {
		try {
			return queryForList(
					getNameSpace() + ".getStatisticsThemesForParentId", id);
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting statistics themes", e);
		}
	}
	
	@Override
	public List<GroupingTheme> getTopLevelStatisticsThemes(long id)
			throws ServiceException {
		try {
			return queryForList(
					getNameSpace() + ".getTopLevelStatisticsThemes", id);
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting statistics themes", e);
		}
	}
    
    @Override
    public List<GroupingTheme> getAllStatisticsThemes()
            throws ServiceException {
        try {
            return queryForList(
                    getNameSpace() + ".getAllStatisticsThemes");
        } catch (Exception e) {
            throw new ServiceException(
                    "Error getting statistics themes", e);
        }
    }

	@Override
	public List<GroupingTheme> findByIds(List<Long> servicePackageIds,
			List<Long> themeIds, boolean includePublic) throws ServiceException
	{		
		if (servicePackageIds.size() == 0 && themeIds.size() == 0 && !includePublic)
			return new Vector<GroupingTheme>();
			
		HashMap<Long, GroupingTheme> map = new HashMap<Long, GroupingTheme>();
		if (includePublic) {
			List<GroupingTheme> themes = queryForList(getNameSpace() + ".getPublicThemes");
			for (GroupingTheme theme : themes)
				map.put(theme.getId(), theme);
		}
		if (servicePackageIds.size() > 0) {
			List<GroupingTheme> themes = queryForList(getNameSpace() + ".getServicePackageThemesById", servicePackageIds);
			for (GroupingTheme theme : themes)
				map.put(theme.getId(), theme);
		}
		if (themeIds.size() > 0) {
			List<GroupingTheme> themes = queryForList(getNameSpace() + ".getThemesById", themeIds);
			for (GroupingTheme theme : themes)
				map.put(theme.getId(), theme);
		}
		
		return new ArrayList<GroupingTheme>(map.values());
	}
	
/*
	@Override
	public List<GroupingTheme> getMainStatsThemesForGroupingId(
			long oskariGroupingId) throws ServiceException {
		try {
			return queryForList(
					getNameSpace() + ".getMainStatsThemesForGroupingId",
					oskariGroupingId);
		} catch (Exception e) {
			throw new ServiceException(
					"Error getting stats themes for grouping:"
							+ oskariGroupingId, e);
		}
	}

	@Override
	public GroupingTheme getMainUnbindedStatsTheme(long id)
			throws ServiceException {
		try {
			return queryForObject(getNameSpace()
					+ ".getMainUnbindedStatsTheme", id);
		} catch (Exception e) {
			throw new ServiceException("Error getting theme:" + id, e);
		}
	}*/
}

	/*
	 * public List<GroupingTheme> GetAllMainThemes()throws ServiceException {
	 * try { return queryForList(getNameSpace() + ".getAllMainThemes"); } catch
	 * (Exception e) { throw new ServiceException("Error getting main themes" ,
	 * e); } }
	 */
