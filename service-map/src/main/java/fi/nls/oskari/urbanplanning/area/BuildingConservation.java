package fi.nls.oskari.urbanplanning.area;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.params.NumberParameter;
import fi.nls.oskari.urbanplanning.utils.JSONCopyHelper;
import fi.nls.oskari.urbanplanning.utils.PlanAction;
import fi.nls.oskari.util.JSONHelper;

public class BuildingConservation extends Area {

	@Override
	protected String getName() {
		return "BuildingConservations";
	}

	@Override
	protected String getUrlExtension() {
		return "buildingConservations";
	}

	@Override
	public JSONObject getDataObject(NumberParameter param, PlanAction action)
			throws ServiceException {
		if (param == null || (param != null && !param.Validate()))
			throw new ServiceException("Invalid parameter format");
		// To think about it
		if (action == PlanAction.PLAN
				&& !(Long.parseLong(param.getValue()) > 0)) {
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
		
		/*if (mainObjects.size() > 0) {
			List<AreaWrapper> mainObjectOutput = new ArrayList<AreaWrapper>();
			List<AreaWrapper> childObjects = getWrappedData(param, action,
					false);
			AreaWrapper notEmpty = findNotEmptyParent(mainObjects);
			if (notEmpty != null) {
				mainObjectOutput.add(notEmpty);
				for (AreaWrapper w : childObjects) {
					if (CheckIfChildObjectContainsValues(w))
						notEmpty.getChildren().add(w);
				}
			} else if (mainObjects.size() > 0) {
				mainObjectOutput.add(mainObjects.get(0));
			}

			JSONArray arr = new JSONArray();
			for (AreaWrapper w : mainObjectOutput) {
				arr.put(w.getJSONObject());
			}

			return arr;
			// }

		}
		return createFakeEmptyObject();*/
		
	}

	@Override
	protected AreaWrapper createAreaWrapper(JSONObject ob) throws JSONException {
		JSONObject outPutOb = new JSONObject();
		Long id = null;

		Long conservationTypeId = ob.optLong("ConservationTypeId") == 0 ? null
				: ob.getLong("ConservationTypeId");

		Long buildingCount = ob.optLong("BuildingCount") == 0 ? null : ob
				.getLong("BuildingCount");

		Long changeCount = ob.optLong("ChangeCount") == 0 ? null : ob
				.getLong("ChangeCount");

		JSONCopyHelper.CopyDoubleValue(ob, "ConservationTypeId", outPutOb, "conservationTypeId");
		
		JSONCopyHelper.CopyDoubleValue(ob, "BuildingCount", outPutOb, "buildingCount");

		JSONCopyHelper.CopyDoubleValue(ob, "ChangeCount", outPutOb, "changeCount");

		JSONHelper.putValue(outPutOb, "conservationTypeName",
				ob.getString("ConservationTypeName"));

		JSONCopyHelper.CopyDoubleValue(ob, "FloorSpace", outPutOb, "floorSpace");
		
		JSONCopyHelper.CopyDoubleValue(ob, "ChangeFloorSpace", outPutOb, "changeFloorSpace");

		AreaWrapper w = new AreaWrapper(id, outPutOb);
		w.setIsEmpty(conservationTypeId != null ? false : true);
		w.setBuildingCount(ob.optLong("BuildingCount") == 0 ? null
				: buildingCount);
		w.setChangeCount(changeCount == null ? null : changeCount);
		w.setFloorSpace(!((Double) ob.optDouble("FloorSpace"))
				.equals(Double.NaN) ? ((Double) ob.optDouble("FloorSpace"))
				: null);
		w.setChangeFloorSpace(!((Double) ob.optDouble("ChangeFloorSpace"))
				.equals(Double.NaN) ? ((Double) ob
				.optDouble("ChangeFloorSpace")) : null);
		if (ob.getString("ConservationTypeName") != null
				&& !ob.getString("ConservationTypeName").isEmpty()
				&& ob.getString("ConservationTypeName") != NULL_STRING) {
			w.setConservationTypeName(ob.getString("ConservationTypeName"));
		}
		return w;
	}

	private AreaWrapper findNotEmptyParent(List<AreaWrapper> mainObjects) {
		for (AreaWrapper w : mainObjects) {
			if (!w.getIsEmpty())
				return w;
		}
		return null;
	}

	private boolean CheckIfChildObjectContainsValues(AreaWrapper w) {
		return w.getBuildingCount() != null || w.getChangeCount() != null
				|| w.getChangeFloorSpace() != null || w.getFloorSpace() != null;
	}
	/*
	 * private AreaWrapper findBuildingConservationParent( List<AreaWrapper>
	 * mainObjects, String conservationTypeName) { for (AreaWrapper w :
	 * mainObjects) { if (conservationTypeName != null &&
	 * w.getConservationTypeName().equals(conservationTypeName)) return w; }
	 * 
	 * return null; }
	 */
	
	private JSONArray createFakeEmptyObject()
	{
		JSONObject ob = new JSONObject();
		JSONHelper.putValue(ob, "changeCount", JSONObject.NULL.toString());
		JSONHelper.putValue(ob, "conservationTypeName", "Yhteensä");
		JSONHelper.putValue(ob, "floorSpace", JSONObject.NULL.toString());
		JSONHelper.putValue(ob, "changeFloorSpace", JSONObject.NULL.toString());
		JSONHelper.putValue(ob, "buildingCount", JSONObject.NULL.toString());
		JSONHelper.putValue(ob, "changeCount", JSONObject.NULL.toString());
		JSONHelper.putValue(ob, "conservationTypeId", JSONObject.NULL.toString());
		JSONArray arr =new JSONArray();
		arr.put(ob);
		return arr;
	}
}
