package fi.nls.oskari.groupings.utils;

import java.util.ArrayList;
import java.util.List;

import fi.nls.oskari.domain.groupings.Grouping;
import fi.nls.oskari.domain.groupings.GroupingPermission;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.domain.groupings.GroupingThemeData;

public class GroupingCollectionHelper {
	public static List<GroupingTheme> findMainThemesForGrouping(long groupingsId,
			List<GroupingTheme> groupingThemes) {
		List<GroupingTheme> list = new ArrayList<GroupingTheme>();
		for (GroupingTheme g : groupingThemes) {
			if (g.getParentThemeId() == null && g.getOskariGroupingId() != null
					&& g.getOskariGroupingId().longValue() == groupingsId) {
				list.add(g);
			}
		}
		return list;
	}
	
	public static List<GroupingTheme> findUnbindedMainThemes(
			List<GroupingTheme> groupingThemes) {
		List<GroupingTheme> list = new ArrayList<GroupingTheme>();
		for (GroupingTheme g : groupingThemes) {
			if (g.getParentThemeId() == null
					&& g.getMainThemeId() == null && g.getOskariGroupingId() == null) {
				list.add(g);
			}
		}
		return list;
	}

	public static List<GroupingTheme> findSubthemes(long parentId,
			long groupingsId, List<GroupingTheme> groupingThemes) {
		List<GroupingTheme> list = new ArrayList<GroupingTheme>();
		for (GroupingTheme g : groupingThemes) {
			if (g.getParentThemeId() != null
					&& g.getParentThemeId().longValue() == parentId && g.getOskariGroupingId() != null
					&& g.getOskariGroupingId().longValue() == groupingsId) {
				list.add(g);
			}
		}
		return list;
	}
	
	public static List<GroupingTheme> findSubthemes(long parentId, List<GroupingTheme> groupingThemes) {
		List<GroupingTheme> list = new ArrayList<GroupingTheme>();
		for (GroupingTheme g : groupingThemes) {
			if (g.getParentThemeId() != null
					&& g.getParentThemeId().longValue() == parentId) {
				list.add(g);
			}
		}
		return list;
	}
	
	public static List<GroupingTheme> findUnbindedMainThemeSubthemes(long parentId,
			long mainThemId, List<GroupingTheme> groupingThemes) {
		List<GroupingTheme> list = new ArrayList<GroupingTheme>();
		for (GroupingTheme g : groupingThemes) {
			if (g.getParentThemeId() != null
					&& g.getParentThemeId().longValue() == parentId && g.getMainThemeId() != null
					&& g.getMainThemeId().longValue() == mainThemId) {
				list.add(g);
			}
		}
		return list;
	}
	

	public static List<GroupingThemeData> findGroupingThemeData(
			long groupingThemeId, List<GroupingThemeData> data) {
		List<GroupingThemeData> list = new ArrayList<GroupingThemeData>();
		for (GroupingThemeData g : data) {
			if (g.getOskariGroupingThemeId() == groupingThemeId) {
				list.add(g);
			}
		}
		return list;
	}

	public static List<GroupingPermission> findGroupingPermissions(
			List<GroupingPermission> permissions, long oskariGroupingId) {
		List<GroupingPermission> list = new ArrayList<GroupingPermission>();
		for (GroupingPermission p : permissions) {
			if (p.getOskariGroupingId() == oskariGroupingId) {
				list.add(p);
			}
		}

		return list;
	}
	
	public static Grouping createServicePackageStructure(Grouping g, List<GroupingTheme> groupingThemes, List<GroupingThemeData> data) {
		List<GroupingTheme> mainThemesForGrouping = GroupingCollectionHelper.findMainThemesForGrouping(g.getId(), groupingThemes);
		
		for (GroupingTheme gt : mainThemesForGrouping) {
			gt.setSubThemes(GroupingCollectionHelper.createSubthemesStructure(gt, groupingThemes, data));
		}
		
		g.setThemes(mainThemesForGrouping);
		
		return g;
	}
	
	private static List<GroupingTheme> createSubthemesStructure(GroupingTheme theme, List<GroupingTheme> groupingThemes, List<GroupingThemeData> data) {
		List<GroupingTheme> subthemes = GroupingCollectionHelper.findSubthemes(theme.getId(), theme.getOskariGroupingId(), groupingThemes);
		
		if (theme.getThemeType() == ThemeType.STAT.getCode()) {
			subthemes = GroupingCollectionHelper.findSubthemes(theme.getId(), groupingThemes);
		}
		
		if (subthemes.size() > 0) {
			for (GroupingTheme gt : subthemes) {
				gt.setSubThemes(GroupingCollectionHelper.createSubthemesStructure(gt, groupingThemes, data));
			}
		} else {
			List<GroupingThemeData> grData = GroupingCollectionHelper.findGroupingThemeData(theme.getId(), data);
			if (grData.size() > 0) {
				theme.setThemeData(grData);
			}
		}
		
		return subthemes;
	}
}
