package fi.nls.oskari.control.twowaystats.requests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.szopa.JSONSzopaHelper;
import fi.nls.oskari.control.szopa.RegionDefinition;
import fi.nls.oskari.control.szopa.RegionService;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONCopyHelper;
import fi.nls.oskari.util.JSONHelper;

public class Indicators extends TwowayRequest {

    private static Logger log = LogFactory.getLogger(Indicators.class);
    private RegionService _regionService = RegionService.getInstance();

    @Override
    public String getName() {
        return "indicators";
    }

    @Override
    public String getRequestSpecificParams() {
        return "/commuteStatistics";
    }

    // @Override
    // public String getData() throws ActionException {
    // return ConvertData("");
    //
    // // final String format = getFormat();
    // // final User user = getUser();
    // //
    // // if ("tree".equals(format) && user != null) {
    // // List<JSONObject> themes = new ArrayList<JSONObject>();
    // //
    // // try {
    // // for (GroupingTheme t :
    // groupingThemesService.getTopLevelStatisticsThemes(user.getId())) {
    // // JSONObject r = themeToJSON(t);
    // // if(r != null)
    // // themes.add(r);
    // // }
    // // } catch (ServiceException e) {
    // // throw new ActionException("Error getting statistics themes", e);
    // // }
    // //
    // // JSONObject ret = new JSONObject();
    // //
    // // JSONHelper.putValue(ret, "themes", new JSONArray(themes));
    // //
    // // return ret.toString();
    // // }
    // // return super.getData();
    // }
    //
    @Override
    protected String ConvertData(String data) throws ActionException {
        try {
            return Convert(data);
        } catch (JSONException e) {
            log.error("Cannot convert data", e);
            throw new ActionException("Cannot convert data", e);
        }
    }

    private String Convert(String data) throws JSONException {

        JSONArray resultArray = new JSONArray();
        JSONArray inputArray = JSONHelper.createJSONArray(data);

        log.info("Got #%s statistics", inputArray.length());

        for (int i = 0; i < inputArray.length(); i++) {

            // put one static two way statistic to list
            // TODO: replace with actual data from API
            JSONObject itemResult = new JSONObject();
            JSONObject inputItem = inputArray.getJSONObject(i);

            String name = inputItem.getString("Name");

            JSONCopyHelper.Copy(inputItem, "Id", itemResult, "id");
            JSONHelper.putValue(itemResult, "name",
                    JSONSzopaHelper.createLanguageJSONObject(name));
            JSONHelper.putValue(itemResult, "description", JSONSzopaHelper
                    .createLanguageJSONObject(inputItem
                            .getString("Description")));

            // TODO: by default all region categories are taken
            JSONObject classifications = new JSONObject();
            JSONObject regionClassifications = new JSONObject();
            JSONArray regionCategoriesArray = JSONSzopaHelper
                    .createJSONArrayFromArray(getRegionCategories());
            JSONHelper.putValue(regionClassifications, "values",
                    regionCategoriesArray);
            JSONHelper.putValue(classifications, "region",
                    regionClassifications);

            JSONArray typeClassifications = new JSONArray();

            JSONArray inputTypes = inputItem
                    .getJSONArray("CommuteStatisticsTypes");

            for (int j = 0; j < inputTypes.length(); j++) {
                JSONObject inputType = inputTypes.getJSONObject(j);
                JSONObject type = new JSONObject();
                JSONCopyHelper.Copy(inputType, "Id", type, "id");
                JSONCopyHelper.Copy(inputType, "Description", type, "name");
                typeClassifications.put(type);
            }

            JSONHelper.putValue(classifications, "type", typeClassifications);

            JSONHelper.putValue(itemResult, "title",
                    JSONSzopaHelper.createLanguageJSONObject(name));

            JSONHelper.putValue(itemResult, "classifications", classifications);

            JSONArray years = convertTimePeriods(inputItem
                    .getJSONArray("CommuteStatisticsYears"));
            JSONHelper.putValue(itemResult, "years", years);
            JSONHelper.putValue(itemResult, "gridYears", years);

            Map<String, HashSet<Integer>> dataSourcesMap = gatherDataSources(inputItem);
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
            JSONHelper.putValue(itemResult, "dataSources", dataSources);

            resultArray.put(itemResult);
        }
        return resultArray.toString(1);
    }

    private List<String> getRegionCategories() {
        ArrayList<String> result = new ArrayList<String>();
        for (RegionDefinition definiton : _regionService.GetAllRegions()) {
            result.add(definiton.getId());
        }
        return result;
    }

    protected static JSONArray convertTimePeriods(JSONArray source)
            throws JSONException {
        JSONArray result = new JSONArray();

        for (int i = 0; i < source.length(); i++) {
            JSONObject itemSource = source.getJSONObject(i);
            String yearId = itemSource.getString("Year");
            result.put(yearId);
        }

        return result;
    }

    protected static Map<String, HashSet<Integer>> gatherDataSources(
            JSONObject source) throws JSONException {
        HashMap<String, HashSet<Integer>> map = new HashMap<String, HashSet<Integer>>();

        JSONArray timePeriods = source.getJSONArray("CommuteStatisticsYears");
        for (int i = 0; i < timePeriods.length(); i++) {
            JSONObject itemSource = timePeriods.getJSONObject(i);
            Integer id = itemSource.getInt("Year");
            JSONArray dataSources = itemSource.getJSONArray("DataSources");
            for (int k = 0; k < dataSources.length(); k++) {
                String dataSource = dataSources.getString(k);
                if (!map.containsKey(dataSource))
                    map.put(dataSource, new HashSet<Integer>());

                map.get(dataSource).add(id);
            }
        }

        return map;
    }
}
