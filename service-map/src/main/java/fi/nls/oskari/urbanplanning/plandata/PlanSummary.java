package fi.nls.oskari.urbanplanning.plandata;

import java.io.StringWriter;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.area.Area;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.urbanplanning.params.NumberParameter;
import fi.nls.oskari.urbanplanning.utils.JSONCopyHelper;
import fi.nls.oskari.urbanplanning.utils.PlanAction;
import fi.nls.oskari.util.JSONHelper;

public class PlanSummary extends PlanData{
	private static final Logger log = LogFactory.getLogger(PlanSummary.class);
	
	@Override
	protected String getName() {
		return "planSummary";
	}
	
	protected String getUrlExtension() {
		return "/plansummary";
	}
	
	
	@Override
	protected String buildUrl(List<CommonParameter> params) {
		StringWriter wr = new StringWriter();
		wr.write(_baseUrl);
		wr.write(getUrlExtension());
		return wr.toString();
	}
	
	@Override
	public String getData(List<CommonParameter> params) throws ServiceException {
		if (params != null && !validateInputParameters(params)) {
			throw new ServiceException("Invalid parameters");
		}
		try {
			String postContent = null;
			StringWriter wr = new StringWriter();
			wr.write("{ 'PlanIds': [");
			wr.write(params.get(0).getValue());
			wr.write("] }");
			postContent = wr.toString();
			String data = convertData(getPlansData(buildUrl(params), postContent));
			JSONObject ob = JSONHelper.createJSONObject(data);
			addAreaArrayToJSONObject(Area.getInstance(AREA_RESERVATION)
					.getData((NumberParameter) params.get(0), PlanAction.PLANSUMMARY),
					ob, AREA_RESERVATION);
			addAreaObjectToJSONObject(Area.getInstance(BUILDING_CONSERVATION)
					.getDataObject((NumberParameter) params.get(0), PlanAction.PLANSUMMARY),
					ob, BUILDING_CONSERVATION);
			addAreaObjectToJSONObject(Area.getInstance(UNDERGROUND_AREA)
					.getDataObject((NumberParameter) params.get(0), PlanAction.PLANSUMMARY),
					ob, UNDERGROUND_AREA);
			return ob.toString();

		} catch (IllegalArgumentException ex) {
			throw new ServiceException("Invalid parameter value", ex);
		}

		catch (JSONException ex) {
			throw new ServiceException("Error during preparing JSON Object", ex);
		}
	}
	
	
	
	@Override
	protected  String convertData(String inputData)
			throws JSONException {
		JSONObject ob = JSONHelper.createJSONObject(inputData);
		JSONObject outPutOb = new JSONObject();
		JSONHelper.putValue(outPutOb, "planCount", ob.getLong("PlanCount"));
		
		JSONCopyHelper.CopyDoubleValue(ob, "PlanArea", outPutOb, "planArea");
		JSONCopyHelper.CopyDoubleValue(ob, "UndergroundArea", outPutOb, "undergroundArea");
		JSONCopyHelper.CopyDoubleValue(ob, "PlanAreaNew", outPutOb, "planAreaNew");
		JSONCopyHelper.CopyDoubleValue(ob, "PlanAreaChange", outPutOb, "planAreaChange");
		
		JSONCopyHelper.CopyDoubleValue(ob, "CoastlineLength", outPutOb, "coastlineLength");
		JSONCopyHelper.CopyLongValue(ob, "BuildingCountOwn", outPutOb, "buildingCountOwn");
		JSONCopyHelper.CopyLongValue(ob, "BuildingCountOther", outPutOb, "buildingCountOther");
		JSONCopyHelper.CopyLongValue(ob, "BuildingCountOwnHoliday", outPutOb, "buildingCountOwnHoliday");
		JSONCopyHelper.CopyLongValue(ob, "BuildingCountOtherHoliday", outPutOb, "buildingCountOtherHoliday");		
		
		JSONCopyHelper.CopyDoubleValue(ob, "DurationAverage", outPutOb, "durationAverage");
		JSONCopyHelper.CopyDoubleValue(ob, "DurationMedian", outPutOb, "durationMedian");
		
		log.info(outPutOb.toString());

		return outPutOb.toString();
	}

}
