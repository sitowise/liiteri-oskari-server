package fi.nls.oskari.urbanplanning.area;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.params.NumberParameter;
import fi.nls.oskari.urbanplanning.utils.JSONCopyHelper;
import fi.nls.oskari.urbanplanning.utils.PlanAction;
import fi.nls.oskari.util.JSONHelper;

public class UndergroundArea extends Area {

	@Override
	protected String getName() {
		return "UndergroundAreas";
	}
	
	@Override
	protected String getUrlExtension() {
		return "undergroundAreas";
	}
	
	@Override
	protected AreaWrapper createAreaWrapper(JSONObject ob) throws JSONException {
		JSONObject outPutOb = new JSONObject();
		Long id = null;
		JSONHelper.putValue(outPutOb, "description",
				ob.getString("Description"));
		
		JSONCopyHelper.CopyDoubleValue(ob, "AreaSize", outPutOb, "areaSize");
		
		JSONCopyHelper.CopyDoubleValue(ob, "AreaPercent", outPutOb, "areaPercent");
		
		JSONCopyHelper.CopyDoubleValue(ob, "FloorSpace", outPutOb, "floorSpace");
		
		JSONCopyHelper.CopyDoubleValue(ob, "AreaChange", outPutOb, "areaChange");
		
		JSONCopyHelper.CopyDoubleValue(ob, "FloorSpaceChange", outPutOb, "floorSpaceChange");
			
		return new AreaWrapper(id,outPutOb);
	}
	
	@Override
	public JSONObject getDataObject(NumberParameter param, PlanAction action)
			throws ServiceException {
		if (param == null || (param != null && !param.Validate()))
			throw new ServiceException("Invalid parameter format");
		// To think about it
		if(action == PlanAction.PLAN && !(Long.parseLong(param.getValue())>0))
		{
			throw new ServiceException("Invalid parameter format");
		}
		
		List<AreaWrapper> mainObjects = getWrappedData(param, action, true);
		List<AreaWrapper> childObjects = getWrappedData(param, action, false);
		
		JSONArray arrMain = new JSONArray();
		for (AreaWrapper w : mainObjects) {
			arrMain.put(w.getJSONObject());
		}
		
		JSONArray arrSub = new JSONArray();
		for (AreaWrapper w : childObjects) {
			arrSub.put(w.getJSONObject());
		}
		
		JSONObject outputObj = new JSONObject();
		try {
			outputObj.put("main", arrMain);
			outputObj.put("sub", arrSub);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return outputObj;

		/*List<AreaWrapper> mainObjects = getWrappedData(param, action, true);
		
		List<AreaWrapper> childObjects = getWrappedData(param, action, false);
		if (mainObjects.size() > 0) {
			for (AreaWrapper w : childObjects) {
	
				mainObjects.get(0).getChildren().add(w);
			}

			JSONArray arr = new JSONArray();
			for (AreaWrapper w : mainObjects) {
				arr.put(w.getJSONObject());
			}

			return arr;
		}
		return null;*/
	}
	
	
}
