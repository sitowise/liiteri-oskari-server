package fi.nls.oskari.control.szopa;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;

public final class JSONSzopaHelper {

    private static Logger log = LogFactory.getLogger(JSONSzopaHelper.class);

    public static final JSONObject createLanguageJSONObject(String value) {
        JSONObject result = new JSONObject();

        String[] languages = { "fi", "en", "sv" };

        for (String language : languages) {
            JSONHelper.putValue(result, language, value);
        }

        return result;
    }

    public static final JSONArray createJSONArrayFromArray(
            Iterable<String> values) {
        JSONArray array = new JSONArray();

        for (String value : values) {
            array.put(value);
        }

        return array;
    }

    public static final String[] getArrayFromJSONArray(JSONArray array)
            throws JSONException {
        List<String> result = new ArrayList<String>();

        for (int i = 0; i < array.length(); i++) {
            String itemName = array.getString(i);
            result.add(itemName);
        }

        return result.toArray(new String[result.size()]);
    }

    public static final void CopyText(JSONObject source,
            String sourcePropertyName, JSONObject destination,
            String destinationPropertyName) throws JSONException {
        CopyText(source, sourcePropertyName, destination,
                destinationPropertyName, false);
    }

    public static final void CopyText(JSONObject source,
            String sourcePropertyName, JSONObject destination,
            String destinationPropertyName, boolean addLanguageVersion)
            throws JSONException {
        if (!source.has(sourcePropertyName)) {
            String message = String.format("Cannot find property %s in source",
                    sourcePropertyName);
            log.warn(message);
            throw new JSONException(message);
        }

        String sourcePropertyValue = source.getString(sourcePropertyName);

        if (addLanguageVersion) {
            destination.put(destinationPropertyName, JSONSzopaHelper
                    .createLanguageJSONObject(sourcePropertyValue));
        } else {
            destination.put(destinationPropertyName, sourcePropertyValue);
        }
    }
}
