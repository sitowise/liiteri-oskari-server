package fi.nls.oskari.groupings.themes.api;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.List;

import org.json.JSONObject;

import fi.nls.oskari.domain.groupings.Grouping;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.domain.groupings.GroupingThemeData;
import fi.nls.oskari.groupings.utils.ThemeType;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;

public class GroupingThemesApiService {
	/*private static final Logger log = LogFactory
			.getLogger(GroupingThemesApiService.class);

	private static String _baseUrl;
	static {
		_baseUrl = PropertyUtil.get("themes.baseurl");
	}

	private String getData(String url) throws ServiceException {
		HttpURLConnection con = null;
		try {

			con = IOHelper.getConnection(url);
			final String data = IOHelper.readString(con.getInputStream());
			return data;
		} catch (Exception e) {
			throw new ServiceException(
					"Couldn't proxy request to Themes API server", e);
		} finally {
			try {
				con.disconnect();
			} catch (Exception ignored) {
			}
		}
	}

	private String buildBaseThemeUrl(long id, boolean isParent) {
		StringWriter wr = new StringWriter();
		wr.write(_baseUrl);
		wr.write("/themes/");
		if (id > 0) {
			if (isParent) {
				wr.write("?parentId=");
			}
			wr.write(String.valueOf(id));
		}
		return wr.toString();
	}

	private String buildBaseIndicatorUrl(long id) {
		StringWriter wr = new StringWriter();
		wr.write(_baseUrl);
		wr.write("/indicators/");
		if (id > 0) {

			wr.write(String.valueOf(id));
		}
		return wr.toString();
	}

	private int deleteTheme(long id) throws ServiceException {
		HttpURLConnection con = null;
		try {

			con = IOHelper.getConnection(buildBaseThemeUrl(id, false));
			con.setRequestMethod("DELETE");
			con.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			return con.getResponseCode();
		} catch (Exception e) {
			throw new ServiceException(
					"Couldn't proxy request to Themes API server", e);
		} finally {
			try {
				con.disconnect();
			} catch (Exception ignored) {
			}
		}
	}

	private void writeTheme(GroupingTheme theme, Long parentApiThemeId)
			throws ServiceException {

		HttpURLConnection connection = null;
		try {
			JSONObject ob = JSONGroupingThemesApiServiceHelper
					.createApiThemeObject(theme, parentApiThemeId);

			connection = IOHelper.getConnection(buildBaseThemeUrl(0, false));
			connection.setRequestProperty("Content-Type", "application/json");
			IOHelper.writeToConnection(connection, ob.toString());
			theme.setApiThemeId(JSONGroupingThemesApiServiceHelper
					.getApiThemeId(IOHelper.readString(connection
							.getInputStream())));
			if (theme.getSubThemes().size() > 0) {
				for (GroupingTheme s : theme.getSubThemes()) {
					writeTheme(s, s.getApiThemeId());
				}
			}
			if (theme.getThemeData().size() > 0) {
				for (GroupingThemeData d : theme.getThemeData()) {
					writeIndicator(d.getDataId(), theme.getApiThemeId());
				}
			}

		} catch (Exception e) {
			throw new ServiceException(
					"Couldn't proxy request to Themes API server", e);
		} finally {
			try {
				connection.disconnect();
			} catch (Exception ignored) {
			}
		}
	}

	private void writeIndicator(long id, Long themeId) throws ServiceException {

		HttpURLConnection connection = null;
		try {
			JSONObject ob = JSONGroupingThemesApiServiceHelper
					.createApiIndicatorObject(id, themeId);

			connection = IOHelper.getConnection(buildBaseIndicatorUrl(id));
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.connect();
			BufferedOutputStream out = new BufferedOutputStream(
					connection.getOutputStream());
			out.write(ob.toString().getBytes());
			out.flush();
			out.close();
			connection.getResponseCode();

		} catch (Exception e) {
			throw new ServiceException(
					"Couldn't proxy request to Themes API server", e);
		} finally {
			try {
				connection.disconnect();
			} catch (Exception ignored) {
			}
		}
	}

	public String getTheme(long id) throws ServiceException {
		return getData(buildBaseThemeUrl(id, false));
	}

	public String getSubThemes(long id) throws ServiceException {
		return getData(buildBaseThemeUrl(id, true));
	}

	/*
	 * Deleting doesn't require checking theme type because it is included in
	 * SQL condition. Insert does need it that checking
	 

	public void insertUnbindedTheme(GroupingTheme theme)
			throws ServiceException {
		if (theme.getThemeType() == ThemeType.STAT.getCode())
			writeTheme(theme, null);
	}

	public void updateUnbindedTheme(GroupingTheme theme,
			GroupingTheme themeToDelete) throws ServiceException {
		if (themeToDelete.getApiThemeId() != null) {
			deleteTheme(themeToDelete.getApiThemeId());
		}
		if (theme.getThemeType() == ThemeType.STAT.getCode())
			writeTheme(theme, null);
	}

	public void insertThemes(Grouping grouping) throws ServiceException {
		for (GroupingTheme t : grouping.getThemes()) {
			if (t.getThemeType() == ThemeType.STAT.getCode()) {
				writeTheme(t, null);
			}
		}
	}

	// //REMEBER ABOUT CONDITION
	public void updateThemes(Grouping grouping,
			List<GroupingTheme> apiThemesToDelete) throws ServiceException {
		for (GroupingTheme at : apiThemesToDelete) {
			if (at.getApiThemeId() != null) {
				deleteTheme(at.getApiThemeId());
			}
		}
		for (GroupingTheme t : grouping.getThemes()) {
			if (t.getThemeType() == ThemeType.STAT.getCode()) {
				writeTheme(t, null);
			}

		}
	}

	// //REMEBER ABOUT CONDITION
	public void deleteUnbindedTheme(GroupingTheme themeToDelete)
			throws ServiceException {
		if (themeToDelete.getApiThemeId() != null)
			deleteTheme(themeToDelete.getApiThemeId());
	}

	// //REMEBER ABOUT CONDITION
	public void deleteThemes(List<GroupingTheme> apiThemesToDelete)
			throws ServiceException {
		for (GroupingTheme at : apiThemesToDelete) {
			if (at.getApiThemeId() != null) {
				deleteTheme(at.getApiThemeId());
			}
		}
	}
*/
}
