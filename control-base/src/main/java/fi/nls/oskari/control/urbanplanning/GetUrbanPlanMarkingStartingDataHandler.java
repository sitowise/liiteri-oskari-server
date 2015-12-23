package fi.nls.oskari.control.urbanplanning;

import java.util.*;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.urbanplanning.params.MultiSelectListParameter;
import fi.nls.oskari.urbanplanning.service.UrbanPlanningApiService;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetUrbanPlanMarkingsStartingData")
public class GetUrbanPlanMarkingStartingDataHandler extends ActionHandler {
    private static final Logger log = LogFactory
            .getLogger(GetUrbanPlanMarkingHandler.class);
    private static final UrbanPlanningApiService apiService = new UrbanPlanningApiService();

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";

    private static final String MARK_TYPE = "type";
    private static final String AREA_TYPE = "areaType";

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        MarkingsStartingData data = new MarkingsStartingData();

        try {
            data.Merge(GetUrbanPlanData("municipality"));
            data.Merge(GetUrbanPlanData("standard"));

            String main = CreateResponse(data);
            final HttpServletResponse response = params.getResponse();
            response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
            response.setCharacterEncoding(OSKARI_ENCODING);
            ResponseHelper.writeResponse(params, main);
        } catch (Exception e) {

            throw new ActionException(
                    "Error during getting urban planning starting data");
        }
    }

    private MarkingsStartingData GetUrbanPlanData(String typeString)
            throws ServiceException {
        MarkingsStartingData result = new MarkingsStartingData();

        List<CommonParameter> parameters = new ArrayList<CommonParameter>();
        MultiSelectListParameter areaType = new MultiSelectListParameter(
                AREA_TYPE, "undergroundAreas,areaReservations",
                new ArrayList<String>(Arrays.asList("undergroundAreas",
                        "areaReservations")));
        CommonParameter type = new CommonParameter(MARK_TYPE, typeString);

        String resultStr = apiService.getMarkings(parameters, type, null,
                areaType, null);

        try {
            JSONArray resultArray = new JSONArray(resultStr);
            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject itemObject = resultArray.getJSONObject(i);

                if (itemObject.has("municipalityName")
                        && itemObject.has("municipalityId"))
                    result.Municipalities.put(
                            itemObject.getString("municipalityName"),
                            itemObject.getString("municipalityId"));
                if (!itemObject.isNull("mainMarkName")) {
                    result.MarkNames.put(itemObject.getString("mainMarkName"),
                            itemObject.getInt("mainMarkId"));
                    result.Names.put(itemObject.getString("name"),
                            itemObject.getInt("mainMarkId"));
                } else {
                    result.Names.put(itemObject.getString("name"), -1);
                }
            }
            result.MarkNames.put("ma", -1);
        } catch (JSONException e) {
            log.warn(e);
        }

        return result;
    }

    private String CreateResponse(MarkingsStartingData data) {

        SortedSet<Map.Entry<String, Integer>> sortedNames = new TreeSet<Map.Entry<String, Integer>>(
                new MarkNameComparator());

        JSONObject result = new JSONObject();

        JSONArray namesArray = new JSONArray();

        sortedNames.addAll(data.Names.entrySet());

        for (Map.Entry<String, Integer> name : sortedNames) {
            namesArray.put(name.getKey());
        }

        JSONArray municipalitiesArray = new JSONArray();
        for (String municipalityName : data.Municipalities.keySet()) {
            JSONObject itemObject = new JSONObject();
            JSONHelper.putValue(itemObject, "name", municipalityName);
            JSONHelper.putValue(itemObject, "id",
                    data.Municipalities.get(municipalityName));
            municipalitiesArray.put(itemObject);
        }

        JSONArray markNamesArray = new JSONArray();

        sortedNames.clear();
        sortedNames.addAll(data.MarkNames.entrySet());

        for (Map.Entry<String, Integer> markName : sortedNames) {
            markNamesArray.put(markName.getKey());
        }

        try {
            result.put("names", namesArray);
            result.put("municipalities", municipalitiesArray);
            result.put("markNames", markNamesArray);
        } catch (JSONException e) {
            log.warn(e);
        }

        return result.toString();
    }

    public class MarkingsStartingData {
        public SortedMap<String, Integer> Names = new TreeMap<String, Integer>();
        public Map<String, String> Municipalities = new TreeMap<String, String>();
        public SortedMap<String, Integer> MarkNames = new TreeMap<String, Integer>();

        public void Merge(MarkingsStartingData data) {
            Names.putAll(data.Names);
            Municipalities.putAll(data.Municipalities);
            MarkNames.putAll(data.MarkNames);
        }
    }

    public class MarkNameComparator implements
            Comparator<Map.Entry<String, Integer>> {
        @Override
        public int compare(Map.Entry<String, Integer> e1,
                Map.Entry<String, Integer> e2) {

            // sort underground areas to the end
            if (e1.getValue() == -1)
                return 1;
            if (e2.getValue() == -1)
                return -1;

            // we should never return 0 here
            return (e1.getValue() > e2.getValue() ? 1 : -1);
        }
    }
}
