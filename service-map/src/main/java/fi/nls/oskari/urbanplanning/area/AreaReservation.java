package fi.nls.oskari.urbanplanning.area;

import org.json.JSONException;
import org.json.JSONObject;
import fi.nls.oskari.urbanplanning.utils.JSONCopyHelper;
import fi.nls.oskari.util.JSONHelper;

public class AreaReservation extends Area {

	@Override
	protected String getName() {
		return "AreaReservations";
	}

	@Override
	protected String getUrlExtension() {
		return "areaReservations";
	}

	@Override
	protected AreaWrapper createAreaWrapper(JSONObject ob) throws JSONException {
		JSONObject outPutOb = new JSONObject();
		Long id = ob.optLong("MainMarkId") == 0 ? null : ob
				.getLong("MainMarkId");

		JSONHelper.putValue(outPutOb, "description",
				ob.getString("Description"));
		
		if(id != null) {
			JSONHelper.putValue(outPutOb, "mainMarkId", id.toString());
		}
		
		JSONCopyHelper.CopyValue(ob, "MainMarkId", outPutOb, "mainMarkId");
		
		JSONCopyHelper.CopyDoubleValue(ob, "PlanArea", outPutOb, "planArea");

		JSONCopyHelper.CopyDoubleValue(ob, "AreaSize", outPutOb, "areaSize");
		
		JSONCopyHelper.CopyDoubleValue(ob, "AreaPercent", outPutOb, "areaPercent");
		
		JSONCopyHelper.CopyDoubleValue(ob, "FloorSpace", outPutOb, "floorSpace");
		
		JSONCopyHelper.CopyDoubleValue(ob, "Efficiency", outPutOb, "efficiency");
		
		JSONCopyHelper.CopyDoubleValue(ob, "AreaChange", outPutOb, "areaChange");
		
		JSONCopyHelper.CopyDoubleValue(ob, "FloorSpaceChange", outPutOb, "floorSpaceChange");

		return new AreaWrapper(id, outPutOb);
	}

}
