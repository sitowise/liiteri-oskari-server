package fi.nls.oskari.util;

import org.json.JSONException;
import org.json.JSONObject;

public final class JSONCopyHelper
{	
	public static void Copy(JSONObject source, String sourceKey, JSONObject destination, String destKey) throws JSONException {
		Object sourceValue = source.get(sourceKey);
		destination.put(destKey, sourceValue);	
	}	
	
	public static void Copy(JSONObject source, String sourceKey, JSONObject destination, String destKey, Object defaultValue) throws JSONException {		
		Object sourceValue = source.has(sourceKey) ? source.get(sourceKey) : defaultValue;
		destination.put(destKey, sourceValue);	
	}	
	
	public static void CopyAsString(JSONObject source, String sourceKey, JSONObject destination, String destKey) throws JSONException {
		String sourceValue = source.getString(sourceKey);
		destination.put(destKey, sourceValue);	
	}	
	
	public static void LanguageAwareCopy(JSONObject source, String sourceKey, JSONObject destination, String destKey) throws JSONException {
		Object sourceValue = source.get(sourceKey);
		JSONObject languageArray = createLanguageJSONObject(sourceValue);
		destination.put(destKey, languageArray);
	}
	
	public static void CopyDoubleValue(JSONObject source, String sourceKey, JSONObject destination, String destKey) throws JSONException {
		Double sourceValue = source.optDouble(sourceKey);
		
		if (!sourceValue.equals(Double.NaN)) {
			destination.put(destKey, sourceValue.toString());
		}
		else {
			destination.put(destKey, JSONObject.NULL);
		}		
	}
	
    public static final JSONObject createLanguageJSONObject(Object value) throws JSONException {
    	JSONObject result = new JSONObject();
    	
    	String[] languages = { "fi", "en", "sv" };
    	
    	for (String language : languages) 
    	{		
    		result.put(language, value);
		}
    	
    	return result;
    }
}
