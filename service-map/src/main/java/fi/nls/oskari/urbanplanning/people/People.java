package fi.nls.oskari.urbanplanning.people;

import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.urbanplanning.utils.JSONCopyHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;

public class People {
	private static final Logger log = LogFactory.getLogger(People.class);
	protected static String _baseUrl = PropertyUtil.get("urbanPlanning.baseurl");
	private static final String PERSON_TYPE = "personType";
	private static final String Municipality_Contact = "MunicipalityContact";
    private static final String AUTHORIZED_ONLY = "authorizedOnly";
	
	private final List<CommonParameter> _params;
	
	public People(List<CommonParameter> params) {
		_params = params != null ? params : new Vector<CommonParameter>();
	}
	
	public JSONArray getData() throws ServiceException {
		if (!validateInputParameters(_params)) {
			throw new ServiceException("Invalid parameters");
		}
		final String url = buildUrl(_params);
		try {
			String data = getPeopleData(url);
			return convertData(data);
		} catch (JSONException e) {
			throw new ServiceException("Error during JSON object parsing", e);
		}
	}
	
	public boolean areAdditionalDataNeeded() {
		boolean result = false;
		
		for (CommonParameter param : _params)
		{
			if (param.getName().equals(PERSON_TYPE) && param.getValue().equals(Municipality_Contact)) {
				result = true;
				break;
			}
		}		
		return result;
	}
	
	protected boolean validateInputParameters(List<CommonParameter> params) {

		for (CommonParameter p : params) {
			if (!p.Validate())
				return false;
		}

		return true;
	}
	
	protected String buildUrl(List<CommonParameter> params) {
		StringWriter wr = new StringWriter();
		wr.write(_baseUrl);
		wr.write(getUrlExtension());
		if (params != null) {

			for (CommonParameter p : params) {

				if (params.indexOf(p) == 0)
					wr.write("?");
				else
					wr.write("&");
				wr.write(p.getName());
				wr.write("=");
				wr.write(p.getValue());
			}

		}
		return wr.toString();
	}
	
	protected String getUrlExtension() {
		return "/people";
	}
	
	protected String getPeopleData(String url) throws ServiceException {

		HttpURLConnection con = null;
		try {
			con = IOHelper.getConnection(url);
			final String data = IOHelper.readString(con.getInputStream());
			return data;
		} catch (Exception e) {
			throw new ServiceException(
					"Couldn't proxy request to Urban planning API server", e);
		} finally {
			try {
				con.disconnect();
			} catch (Exception ignored) {
			}
		}
	}

	protected JSONArray convertData(String inputData) throws JSONException {
		JSONArray array = JSONHelper.createJSONArray(inputData);
		JSONArray outPutArray = new JSONArray();
		
		boolean authorizedOnly = false;
		for (CommonParameter param : _params)
		{
		    if (param.getName().equals(AUTHORIZED_ONLY) && param.getValue().equals("true")) {
		        authorizedOnly = param.getValue().equals("true");
		        break;
		    }
		}   

		for (int i = 0; i < array.length(); i++) {
			JSONObject source = array.getJSONObject(i);
			JSONObject destination = new JSONObject();
			
			JSONCopyHelper.CopyValue(source, "City", destination, "city");
			JSONCopyHelper.CopyValue(source, "MunicipalityName", destination, "municipalityName");
			JSONCopyHelper.CopyValue(source, "MunicipalityId", destination, "municipalityId");
			JSONCopyHelper.CopyValue(source, "Email", destination, "email");
			JSONCopyHelper.CopyValue(source, "Fax", destination, "fax");
			JSONCopyHelper.CopyValue(source, "Office", destination, "office");
			JSONCopyHelper.CopyValue(source, "OrganizationName", destination, "organizationName");
			JSONCopyHelper.CopyValue(source, "PersonType", destination, "personType");
			JSONCopyHelper.CopyValue(source, "Phone", destination, "phone");
			JSONCopyHelper.CopyValue(source, "VatNumber", destination, "vatNumber");
			JSONCopyHelper.CopyValue(source, "ConsultAuthorized", destination, "consultAuthorized");
			JSONHelper.putValue(destination, "personName", source.getString("Lastname") + " " + source.getString("Firstname"));
			JSONHelper.putValue(destination, "address", source.getString("StreetName") + ", " + source.getString("ZipCode") + " " + source.getString("City"));
			
			if(!authorizedOnly || destination.getBoolean("consultAuthorized")) {
			    outPutArray.put(destination);
			}
		}

		return outPutArray;
	}

	public JSONArray mergeWithRegions(JSONArray people, JSONArray regions) throws JSONException
	{
		HashSet<Integer> presentMunicipalities = new HashSet<Integer>();
		for (int i = 0; i < people.length(); i++)
		{
			JSONObject item = people.getJSONObject(i);
			int id = item.getInt("municipalityId");
			presentMunicipalities.add(id);
		}
		for (int i = 0; i < regions.length(); i++)
		{
			JSONObject item = regions.getJSONObject(i);
			int id = item.getInt("id");
			if (!presentMunicipalities.contains(id)) {
				String name = item.getString("name");
				JSONObject newItem = new JSONObject();
				newItem.put("municipalityId", id);
				newItem.put("municipalityName", name);
				newItem.put("vatNumber", "");
				newItem.put("organizationName", "");
				newItem.put("office", "");
				newItem.put("personName", "");
				newItem.put("address", "");
				newItem.put("email", "");
				newItem.put("phone", "");
				newItem.put("fax", "");
				newItem.put("consultAuthorized", "");
				
				people.put(newItem);
			}
		}
		
		
		return people;
	}

}
