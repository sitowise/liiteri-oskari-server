package fi.nls.oskari.control.twowaystats.requests;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.control.ActionException;
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
public class IndicatorData extends TwowayRequest {

    private static Logger log = LogFactory.getLogger(IndicatorData.class);

    public IndicatorData() {

    }

    public boolean isValid() {
        return getType() != null && getType().isEmpty();
    }

    @Override
    public String getName() {
        return "data";
    }

    @Override
    public String getRequestSpecificParams() {
        StringWriter writer = new StringWriter();
        String ret = "";
        try {
            writer.write("/commuteStatistics/");
            writer.write(URLEncoder.encode(getIndicator(), "UTF-8"));
            writer.write("/?type=");
            writer.write(URLEncoder.encode(getType(), "UTF-8"));
            writer.write("&years=");
            writer.write(URLEncoder.encode(StringUtils.join(getYears(), ","),
                    "UTF-8"));
            writer.write("&group=");
            writer.write(URLEncoder.encode(getGroup(), "UTF-8"));
            writer.write("&gender=");
            String gender = getGender();
            if ("total".equalsIgnoreCase(gender)) {
                gender = "0";
            } else if ("male".equalsIgnoreCase(gender)) {
                gender = "1";
            } else if ("female".equalsIgnoreCase(gender)) {
                gender = "2";
            } else {
                gender = "0";
            }
            writer.write(URLEncoder.encode(gender, "UTF-8"));
            writer.write("&work_filter=");
            writer.write(URLEncoder.encode(getWorkFilter(), "UTF-8"));
            writer.write("&home_filter=");
            writer.write(URLEncoder.encode(getHomeFilter(), "UTF-8"));
            ret = writer.toString();
        } catch (UnsupportedEncodingException e) {
            log.error(e, "Error writing params");
        }
        return ret;
    }

    @Override
    public String getData() throws ActionException {
        final String cachedData = TryGetCacheDataIfDesired();
        if (cachedData != null)
            return cachedData;

        try {
            final JSONArray result = new JSONArray();

            try {
                String data = super.getData();
                final JSONArray array = ConvertDataToJSON(data, getGroup()
                        .split(":")[1]);
                addRange(result, array);
            } catch (ActionException ae) {
                log.warn("Cannot get part request data " + ae.toString());
                throw ae;
            }

            String resultStr = result.toString();

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

            log.info("Got", inputArray.length(), "statistics results");

            for (int i = 0; i < inputArray.length(); i++) {
                JSONObject inputItem = inputArray.getJSONObject(i);
                JSONObject resultItem = new JSONObject();

                String region = groupName + ":" + inputItem.get("AreaId");
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
                JSONHelper.putValue(resultItem, "gender", getGender());
                JSONHelper.putValue(resultItem, "indicator", getIndicator());
                JSONHelper.putValue(resultItem, "type", getType());

                resultArray.put(resultItem);
            }

            return resultArray;
        } catch (JSONException e) {
            log.error("Cannot convert data", e);
            throw new ActionException("Cannot convert data", e);
        }
    }

}
