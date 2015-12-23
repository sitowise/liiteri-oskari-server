package fi.nls.oskari.control.szopa.requests;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.szopa.FilterParser;
import fi.nls.oskari.control.szopa.RegionDefinition.RegionType;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONCopyHelper;
import fi.nls.oskari.util.JSONHelper;

public class FunctionalAreaAvailability extends SzopaRequest {
    private static Logger log = LogFactory
            .getLogger(FunctionalAreaAvailability.class);
    private FilterParser _filterParser = FilterParser.getInstance();

    @Override
    public String getName() {
        return "functionalAvailability";
    }

    @Override
    public String getRequestSpecificParams() {
        String url = "/areaTypes/" + getGroup()
                + "/functionalAreaAvailability/" + getAreaYear();

        if (getStandardFilterParam().length() > 0) {
            try {
                url += "?filter="
                        + URLEncoder.encode(_filterParser
                                .getFilterFromAreas(_filterParser
                                        .getAreasFromFilter(
                                                getStandardFilterParam(),
                                                RegionType.ADMINISTRATIVE)),
                                "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.warn(e, "Could not encode filter param");
            }
        }

        return url;
    }

    @Override
    public boolean getUseCache() {
        return true;
    }

    @Override
    public String getData() throws ActionException {
        if (getGroup().equalsIgnoreCase("finland")
                || getGroup().startsWith("grid")) { // api
                                                    // crashes
                                                    // for
                                                    // whole
                                                    // country
                                                    // and
                                                    // grid
            return "[]";
        } else {
            return super.getData();
        }
    }

    @Override
    protected String ConvertData(String data) throws ActionException {
        try {
            JSONArray array = JSONHelper.createJSONArray(data);
            JSONArray resultArray = new JSONArray();
            Map<String, List<String>> areas = new HashMap<String, List<String>>();

            if (getStandardFilterParam().length() > 0) {
                areas = _filterParser.getAreasFromFilter(
                        getStandardFilterParam(), RegionType.FUNCTIONAL);
            }

            log.info("Got #%s regions", array.length());

            for (int i = 0; i < array.length(); i++) {
                JSONObject inputItem = array.getJSONObject(i);
                JSONObject resultItem = new JSONObject();

                JSONHelper.putValue(resultItem, "id", inputItem.get("AreaType")
                        + ":" + inputItem.get("Id"));

                JSONCopyHelper.Copy(inputItem, "AlternativeId", resultItem,
                        "code");
                JSONCopyHelper.Copy(inputItem, "OrderNumber", resultItem,
                        "orderNumber");

                JSONArray availableFunctionalAreas = inputItem
                        .getJSONArray("AvailableFunctionalAreas");
                JSONArray functionalAreas = new JSONArray();
                int foundTypes = 0;

                for (int j = 0; j < availableFunctionalAreas.length(); ++j) {
                    String areaId = availableFunctionalAreas.getString(j);
                    if (areas.keySet().contains(areaId)) {
                        foundTypes++;
                        functionalAreas.put(areaId);
                    }
                }

                if (foundTypes == areas.size()) {
                    JSONHelper.putValue(resultItem, "functionalAreas",
                            functionalAreas);
                } else {
                    JSONHelper.putValue(resultItem, "functionalAreas",
                            new JSONArray());
                }

                resultArray.put(resultItem);
            }

            return resultArray.toString();
        } catch (JSONException e) {
            log.error("Cannot convert data", e);
            throw new ActionException("Cannot convert data", e);
        }
    }
}
