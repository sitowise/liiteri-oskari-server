package fi.nls.oskari.urbanplanning.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.marking.Marking;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.urbanplanning.params.IdParameter;
import fi.nls.oskari.urbanplanning.params.ListIdParameter;
import fi.nls.oskari.urbanplanning.params.MultiSelectListParameter;
import fi.nls.oskari.urbanplanning.people.People;
import fi.nls.oskari.urbanplanning.plandata.PlanData;
import fi.nls.oskari.urbanplanning.region.Region;
import fi.nls.oskari.urbanplanning.utils.*;
import fi.nls.oskari.util.JSONHelper;

public class UrbanPlanningApiService {
	
	private static final Logger log = LogFactory.getLogger(UrbanPlanningApiService.class);

	public String getUrbanPlanningStartingData() throws ServiceException {
		SelectAreaCount selectAreaCount = new SelectAreaCount();
		JSONObject ob = new JSONObject();
		JSONHelper.putValue(ob, "approver", getPlanApprovers());
		JSONHelper.putValue(ob, "planType", getPlanTypes());
		JSONHelper.putValue(ob, "timeSelectorType", getTimeSelectorTypes());

		String regionTypes = Region.createRegion().getData(null);
		JSONArray array = JSONHelper.createJSONArray(regionTypes);

		for (int i = 0; i < array.length(); i++) {
			try {
				JSONObject obLow = array.getJSONObject(i);
				JSONHelper.putValue(
						obLow,
						"data",
						createJSONRegionObject(
								Region.getInstance(obLow.getString("type"))
										.getData(null), Region
										.getInstance(obLow.getString("type")),
								selectAreaCount, false));
			} catch (JSONException e) {
				throw new ServiceException("Error during JSON cretaion");
			}

		}
		JSONHelper.putValue(
				ob,
				"region",
				createJSONRegionObject(array.toString(), Region.createRegion(),
						selectAreaCount, false));
		JSONHelper.putValue(ob, "allRegionCount", selectAreaCount.getValue());
		return ob.toString();
	}

	public String getUrbanPlanningRegionDetailData(String regionType,
			List<CommonParameter> params) throws ServiceException {
		Region reg = Region.getInstance(regionType);
		SelectAreaCount selectAreaCount = new SelectAreaCount();
		return createJSONRegionObject(reg.getData(params), reg,
				selectAreaCount, true).toString();
	}

	private JSONObject getPlanApprovers() {

		JSONArray array = new JSONArray();

		for (PlanApprover app : PlanApprover.values()) {
			JSONObject ob = new JSONObject();
			JSONHelper.putValue(ob, "value", app.getValue());
			JSONHelper.putValue(ob, "name", app.getName());
			array.put(ob);
		}
		JSONObject main = new JSONObject();
		JSONHelper.putValue(main, "data", array);
		JSONHelper.putValue(main, "count", PlanApprover.values().length);
		return main;

	}

	private JSONObject getPlanTypes() {
		JSONArray array = new JSONArray();

		for (PlanType type : PlanType.values()) {
			JSONObject ob = new JSONObject();
			JSONHelper.putValue(ob, "value", type.getValue());
			JSONHelper.putValue(ob, "name", type.getName());
			array.put(ob);
		}
		JSONObject main = new JSONObject();
		JSONHelper.putValue(main, "data", array);
		JSONHelper.putValue(main, "count", PlanApprover.values().length);
		return main;
	}

	private JSONObject getTimeSelectorTypes()

	{
		JSONArray array = new JSONArray();

		for (TimeSelectorType type : TimeSelectorType.values()) {
			JSONObject ob = new JSONObject();
			JSONHelper.putValue(ob, "value", type.getValue());
			JSONHelper.putValue(ob, "name", type.getName());
			array.put(ob);
		}
		JSONObject main = new JSONObject();
		JSONHelper.putValue(main, "data", array);
		JSONHelper.putValue(main, "count", PlanApprover.values().length);
		return main;
	}

	private JSONObject createJSONRegionObject(String data, Region region,
			SelectAreaCount selectAreaCount, boolean putRegionName) {
		JSONObject ob = new JSONObject();
		JSONArray arr = JSONHelper.createJSONArray(data);
		JSONHelper.putValue(ob, "data", arr);
		JSONHelper.putValue(ob, "count", arr.length());
		if (putRegionName)
			JSONHelper.putValue(ob, "name", region.getTitle());
		selectAreaCount.setValue(arr.length());
		return ob;
	}

	public String getPlans(List<CommonParameter> params)
			throws ServiceException {
		PlanData p = PlanData.getInstance("planList");
		return p.getData(params);
	}

	public String getPlan(IdParameter id) throws ServiceException {	
		String key = "UrbanPlanningApiService:plan:" + id.getValue();
		String cacheValue = JedisManager.get(key);
		if (cacheValue != null)
			return cacheValue;
		
		List<CommonParameter> params = new ArrayList<CommonParameter>();
		params.add(id);
		PlanData p = PlanData.getInstance("plan");
		String result = p.getData(params);
		
		JedisManager.setex(key, 3600, result); // 3600 s = 60 s * 60 min = 1 h
		
		return result;
	}

	public String getPlanSummary(ListIdParameter id) throws ServiceException {
		List<CommonParameter> params = new ArrayList<CommonParameter>();
		params.add(id);
		PlanData p = PlanData.getInstance("planSummary");
		return p.getData(params);
	}

	public String getMarkings(List<CommonParameter> params,
			CommonParameter type, IdParameter municipalityId,
			MultiSelectListParameter areaType, CommonParameter mainMarkName) throws ServiceException {
		
		
		return Marking.getInstance(areaType.getValue()).getData(params, type,
				municipalityId,mainMarkName);
	}
	
	public String getPeople(List<CommonParameter> params)
			throws ServiceException {
		People p = new People(params);
		JSONArray people = p.getData();
		
		if (p.areAdditionalDataNeeded()) {
			JSONArray regions = this.getRegions(params);
			try {
				people = p.mergeWithRegions(people, regions);	
			}
			catch (JSONException e) {
				/* suppress */
			}
		}		
		
		return people.toString();
	}
	
	private JSONArray getRegions(List<CommonParameter> params) throws ServiceException {
		JSONArray result = new JSONArray();
		final String ELY = "ely";
		final String MUNICIPALITY = "municipality";	
		
		Vector<CommonParameter> innerParams = new Vector<CommonParameter>();
		
		for (CommonParameter param : params)
		{
			if (param.getName().equals(ELY)) {
				innerParams.add(param);
			}
		}		
		String regionData = this.getUrbanPlanningRegionDetailData(MUNICIPALITY, innerParams);
		try
		{
			JSONObject regionObj = new JSONObject(regionData);
			result = regionObj.getJSONArray("data");
		} catch (JSONException e)
		{
			log.warn(e, "Cannot parse region data");
		}
		
		for (CommonParameter param : params)
		{
			if (param.getName().equals(MUNICIPALITY)) {
				String municipalityId = param.getValue();
				for (int i = 0; i < result.length(); i++)
				{
					try
					{
						JSONObject item = result.getJSONObject(i);
						if (municipalityId.equals(item.get("id"))) {
							result = new JSONArray();
							result.put(item);
							break;
						}
					} catch (JSONException e)
					{
						log.warn(e, "Cannot parse region data");
					}
				}
			}
		}		
		
		return result;
	}

	private class SelectAreaCount {
		private long value = 0;

		public void setValue(long value) {
			this.value += value;
		}

		public long getValue() {
			return value;
		}
	}

}
