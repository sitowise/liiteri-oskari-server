package fi.nls.oskari.urbanplanning.plandata;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.utils.JSONCopyHelper;
import fi.nls.oskari.util.JSONHelper;

public class PlanList extends PlanData {

	private static final Logger log = LogFactory.getLogger(PlanList.class);
	
	@Override
	protected String getName() {
		return "planList";
	}
	
	@Override
	protected void validateResult(String result) throws ServiceException {
		JSONArray array = JSONHelper.createJSONArray(result);
		if (array != null && array.length() > 2100)
			throw new ServiceException("max_limit_reached");
	}

	@Override
	protected String convertData(String inputData) throws JSONException {
		JSONArray array = JSONHelper.createJSONArray(inputData);
		JSONArray outputArray = new JSONArray();
		for (int i = 0; i < array.length(); i++) {
			JSONObject ob = array.getJSONObject(i);					
			JSONObject outPutOb = new JSONObject();
			
			JSONCopyHelper.CopyValue(ob, "Id", outPutOb, "id");
			JSONCopyHelper.CopyValue(ob, "Name", outPutOb, "name");
			JSONCopyHelper.CopyValue(ob, "MunicipalityPlanId", outPutOb, "municipalityPlanId");
			JSONCopyHelper.CopyValue(ob, "GeneratedPlanId", outPutOb, "generatedPlanId");
			JSONCopyHelper.CopyValue(ob, "MunicipalityId", outPutOb, "municipalityId");
			JSONCopyHelper.CopyValue(ob, "TyviId", outPutOb, "tyviId");
			
			String formattedMunicipalityId = new DecimalFormat("000").format(ob.getDouble("MunicipalityId"));
			JSONHelper.putValue(outPutOb, "municipality", formattedMunicipalityId + " " + ob.getString("MunicipalityName"));
			
			try {				
				JSONCopyHelper.CopyDateValue(ob, "ApprovalDate", outPutOb, "approvalDate");
				JSONCopyHelper.CopyDateValue(ob, "ProposalDate", outPutOb, "proposalDate");
				JSONCopyHelper.CopyDateValue(ob, "InitialDate", outPutOb, "initialDate");
				JSONCopyHelper.CopyDateValue(ob, "FillDate", outPutOb, "fillDate");							
			} 
			catch (Exception ex) 
			{
				throw new JSONException("Error during date parsing");
			}			
			
			outputArray.put(outPutOb);
		}

		return outputArray.toString();
	}
	


}
