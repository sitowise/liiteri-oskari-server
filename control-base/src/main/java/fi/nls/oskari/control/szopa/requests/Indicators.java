package fi.nls.oskari.control.szopa.requests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.mml.portti.domain.permissions.Permissions;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.szopa.JSONSzopaHelper;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.domain.groupings.GroupingThemeData;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.groupings.db.GroupingThemeDataServiceIbatisImpl;
import fi.nls.oskari.groupings.db.GroupingThemeDbService;
import fi.nls.oskari.groupings.db.GroupingThemeServiceIbatisImpl;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.JSONCopyHelper;
import fi.nls.oskari.util.JSONHelper;

public class Indicators extends SzopaRequest {

    private static Logger log = LogFactory.getLogger(Indicators.class);
    private static final GroupingThemeDbService groupingThemesService = new GroupingThemeServiceIbatisImpl();
    private static final GroupingThemeDataServiceIbatisImpl groupingThemeDataService = new GroupingThemeDataServiceIbatisImpl();

    @Override
    public String getCacheKey() {
        return this.getUrl() + "|" + this.getFormat() + "|" + this.getRequestBody();
    }

    @Override
    public String getName() {
        return "indicators";
    }

    @Override
    public String getRequestSpecificParams() {
        final User user = getUser();
        boolean hasPermission = false;

        if (user != null) {
            PermissionsService permissionsService = new PermissionsServiceIbatisImpl();
            final Set<String> permissionsList = permissionsService
                    .getResourcesWithGrantedPermissions("operation", user,
                            Permissions.PERMISSION_TYPE_EXECUTE);
            for(String permission : permissionsList) {
                if("statistics+restricted".equals(permission)) {
                    hasPermission = true;
                }
            }
        }

        if (hasPermission) {
            return "/indicators";
        } else {
            return "/indicators?accessRight=1";
        }
    }

    @Override
    public String getData() throws ActionException {
        final String format = getFormat();
        final User user = getUser();

        if ("tree".equals(format) && user != null) {
            List<JSONObject> themes = new ArrayList<JSONObject>();
            List<Long> allowedIndicatorIds = new ArrayList<Long>();

            try {
                final SzopaRequest flatIndicatorRequest = SzopaRequest
                        .getInstance("indicators");

                flatIndicatorRequest.setVersion("v1");

                if (user != null) {
                    flatIndicatorRequest.setUser(user);
                }

                JSONArray indicators = new JSONArray(flatIndicatorRequest.getData());
                for (int i = 0; i < indicators.length(); ++i) {
                    allowedIndicatorIds.add(indicators.getJSONObject(i)
                            .getLong("id"));
                }
            } catch (JSONException e) {
                log.error(e, "Could not get allowed statistics");
                throw new ActionException("Error getting allowed statistics", e);
            }

            try {
                List<GroupingTheme> statisticsThemes = groupingThemesService
                        .getAllStatisticsThemes();
                log.info("statisticsThemes.size() " + statisticsThemes.size());
                List<GroupingThemeData> indicatorsInThemes = groupingThemeDataService
                        .getAllIndicatorsForStatisticsThemes();
                log.info("indicatorsInThemes.size() "
                        + indicatorsInThemes.size());

                for (Iterator<GroupingThemeData> iterator = indicatorsInThemes
                        .iterator(); iterator.hasNext();) {
                    GroupingThemeData gtd = iterator.next();
                    if (!allowedIndicatorIds.contains(gtd.getDataId())) {
                        // Remove the current element from the iterator and the
                        // list.
                        iterator.remove();
                    }
                }

                for (GroupingTheme t : groupingThemesService
                        .getTopLevelStatisticsThemes(user.getId())) {
                    JSONObject r = themeToJSON(t, statisticsThemes,
                            indicatorsInThemes);
                    if (r != null)
                        themes.add(r);
                }
            } catch (ServiceException e) {
                throw new ActionException("Error getting statistics themes", e);
            }

            JSONObject ret = new JSONObject();

            JSONHelper.putValue(ret, "themes", new JSONArray(themes));

            CacheDataIfDesired(ret.toString());

            return ret.toString();
        }
        return super.getData();
    }

    @Override
    protected String ConvertData(String data) throws ActionException {
        try {
            return Convert(data);
        } catch (JSONException e) {
            log.error("Cannot convert data", e);
            throw new ActionException("Cannot convert data", e);
        }
    }

    private JSONObject themeToJSON(GroupingTheme theme,
            List<GroupingTheme> statisticsThemes,
            List<GroupingThemeData> indicatorsInThemes) throws ServiceException {
        JSONObject ret = new JSONObject();
        JSONHelper.putValue(ret, "fi", theme.getName());
        JSONHelper.putValue(ret, "id", theme.getId());

        List<GroupingThemeData> themeData = new ArrayList<GroupingThemeData>();

        for (GroupingThemeData indicator : indicatorsInThemes) {
            if (indicator.getOskariGroupingThemeId() == theme.getId()) {
                themeData.add(indicator);
            }
        }
        List<Long> themeDataIds = new ArrayList<Long>();
        for (GroupingThemeData gtd : themeData) {
            themeDataIds.add(gtd.getDataId());
        }
        JSONHelper.putValue(ret, "indicators", new JSONArray(themeDataIds));

        List<GroupingTheme> subThemes = new ArrayList<GroupingTheme>();

        for (GroupingTheme statisticsTheme : statisticsThemes) {
            if (statisticsTheme.getParentThemeId() != null
                    && statisticsTheme.getParentThemeId() == theme.getId()) {
                subThemes.add(statisticsTheme);
            }
        }

        List<JSONObject> subThemeJSON = new ArrayList<JSONObject>();
        for (GroupingTheme gt : subThemes) {
            JSONObject t = themeToJSON(gt, statisticsThemes, indicatorsInThemes);
            if (t != null)
                subThemeJSON.add(t);
        }
        JSONHelper.putValue(ret, "themes", new JSONArray(subThemeJSON));
        if (themeDataIds.size() == 0 && subThemeJSON.size() == 0) { // skip
                                                                    // empty
                                                                    // theme
            return null;
        }

        if (theme.getOskariGroupingId() != null) {
            JSONHelper.putValue(ret, "mainType", "package");
        }
        return ret;
    }

    private String Convert(String data) throws JSONException {

        JSONArray array = JSONHelper.createJSONArray(data);
        JSONArray resultArray = new JSONArray();

        log.info("Got " + array.length() + " indicators");

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            JSONObject itemResult = new JSONObject();

            JSONCopyHelper.Copy(item, "Id", itemResult, "id");
            JSONCopyHelper.LanguageAwareCopy(item, "Name", itemResult, "name");
            JSONCopyHelper.Copy(item, "OrderNumber", itemResult, "orderNumber",
                    0);

            String name = item.getString("Name");
            JSONArray themesInput = new JSONArray();
            JSONArray themesResult = new JSONArray();
            String[] themesArray = JSONSzopaHelper
                    .getArrayFromJSONArray(themesInput);

            for (String themeItem : themesArray) {
                themesResult.put(JSONSzopaHelper
                        .createLanguageJSONObject(themeItem));
            }

            String themesJoinedString = StringUtils.join(themesArray, ",");

            JSONHelper.putValue(
                    itemResult,
                    "title",
                    JSONSzopaHelper.createLanguageJSONObject(name + " "
                            + themesJoinedString));
            JSONHelper.putValue(itemResult, "themes", themesResult);

            resultArray.put(itemResult);
        }

        return resultArray.toString(1);
    }
}
