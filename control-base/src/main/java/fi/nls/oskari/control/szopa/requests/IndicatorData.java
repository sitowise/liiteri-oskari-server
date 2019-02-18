package fi.nls.oskari.control.szopa.requests;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.mml.portti.domain.permissions.Permissions;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.szopa.FilterParser;
import fi.nls.oskari.control.szopa.RegionDefinition;
import fi.nls.oskari.control.szopa.RegionDefinition.RegionType;
import fi.nls.oskari.control.szopa.RegionService;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONCopyHelper;
import fi.nls.oskari.util.JSONHelper;

/**
 * Request class for SotkaNET statistics query to get indicator data in CSV
 * format. SotkaRequest transforms CSV to JSON since we defined isCSV() => true
 *
 * @author SMAKINEN
 */
public class IndicatorData extends SzopaRequest {

    private static Logger log = LogFactory.getLogger(IndicatorData.class);

    // private final static String CACHE_KEY =
    // "liiteri_szopa_sample_indicator_data";

    private HashMap<String, SzopaRequestPart> _administrativeRequestsMap = new LinkedHashMap<String, SzopaRequestPart>();
    private HashMap<String, SzopaRequestPart> _partRequestsMap = new HashMap<String, SzopaRequestPart>();
    private RegionService _regionService = RegionService.getInstance();
    private FilterParser _filterParser = FilterParser.getInstance();

    private String _cacheKey = null;

    public IndicatorData() {

        List<RegionDefinition> definition = _regionService
                .GetRegionsOfType(RegionType.ADMINISTRATIVE); // default
                                                              // is
                                                              // to
                                                              // get
                                                              // the
                                                              // only
                                                              // administrative
                                                              // areas
        for (RegionDefinition regionDefinition : definition) {
            _administrativeRequestsMap.put(regionDefinition.getApiid(),
                    new SzopaRequestPart(regionDefinition.getId()));
        }

        definition = _regionService.GetAllRegions();
        for (RegionDefinition regionDefinition : definition) {
            _partRequestsMap.put(regionDefinition.getApiid(),
                    new SzopaRequestPart(regionDefinition.getId()));
        }
    }

    public boolean isValid() {
        return getIndicator() != null && getIndicator().isEmpty();
    }

    @Override
    public String getName() {
        return "data";
    }

    @Override
    public String getCacheKey() {
        if (_cacheKey == null) {
            StringBuffer buf = new StringBuffer();
            final ArrayList<String> groups = GetGroups();

            for (String groupName : groups) {
                final SzopaRequestPart partRequest = _partRequestsMap
                        .get(groupName);
                partRequest.setRequestSpecificParams(GetParamsFor(groupName));
                partRequest.setVersion(getVersion());
                partRequest.setRequestBody(getRequestBody(groupName));

                buf.append("|");
                buf.append(partRequest.getCacheKey());
            }
            _cacheKey = buf.toString();
        }

        return _cacheKey;
    }

    private String GetParamsFor(String groupName) {
        StringWriter writer = new StringWriter();
        writer.write("/statistics/");
        writer.write(getIndicator());

        return writer.toString();
    }

    private ArrayList<String> GetGroups() {
        ArrayList<String> result = new ArrayList<String>();
        String group = this.getGroup();
        String filter = this.getFilter();

        if (group == null || group.isEmpty()
                || !_partRequestsMap.containsKey(group)) {
            if (filter == null || filter.isEmpty()) {
                result.addAll(_administrativeRequestsMap.keySet());
            } else {
                for (String key : _administrativeRequestsMap.keySet()) {
                    result.add(key);
                    if (filter.contains(key)) {
                        return result;
                    }
                }
            }
        } else {
            result.add(group);
        }

        return result;
    }

    private String getRequestBody(String groupName) {
        JSONObject ret = new JSONObject();
        JSONHelper.putValue(ret, "group", groupName);
        JSONHelper.putValue(ret, "area_year", getAreaYear());

        try {
            String[] yearStrings = getYears();
            int[] yearInts = new int[yearStrings.length];
            for (int i = 0; i < yearStrings.length; i++) {
                yearInts[i] = Integer.parseInt(yearStrings[i]);
            }
            JSONArray years = new JSONArray(yearInts);
            JSONHelper.putValue(ret, "years", years);
        } catch (JSONException e) {
            log.error("Not an array", e);
        }

        String filter = null;

        if (getFilter().length() > 0) {
            filter = getFilter();
        }

        JSONHelper.putNullAwareValue(ret, "filter", filter);

        return ret.toString();
    }

    @Override
    public String getData() throws ActionException {
        try {
            // if (this.getGroup() == null || this.getGroup().isEmpty()) {
            // final String cachedData = JedisManager.get(CACHE_KEY);
            // if(cachedData != null && !cachedData.isEmpty()) {
            // return cachedData;
            // }
            // }

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
            if (!hasPermission) { // user has no permission to restricted data
                                  // so check from metadata if this indicator is
                                  // restricted
                final SzopaRequest metadataRequest = SzopaRequest
                        .getInstance("indicator_metadata");

                metadataRequest.setIndicator(getIndicator());
                metadataRequest.setVersion("v1");

                if (user != null) {
                    metadataRequest.setUser(user);
                }

                JSONObject metadata = new JSONObject(metadataRequest.getData());
                if (metadata.getJSONObject("accessRight").getInt("Id") != 1) {
                    throw new ActionException("No permissions");
                }
            }

            String cached = TryGetCacheDataIfDesired();
            if (cached != null)
                return cached;

            final JSONArray result = new JSONArray();
            final ArrayList<String> groups = GetGroups();
            boolean throwExceptionOnFail = groups.size() == 1;

            for (String groupName : groups) {
                final SzopaRequestPart partRequest = _partRequestsMap
                        .get(groupName);
                partRequest.setRequestSpecificParams(GetParamsFor(groupName));
                partRequest.setVersion(getVersion());
                partRequest.setRequestBody(getRequestBody(groupName));

                try {
                    StopWatch sw = new StopWatch();
                    sw.start();
                    String data = partRequest.getData();
                    sw.stop();
                    log.info(partRequest.getName() + " " + sw.getTime());
                    final JSONArray array = ConvertDataToJSON(data, groupName);
                    addRange(result, array);
                } catch (ActionException ae) {
                    log.warn("Cannot get part request data for "
                            + partRequest.getName() + " " + ae.toString());
                    if (throwExceptionOnFail) {
                        throw ae;
                    }
                }
            }

            String resultStr = result.toString();

            // if (this.getGroup() == null || this.getGroup().isEmpty()) {
            // JedisManager.setex(CACHE_KEY, JedisManager.EXPIRY_TIME_DAY,
            // resultStr);
            // }

            CacheDataIfDesired(resultStr);

            return resultStr;

        } catch (Exception e) {
            throw new ActionException("Couldn't proxy request to Szopa server",
                    e);
        }
    }

    private void addRange(JSONArray arr1, JSONArray arr2) throws JSONException {
        for (int i = 0; i < arr2.length(); i++) {
            arr1.put(arr2.get(i));
        }
    }

    protected JSONArray ConvertDataToJSON(String data, String groupName)
            throws ActionException {
        try {
            JSONArray inputArray = JSONHelper.createJSONArray(data);
            JSONArray resultArray = new JSONArray();

            List<String> wantedAreas = new ArrayList<String>();

            final SzopaRequest req = SzopaRequest
                    .getInstance("functionalAvailability");
            if (getAreaYear().length() > 0) {
                req.setAreaYear(getAreaYear());
            } else {
                req.setAreaYear(getYears()[0]);
            }
            req.setGroup(groupName);
            req.setStandardFilterParam(getStandardFilterParam());
            req.setVersion("v1");

            JSONArray areaData = new JSONArray(req.getData());
            Set<String> filterAreas = _filterParser.getAreasFromFilter(
                    getStandardFilterParam(), RegionType.FUNCTIONAL).keySet();

            for (int i = 0; i < areaData.length(); ++i) {
                JSONObject areaObject = areaData.getJSONObject(i);
                if (filterAreas.size() == 0) {
                    wantedAreas.add(areaObject.getString("id"));
                } else {
                    int found = 0;
                    JSONArray functionalAreas = areaObject
                            .getJSONArray("functionalAreas");
                    for (int j = 0; j < functionalAreas.length(); ++j) {
                        if (filterAreas.contains(functionalAreas.getString(j))) {
                            if (++found == filterAreas.size()) {
                                wantedAreas.add(areaObject.getString("id"));
                            }
                        }
                    }
                }
            }

            log.info("Got #%s statistics", inputArray.length());

            for (int i = 0; i < inputArray.length(); i++) {
                JSONObject inputItem = inputArray.getJSONObject(i);
                JSONObject resultItem = new JSONObject();

                String region = groupName + ":" + inputItem.get("AreaId");
                wantedAreas.remove(region);
                JSONHelper.putValue(resultItem, "region", region);
                JSONCopyHelper.Copy(inputItem, "AlternativeId", resultItem,
                        "alternativeId");
                JSONCopyHelper.Copy(inputItem, "Year", resultItem, "year");
                if (inputItem.getBoolean("PrivacyLimitTriggered")) {
                    JSONHelper.putValue(resultItem, "PrivacyLimitTriggered",
                            true);
                } else {
                    JSONCopyHelper.CopyAsString(inputItem, "Value", resultItem,
                            "primary value");
                    JSONCopyHelper.CopyAsString(inputItem, "Value", resultItem,
                            "absolute value");
                }
                JSONHelper.putValue(resultItem, "gender", "total");
                JSONHelper.putValue(resultItem, "indicator", getIndicator());

                resultArray.put(resultItem);
            }

            for (String region : wantedAreas) {
                log.info(region + " has no result. Adding null value");
                JSONObject resultItem = new JSONObject();

                JSONHelper.putValue(resultItem, "region", region);
                JSONHelper.putValue(resultItem, "year", getYears()[0]);

                JSONHelper.putValue(resultItem, "NullValue", true);

                JSONHelper.putValue(resultItem, "gender", "total");
                JSONHelper.putValue(resultItem, "indicator", getIndicator());

                resultArray.put(resultItem);
            }

            return resultArray;
        } catch (JSONException e) {
            log.error("Cannot convert data", e);
            throw new ActionException("Cannot convert data", e);
        }
    }

}
