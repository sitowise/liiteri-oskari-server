package fi.nls.oskari.control.permission;

import org.json.JSONObject;

import fi.nls.oskari.util.JSONHelper;

public class JSONPermissionHelper {

	public static JSONObject createPermissionJSONObject(String name, long id) {
		JSONObject ob = new JSONObject();
		JSONHelper.putValue(ob, "id", id);
		JSONHelper.putValue(ob, "name", name);
		return ob;

	}
}
