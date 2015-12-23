package fi.nls.oskari.urbanplanning.region;

import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
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

public class Region {
	private static final Logger log = LogFactory.getLogger(Region.class);
	private static String _baseUrl;
	private static Map<String, Class> _regions = new HashMap<String, Class>();
	static {
		// register possible actions
		registerAction(AdministrativeCourt.class);
		registerAction(Ely.class);
		registerAction(GreaterArea.class);
		registerAction(Municipality.class);
		registerAction(SubRegion.class);
		registerAction(County.class);
		_baseUrl = PropertyUtil.get("urbanPlanning.baseurl") + "/regions";
	}

	private static void registerAction(final Class req) {
		try {
			log.debug("Adding reg ", req);
			_regions.put(getInstance(req).getName(), req); // .getClass()
		} catch (Exception ex) {
			log.error(ex, "Error adding action! " + req);
		}
	}
	
	public static final Region createRegion() {
		return new Region();
	}

	public static Region getInstance(final String action) {
		Class c = _regions.get(action);
		if (c != null) {
			return getInstance(c);
		}
		throw new RuntimeException("Unregistered action requested:" + action);
	}

	private static Region getInstance(final Class req) {
		try {
			return (Region) req.newInstance();
		} catch (Exception ignored) {
		}
		throw new RuntimeException(
				"Unable to craft region instance, shouldn't happen...");
	}

	public static Map<String, Class> getRegions() {
		return _regions;
	}

	private boolean validateInputParameters(List<CommonParameter> params) {

		for (CommonParameter p : params) {
			if (!getValidInputParameters().contains(p.getName())
					|| !p.Validate())
				return false;
		}

		return true;
	}

	protected String getUrlExtension() {
		return "";
	}

	public String getName() {
		return "region";
	}

	protected String ConvertData(String inputData) throws JSONException {

		JSONArray array = JSONHelper.createJSONArray(inputData);
		JSONArray outputArray = new JSONArray();

		for (int i = 0; i < array.length(); i++) {
			JSONObject outputOb = new JSONObject();
			JSONObject ob = array.getJSONObject(i);
			JSONHelper.putValue(outputOb, "name", ob.getString("Name").trim());
			JSONHelper.putValue(outputOb, "type", ob.getString("TypeName")
					.trim());
			outputArray.put(outputOb);
		}

		return outputArray.toString();
	}

	private String buildUrl(List<CommonParameter> params) {
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

	public String getData(List<CommonParameter> params) throws ServiceException {
		if (params != null && !validateInputParameters(params)) {
			throw new ServiceException("Invalid parameter");
		}

		HttpURLConnection con = null;
		try {
			final String url = buildUrl(params);
			log.info("Connecting to " + url);
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

	public String getTitle() {
		return getName() + "s";
	}

	protected List<String> getValidInputParameters() {
		return new ArrayList<String>();
	}

}
