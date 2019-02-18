package fi.nls.oskari.control.szopa.requests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.mml.portti.domain.permissions.Permissions;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.szopa.JSONSzopaHelper;
import fi.nls.oskari.control.szopa.RegionDefinition;
import fi.nls.oskari.control.szopa.RegionDefinition.RegionType;
import fi.nls.oskari.control.szopa.RegionService;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONCopyHelper;
import fi.nls.oskari.util.JSONHelper;

public class IndicatorMetadata extends SzopaRequest {

    private RegionService _regionService = RegionService.getInstance();

    private static Logger log = LogFactory.getLogger(IndicatorMetadata.class);

    // public boolean isValid () {
    // return getIndicator() != null && getIndicator().isEmpty();
    // }

    @Override
    public String getName() {
        return "indicator_metadata";
    }

    @Override
    public String getRequestSpecificParams() {
        return "/indicators/" + getIndicator();
    }

    @Override
    protected String ConvertData(String data) throws ActionException {
        try {
            JSONObject source = JSONHelper.createJSONObject(data);
            JSONObject result = new JSONObject();

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
            if (!hasPermission) {
                if (source.getJSONObject("AccessRight").getInt("Id") != 1) {
                    throw new ActionException("No permissions");
                }
            }

            JSONArray years = ConvertTimePeriods(
                    source.getJSONArray("TimePeriods"), null);
            JSONArray gridYears = ConvertTimePeriods(
                    source.getJSONArray("TimePeriods"), "grid250m");

            JSONCopyHelper.Copy(source, "Id", result, "id");
            JSONCopyHelper.Copy(source, "OrderNumber", result, "orderNumber");
            JSONCopyHelper.Copy(source, "DecimalCount", result, "decimalCount");
            JSONCopyHelper.LanguageAwareCopy(source, "Name", result, "title");
            JSONCopyHelper.LanguageAwareCopy(source, "Description", result,
                    "description");
            JSONCopyHelper.LanguageAwareCopy(source, "AdditionalInformation",
                    result, "additionalInfo");
            JSONCopyHelper.Copy(source, "Unit", result, "unit");
            JSONCopyHelper.LanguageAwareCopy(source, "ProcessingStage", result,
                    "stage");
            // JSONCopyHelper.LanguageAwareCopy(source, "TimeSpan", result,
            // "lifeCycleState");
            JSONCopyHelper.LanguageAwareCopy(source, "TimeSpanDetails", result,
                    "lifeCycleState");
            JSONHelper.putValue(result, "themes", new JSONArray());

            Map<String, HashSet<Integer>> dataSourcesMap = gatherDataSources(source);

            JSONObject organization = new JSONObject();
            JSONHelper.putValue(organization, "id", 69);
            JSONHelper.putValue(organization, "title", JSONSzopaHelper
                    .createLanguageJSONObject(StringUtils.join(
                            dataSourcesMap.keySet(), "; ")));

            JSONArray dataSources = new JSONArray();
            for (String dataSourceName : dataSourcesMap.keySet()) {
                JSONObject dataSourceItem = new JSONObject();
                JSONHelper.putValue(dataSourceItem, "name", dataSourceName);
                JSONArray yearsArray = new JSONArray();
                for (Integer yearItem : dataSourcesMap.get(dataSourceName)) {
                    yearsArray.put(yearItem);
                }
                JSONHelper.putValue(dataSourceItem, "years", yearsArray);
                dataSources.put(dataSourceItem);
            }
            JSONHelper.putValue(result, "dataSources", dataSources);

            // TODO: by default all region categories are taken
            JSONObject classifications = new JSONObject();
            JSONObject regionClassifications = new JSONObject();
            JSONArray regionCategoriesArray = JSONSzopaHelper
                    .createJSONArrayFromArray(GetRegionCategories());
            JSONHelper.putValue(regionClassifications, "values",
                    regionCategoriesArray);
            JSONHelper.putValue(classifications, "region",
                    regionClassifications);

            JSONHelper.putValue(result, "years", years);
            JSONHelper.putValue(result, "gridYears", gridYears);
            JSONHelper.putValue(result, "organization", organization);
            JSONHelper.putValue(result, "classifications", classifications);

            JSONCopyHelper.Copy(source, "TimePeriods", result, "timePeriods");

            JSONCopyHelper.Copy(source, "PrivacyLimit", result, "privacyLimit");

            JSONCopyHelper.Copy(source, "AccessRight", result, "accessRight");

            JSONCopyHelper.Copy(source, "ZeroVisibility", result, "zeroVisibility");

            return result.toString(1);
        } catch (JSONException e) {
            log.error("Cannot convert data", e);
            throw new ActionException("Cannot convert data", e);
        }
    }

    private Map<String, HashSet<Integer>> gatherDataSources(JSONObject source)
            throws JSONException {
        HashMap<String, HashSet<Integer>> map = new HashMap<String, HashSet<Integer>>();

        JSONArray timePeriods = source.getJSONArray("TimePeriods");
        for (int i = 0; i < timePeriods.length(); i++) {
            JSONObject itemSource = timePeriods.getJSONObject(i);
            JSONArray areaTypes = itemSource.getJSONArray("AreaTypes");
            Integer id = itemSource.getInt("Id");
            if (areaTypes != null && areaTypes.length() > 0) {
                for (int j = 0; j < areaTypes.length(); j++) {
                    JSONObject areaTypeItem = areaTypes.getJSONObject(j);
                    String dataSource = areaTypeItem.getString("DataSource");
                    if (!dataSource.isEmpty()) {
                        if (!map.containsKey(dataSource))
                            map.put(dataSource, new HashSet<Integer>());

                        map.get(dataSource).add(id);
                    }
                }
            }
        }

        return map;
    }

    private List<String> GetRegionCategories() {
        ArrayList<String> result = new ArrayList<String>();
        for (RegionDefinition definiton : _regionService.GetRegionsOfType(
                RegionType.ADMINISTRATIVE, RegionType.FUNCTIONAL)) {
            result.add(definiton.getId());
        }
        return result;
    }

    private JSONArray ConvertTimePeriods(JSONArray source, String areaType)
            throws JSONException {
        JSONArray result = new JSONArray();

        for (int i = 0; i < source.length(); i++) {
            JSONObject itemSource = source.getJSONObject(i);
            String yearId = itemSource.getString("Id");
            JSONArray areaTypes = itemSource.getJSONArray("AreaTypes");
            if (areaTypes != null && areaTypes.length() > 0) {
                if (areaType != null) {
                    for (int j = 0; j < areaTypes.length(); j++) {
                        JSONObject areaTypeItem = areaTypes.getJSONObject(j);
                        if (areaTypeItem.getString("Id").equalsIgnoreCase(
                                areaType)) {
                            result.put(yearId);
                            break;
                        }
                    }
                } else {
                    result.put(yearId);
                }
            }
        }

        return result;
    }

}
