package fi.nls.oskari.urbanplanning.region;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.util.JSONHelper;

public class TypedRegion extends Region{
	
	@Override
	protected String ConvertData(String inputData) throws JSONException {

		JSONArray array = JSONHelper.createJSONArray(inputData);
		JSONArray outputArray = new JSONArray();

		for (int i = 0; i < array.length(); i++) {
			JSONObject outputOb = new JSONObject();
			JSONObject ob = array.getJSONObject(i);
			JSONHelper.putValue(outputOb, "name", ob.getString("Name").trim());
			JSONHelper.putValue(outputOb, "type", ob.getString("RegionType").trim());
			JSONHelper.putValue(outputOb, "id", ob.getString("Id").trim());
			JSONHelper.putValue(outputOb, "orderNumber", ob.has("OrderNumber") && !ob.isNull("OrderNumber") ? ob.getInt("OrderNumber") : 0);
			outputArray.put(outputOb);
		}

		return outputArray.toString();
	}
}
