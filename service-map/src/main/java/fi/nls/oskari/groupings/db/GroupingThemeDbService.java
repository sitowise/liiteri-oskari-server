package fi.nls.oskari.groupings.db;

import java.util.List;

import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseService;

public interface GroupingThemeDbService extends BaseService<GroupingTheme> {

	public List<GroupingTheme> getAllGroupingThemesForRole(long id)
			throws ServiceException;

	public List<GroupingTheme> getAllGroupingThemesForUser(long id)
			throws ServiceException;
	
	public List<GroupingTheme> getStatisticsThemesForParentId(Long id)
			throws ServiceException;
	
	public List<GroupingTheme> getTopLevelStatisticsThemes(long id)
			throws ServiceException;

    public List<GroupingTheme> getAllStatisticsThemes() throws ServiceException;

	public List<GroupingTheme> findByIds(List<Long> servicePackageIds,
			List<Long> themeIds, boolean includePublic) throws ServiceException;

/*	public List<GroupingTheme> getMainStatsThemesForGroupingId(
			long oskariGroupingId) throws ServiceException;;

	public GroupingTheme getMainUnbindedStatsTheme(long id)
			throws ServiceException;;
*/
}
