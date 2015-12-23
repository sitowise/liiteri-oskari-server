package fi.nls.oskari.urbanplanning.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public final class JSONCopyHelper
{
	private static final String NULL_STRING = "null";
	
	public static Object formatDateNull(String inputDateString) throws ParseException {
		if (inputDateString != null && !inputDateString.isEmpty()
				&& !inputDateString.equals(NULL_STRING)) {
                        // input is ISO 8061, 2012-10-16T00:00:00
			SimpleDateFormat inputDateFormat =
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date dt = inputDateFormat.parse(inputDateString);
		    	// ISO 8601 output, but just the date
			return new SimpleDateFormat("yyyy-MM-dd").format(dt);
		} else
			return JSONObject.NULL;
	}
	
	public static void CopyValue(JSONObject source, String sourceKey, JSONObject destination, String destKey) throws JSONException {
		Object sourceValue = source.get(sourceKey);
		destination.put(destKey, sourceValue);	
	}
	
	public static void CopyDateValue(JSONObject source, String sourceKey, JSONObject destination, String destKey) throws JSONException, ParseException {
		String sourceValue = source.optString(sourceKey);
		Object formatedValue = formatDateNull(sourceValue);
		destination.put(destKey, formatedValue);	
	}
	
	public static void CopyDoubleValue(JSONObject source, String sourceKey, JSONObject destination, String destKey) throws JSONException {
		Double sourceValue = source.optDouble(sourceKey);
		
		if (IsJSONValueNotEmpty(source.optString(sourceKey)) && !sourceValue.equals(Double.NaN)) {
			destination.put(destKey, sourceValue);
		}		
	}
	
	public static void CopyLongValue(JSONObject source, String sourceKey, JSONObject destination, String destKey) throws JSONException {
		String sourceValue = source.optString(sourceKey);
		
		if (IsJSONValueNotEmpty(sourceValue)) {
			destination.put(destKey, source.getLong(sourceKey));
		}	
	}	
	
	public static boolean IsJSONValueNotEmpty(String value) {
		return value != null && !value.equals(NULL_STRING) && !value.isEmpty();
	}
}
