package fi.nls.oskari.urbanplanning.plandata;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;

public class PlanData {
	private static final Logger log = LogFactory.getLogger(PlanData.class);
	protected static String _baseUrl;
	protected static final String NULL_STRING = "null";
	protected static final String AREA_RESERVATION = "AreaReservations";
	protected static final String BUILDING_CONSERVATION = "BuildingConservations";
	protected static final String UNDERGROUND_AREA = "UndergroundAreas";
	private static Map<String, Class> _planData = new HashMap<String, Class>();
	static {
		_baseUrl = PropertyUtil.get("urbanPlanning.baseurl");
		registerPlanData(Plan.class);
		registerPlanData(PlanList.class);
		registerPlanData(PlanSummary.class);

	}

	private static void registerPlanData(final Class req) {
		try {
			log.debug("Adding reg ", req);
			_planData.put(getInstance(req).getName(), req); // .getClass()
		} catch (Exception ex) {
			log.error(ex, "Error adding action! " + req);
		}
	}

	public static PlanData getInstance(final String planData) {
		Class c = _planData.get(planData);
		if (c != null) {
			return getInstance(c);
		}
		throw new RuntimeException("Unregistered planData requested:"
				+ planData);
	}

	private static PlanData getInstance(final Class req) {
		try {
			return (PlanData) req.newInstance();
		} catch (Exception ignored) {
		}
		throw new RuntimeException(
				"Unable to craft area instance, shouldn't happen...");
	}

	protected String getName() {
		return "";
	}

	protected String getUrlExtension() {
		return "/plans";
	}

	protected String getPlansData(String url, String postContent) throws ServiceException {

		HttpURLConnection con = null;
		try {
			con = IOHelper.getConnection(url);
			if (postContent != null) {			
				con.setRequestProperty("Content-Type", "application/json");
				IOHelper.writeToConnection(con, postContent);
			}
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

	public String getData(List<CommonParameter> params) throws ServiceException {
		if (params != null && !validateInputParameters(params)) {
			throw new ServiceException("Invalid parameters");
		}
		try {
			final String url = buildUrl(params);
			final String data = getPlansData(url, null);
			validateResult(data);
			return convertData(data);
		} catch (JSONException e) {
			throw new ServiceException("Error during JSON object parsing",e );
		} catch (UnsupportedEncodingException e) {
			throw new ServiceException("Error during URL building", e);
		} 
	}

	protected void validateResult(String data) throws ServiceException {
		return;
	}
	
	protected String convertData(String inputData) throws JSONException {

		return "";
	}

	protected String buildUrl(List<CommonParameter> params) throws UnsupportedEncodingException {
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
				wr.write(URLEncoder.encode(p.getValue(), "UTF-8"));
			}

		}
		return wr.toString();
	}

	protected boolean validateInputParameters(List<CommonParameter> params) {

		for (CommonParameter p : params) {
			if (!p.Validate())
				return false;
		}

		return true;
	}



	protected static void addAreaArrayToJSONObject(JSONArray areaData,
			JSONObject ob, String keyName) {
		if (areaData != null && areaData.length() > 0) {
			JSONHelper.putValue(ob, keyName, areaData);
		}

	}
	
	protected static void addAreaObjectToJSONObject(JSONObject areaData,
			JSONObject ob, String keyName) {
		if (areaData != null && areaData.length() > 0) {
			JSONHelper.putValue(ob, keyName, areaData);
		}

	}

}
