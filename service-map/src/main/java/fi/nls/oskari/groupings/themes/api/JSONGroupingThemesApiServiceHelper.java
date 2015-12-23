package fi.nls.oskari.groupings.themes.api;

import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.util.JSONHelper;

public class JSONGroupingThemesApiServiceHelper {
	/*
	public static final JSONObject createApiThemeObject(GroupingTheme theme,
			Long parentApiThemeId) throws JSONException {
		JSONObject ob = new JSONObject();
		JSONHelper.putValue(ob, "Name", theme.getName());
		ob.putOpt("ParentId", parentApiThemeId);
		return ob;

	}

	public static final Long getApiThemeId(String json) {
		JSONObject mainJSON = JSONHelper.createJSONObject(json);
		return mainJSON.optLong("Id");

	}

	public static final JSONObject createApiIndicatorObject(long id, Long themeId)
			throws JSONException {
		JSONObject ob = new JSONObject();
		JSONHelper.putValue(ob, "Id", id);
		ob.putOpt("ThemeId", themeId);
		return ob;

	}
*/
}
