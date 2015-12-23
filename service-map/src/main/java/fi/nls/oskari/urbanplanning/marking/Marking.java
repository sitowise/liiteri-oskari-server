package fi.nls.oskari.urbanplanning.marking;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.params.*;
import fi.nls.oskari.urbanplanning.utils.JSONCopyHelper;
import fi.nls.oskari.urbanplanning.utils.UrbanPlanningMarkingType;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;

public class Marking {

	private static String _baseUrl;
	private static Map<String, Marking> _markings = new HashMap<String, Marking>();
	private static final Logger log = LogFactory.getLogger(Marking.class);

	static {
		_baseUrl = PropertyUtil.get("urbanPlanning.baseurl") + "/markings";
				
		AreaReservation areaMarking = new AreaReservation();
		UndergroundArea undergroundMarking = new UndergroundArea();
		CombinedMarking combinedMarking = new CombinedMarking();
		combinedMarking.addMarking(undergroundMarking);
		combinedMarking.addMarking(areaMarking);
		
		_markings.put(areaMarking.getName(), areaMarking);
		_markings.put(undergroundMarking.getName(), undergroundMarking);
		_markings.put(combinedMarking.getName(), combinedMarking);		
	}

	public static Marking getInstance(final String marking) {
		// combinedMarking.getName() excepts sorted list,
		// let's sort our parameters here
		List<String> l = Arrays.asList(marking.split(","));
		Collections.sort(l);
		String smarking = StringUtils.join(l.toArray(), ",");

		return _markings.get(smarking);
	}

	protected String getName() {
		return "";
	}

	protected String getUrlExtension() {
		return "";
	}

	protected String ConvertData(String inputData) throws JSONException {
		JSONArray array = JSONHelper.createJSONArray(inputData);
		JSONArray outPutArray = new JSONArray();
		for (int i = 0; i < array.length(); i++) {
			JSONObject source = array.getJSONObject(i);
			JSONObject destination = new JSONObject();
			
			JSONCopyHelper.CopyLongValue(source, "MainMarkId", destination, "mainMarkId");
			JSONCopyHelper.CopyLongValue(source, "MunicipalityId", destination, "municipalityId");
			JSONCopyHelper.CopyValue(source, "MunicipalityName", destination, "municipalityName");
			JSONCopyHelper.CopyValue(source, "MainMarkName", destination, "mainMarkName");
			JSONCopyHelper.CopyValue(source, "Name", destination, "name");
			JSONCopyHelper.CopyValue(source, "Description", destination, "description");
			JSONCopyHelper.CopyValue(source, "OrderNumber", destination, "order");
			
			outPutArray.put(destination);
		}

		return outPutArray.toString();

	}

	private String buildUrl(List<CommonParameter> params, CommonParameter type,
			IdParameter municipalityId, CommonParameter mainMarkName) throws UnsupportedEncodingException {
		StringWriter wr = new StringWriter();
		wr.write(_baseUrl);
		wr.write("/");
		wr.write(getUrlExtension());
		wr.write("/");
		wr.write(type.getValue());
		if (type.getValue().equals(
				UrbanPlanningMarkingType.MUNICIPALITY.getName())) {

			if (municipalityId != null) {
				wr.write("/");
				wr.write(municipalityId.getValue());
			}
		}
		if (mainMarkName != null) {
			if (params == null)
				params = new ArrayList<CommonParameter>();
			params.add(mainMarkName);
		}
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

	public String getData(List<CommonParameter> params, CommonParameter type,
			IdParameter municipalityId, CommonParameter mainMarkName)
			throws ServiceException {
		if (params != null
				&& !validateInputParameters(params, type, municipalityId,
						mainMarkName)) {
			throw new ServiceException("Invalid parameter");
		}

		HttpURLConnection con = null;
		try {
			final String url = buildUrl(params, type, municipalityId,mainMarkName);
			con = IOHelper.getConnection(url);
			final String data = IOHelper.readString(con.getInputStream());
			final String converted = ConvertData(data);
			return converted;
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

	protected boolean validateInputParameters(List<CommonParameter> params,
			CommonParameter type, IdParameter municipalityId,
			CommonParameter mainMarkName) {
		if (!type.Validate())
			return false;
		if (!type.getValue().equals(
				UrbanPlanningMarkingType.MUNICIPALITY.getName())
				&& !type.getValue().equals(
						UrbanPlanningMarkingType.STANDARD.getName()))
			return false;
		if (type.getValue().equals(
				UrbanPlanningMarkingType.MUNICIPALITY.getName())
				&& municipalityId != null && !municipalityId.Validate())
			return false;
		for (CommonParameter p : params) {
			if (!p.Validate())
				return false;
		}

		return true;
	}
}
