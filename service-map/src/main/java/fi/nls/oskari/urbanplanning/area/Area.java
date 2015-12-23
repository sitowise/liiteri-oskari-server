package fi.nls.oskari.urbanplanning.area;

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
import fi.nls.oskari.urbanplanning.params.NumberParameter;
import fi.nls.oskari.urbanplanning.utils.PlanAction;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;

public class Area {
	private static String _baseUrl;
	private static Map<String, Class> _areas = new HashMap<String, Class>();
	private static final Logger log = LogFactory.getLogger(Area.class);
	protected static final String NULL_STRING = "null";
	protected static final String MAIN = "main";
	protected static final String SUB = "sub";
	static {
		_baseUrl = PropertyUtil.get("urbanPlanning.baseurl");
		registerArea(AreaReservation.class);
		registerArea(UndergroundArea.class);
		registerArea(BuildingConservation.class);
	}

	private static void registerArea(final Class req) {
		try {
			log.debug("Adding reg ", req);
			_areas.put(getInstance(req).getName(), req); // .getClass()
		} catch (Exception ex) {
			log.error(ex, "Error adding action! " + req);
		}
	}

	public static Area getInstance(final String area) {
		Class c = _areas.get(area);
		if (c != null) {
			return getInstance(c);
		}
		throw new RuntimeException("Unregistered area requested:" + area);
	}

	private static Area getInstance(final Class req) {
		try {
			return (Area) req.newInstance();
		} catch (Exception ignored) {
		}
		throw new RuntimeException(
				"Unable to craft area instance, shouldn't happen...");
	}

	protected String getName() {
		return "";
	}

	protected String getUrlExtension() {
		return "";
	}

	protected AreaWrapper createAreaWrapper(JSONObject ob) throws JSONException {
		return null;
	}

	public JSONArray getData(NumberParameter param, PlanAction action)
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
		if (mainObjects.size() > 0) {
			for (AreaWrapper w : childObjects) {
				AreaWrapper main = findParent(mainObjects, w.getId());
				if(main != null) { //prevent NPE
				    main.getChildren().add(w);
				}
			}

			JSONArray arr = new JSONArray();
			for (AreaWrapper w : mainObjects) {
				arr.put(w.getJSONObject());
			}

			return arr;
		}
		return null;
	}
	
	public JSONObject getDataObject(NumberParameter param, PlanAction action) throws ServiceException {
		return null;
	}

	private String buildUrl(NumberParameter param, PlanAction action,
			boolean main) {
		StringWriter wr = new StringWriter();
		wr.write(_baseUrl);
		wr.write("/");
		wr.write(action.getName());	
		if(!action.getName().equalsIgnoreCase("plansummary")) {
			wr.write("/");
			wr.write(param.getValue());
		}
		wr.write("/");
		wr.write(getUrlExtension());
		wr.write("/");
		wr.write(main ? MAIN : SUB);
		return wr.toString();
	}

	private List<AreaWrapper> buildAreaWrapperObjects(String ob)
			throws JSONException {
		List<AreaWrapper> list = new ArrayList<AreaWrapper>();
		if (ob != null && !ob.isEmpty()) {
			JSONArray array = JSONHelper.createJSONArray(ob);
			for (int i = 0; i < array.length(); i++) {
			    if(!array.getJSONObject(i).has("Description") || !array.getJSONObject(i).getString("Description").equals("FILLEDSUM")) {
			        list.add(createAreaWrapper(array.getJSONObject(i)));
			    }
			}
		}

		return list;
	}

	protected List<AreaWrapper> getWrappedData(NumberParameter param,
			PlanAction action, boolean main) throws ServiceException {

		HttpURLConnection con = null;
		try {
			final String url = buildUrl(param, action, main);
			con = IOHelper.getConnection(url);
			String postContent = null;
			if(action.getName().equalsIgnoreCase("plansummary")) {
				StringWriter wr = new StringWriter();
				wr.write("{ 'PlanIds': [");
				wr.write(param.getValue());
				wr.write("] }");
				postContent = wr.toString();
				if (postContent != null) {		
					con.setRequestProperty("Content-Type", "application/json");
					IOHelper.writeToConnection(con, postContent);
				}
			}
			final String data = IOHelper.readString(con.getInputStream());
			return buildAreaWrapperObjects(data);
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

	private AreaWrapper findParent(List<AreaWrapper> mainObjects, Long id) {
		for (AreaWrapper w : mainObjects) {
			if (id != null && w.getId() == id)
				return w;
		}

		return null;
	}
}
