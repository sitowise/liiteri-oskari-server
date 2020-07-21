package fi.nls.oskari.control.szopa;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.szopa.requests.SzopaRequest;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetSzopaData")
public class GetSzopaDataHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(GetSzopaDataHandler.class);

    private final FilterParser _filterParser = FilterParser.getInstance();

    private static final String PARM_ACTION = "action";

    private static final String PARM_VERSION = "version";
    private static final String PARM_INDICATOR = "indicator";
    private static final String PARM_FORMAT = "format";
    private static final String PARM_YEARS = "years"; // many
    private static final String PARM_GENDERS = "genders"; // total | male |
                                                          // female
    private static final String PARM_GROUP = "group";
    private static final String PARM_GEOMETRYFILTER = "geometryFilter";
    private static final String PARM_FILTER = "filter";
    private static final String PARM_AREA_YEAR = "areaYear";

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";

    @Override
    public void handleAction(final ActionParameters params)
            throws ActionException {

        log.info("GetSzopaData request");

        SzopaRequest request = null;
        String data = null;
        
        String geometryFilterParam = params.getHttpParam(PARM_GEOMETRYFILTER, "", false);
        
        if (!geometryFilterParam.isEmpty()) {
        	JSONArray array = JSONHelper.createJSONArray(geometryFilterParam.toString());
        	
        	JSONArray resultArray = new JSONArray();
        	
        	for (int i = 0; i < array.length(); i++) {
        		JSONObject o;
        		String geom = null;
        		String id = null;
				try {
					o = array.getJSONObject(i);
					id = o.getString("id");
					geom = o.getString("geom");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		
        		SzopaRequest requestX = getRequest(params, geom);
                String dataX = requestX.getData();
                
                //concatenate the responses
                JSONArray array2  = JSONHelper.createJSONArray(dataX);
                JSONObject resultOb = null;
                try {
					resultOb = array2.getJSONObject(0);
					resultOb.put("title", id);
					
					resultArray.put(resultOb);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	
        	data = resultArray.toString();
        	
        } else {
        	request = getRequest(params, "");
            data = request.getData();
        }

        final HttpServletResponse response = params.getResponse();
        response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
        response.setCharacterEncoding(OSKARI_ENCODING);
        ResponseHelper.writeResponse(params, data);
    }

    private SzopaRequest getRequest(final ActionParameters params, String geometry)
            throws ActionException {

        final SzopaRequest req = SzopaRequest.getInstance(params
                .getRequiredParam(PARM_ACTION));

        req.setIndicator(params.getHttpParam(PARM_INDICATOR, ""));
        req.setFormat(params.getHttpParam(PARM_FORMAT, ""));
        req.setYears(params.getRequest().getParameterValues(PARM_YEARS));
        req.setGroup(params.getHttpParam(PARM_GROUP, ""));
        req.setFilter(getFilter(params, geometry, params.getUser()));
        if (params.getRequiredParam(PARM_ACTION).equalsIgnoreCase("data"))
            req.setStandardFilterParam(params.getHttpParam(PARM_FILTER, ""));
        req.setAreaYear(params.getHttpParam(PARM_AREA_YEAR, ""));

        req.setVersion("v1");

        User user = params.getUser();
        if (user != null) {
            req.setUser(user);
        }

        return req;
    }

    private String getFilter(final ActionParameters params, String geometry, User user)
            throws ActionException {
        String filter = "";

        String standardFilterParam = params.getHttpParam(PARM_FILTER, "");
        String parsedStandardFilter = _filterParser.parseStandardFilter(
                standardFilterParam, user);
        if (!parsedStandardFilter.isEmpty()) {
            filter += parsedStandardFilter;
        }
		
        String geometryFilterParam = geometry;
		
        String parsedGeometryFilter = _filterParser.parseGeometryFilter(
                geometryFilterParam, user);
        if (!geometryFilterParam.isEmpty()) {

            if (!parsedStandardFilter.isEmpty())
                filter += " AND ( ";

            filter += parsedGeometryFilter;

            if (!parsedStandardFilter.isEmpty())
                filter += " )";
        }
        return filter;
    }

}
