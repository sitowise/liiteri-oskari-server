package fi.nls.oskari.control.urbanplanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.urbanplanning.params.ListIdParameter;
import fi.nls.oskari.urbanplanning.params.MultiSelectListParameter;
import fi.nls.oskari.urbanplanning.params.RangeDateParameter;
import fi.nls.oskari.urbanplanning.service.UrbanPlanningApiService;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetUrbanPlans")
public class GetUrbanPlansHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(GetUrbanPlansHandler.class);
    private static final UrbanPlanningApiService apiService = new UrbanPlanningApiService();
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";

    private static final String PLAN_NAME = "planName";
    private static final String KEYWORD = "keyword";
    private static final String GENERATED_PLAN_ID = "generatedPlanId";
    private static final String MUNICIPALITY_PLAN_ID = "municipalityPlanId";
    private static final String APPROVER = "approver";
    private static final String PLAN_TYPE = "planType";
    private static final String APPROVAL_DATE_WITHIN = "approvalDateWithin";
    private static final String PROPOSAL_DATE_WITHIN = "proposalDateWithin";
    private static final String INITIAL_DATE_WITHIN = "initialDateWithin";
    private static final String FILL_DATE_WITHIN = "fillDateWithin";
    private static final String ELY = "ely";
    private static final String SUBREGION = "subRegion";
    private static final String COUNTY = "county";
    private static final String GREATER_AREA = "greaterArea";
    private static final String ADMINISTRATIVE_COURT = "administrativeCourt";
    private static final String MUNICIPALITY = "municipality";

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        List<CommonParameter> parameters = new ArrayList<CommonParameter>();
        String[] keywords = null;

        // FIXME: hack for now
        if (params.getHttpParam("limit") != null) {
            String resp = "[]";
            final HttpServletResponse response = params.getResponse();
            response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
            response.setCharacterEncoding(OSKARI_ENCODING);
            ResponseHelper.writeResponse(params, resp);
            return;
        }

        if (params.getHttpParam(PLAN_NAME) != null
                && !params.getHttpParam(PLAN_NAME).isEmpty()) {
            parameters.add(new CommonParameter(PLAN_NAME, params
                    .getHttpParam(PLAN_NAME)));
        }
        if (params.getHttpParam(KEYWORD) != null
                && !params.getHttpParam(KEYWORD).isEmpty()) {
            keywords = params.getHttpParam(KEYWORD).split(",");
        }
        if (params.getHttpParam(GENERATED_PLAN_ID) != null
                && !params.getHttpParam(GENERATED_PLAN_ID).isEmpty()) {
            parameters.add(new CommonParameter(GENERATED_PLAN_ID, params
                    .getHttpParam(GENERATED_PLAN_ID)));
        }
        if (params.getHttpParam(MUNICIPALITY_PLAN_ID) != null
                && !params.getHttpParam(MUNICIPALITY_PLAN_ID).isEmpty()) {
            parameters.add(new CommonParameter(MUNICIPALITY_PLAN_ID, params
                    .getHttpParam(MUNICIPALITY_PLAN_ID)));
        }
        if (params.getHttpParam(PLAN_TYPE) != null
                && !params.getHttpParam(PLAN_TYPE).isEmpty()) {
            parameters.add(new MultiSelectListParameter(PLAN_TYPE, params
                    .getHttpParam(PLAN_TYPE), new ArrayList<String>(Arrays
                    .asList("T", "R", "M", "tavallinen", "rantaasemakaava",
                            "maanalaistasisaltava"))));
        }
        if (params.getHttpParam(APPROVER) != null
                && !params.getHttpParam(APPROVER).isEmpty()) {
            parameters.add(new MultiSelectListParameter(APPROVER, params
                    .getHttpParam(APPROVER), new ArrayList<String>(Arrays
                    .asList("H", "L", "V", "kunnanhallitus", "kunnanvaltuusto",
                            "lautakunta"))));
        }
        if (params.getHttpParam(APPROVAL_DATE_WITHIN) != null
                && !params.getHttpParam(APPROVAL_DATE_WITHIN).isEmpty()) {
            parameters.add(new RangeDateParameter(APPROVAL_DATE_WITHIN, params
                    .getHttpParam(APPROVAL_DATE_WITHIN)));
        }
        if (params.getHttpParam(PROPOSAL_DATE_WITHIN) != null
                && !params.getHttpParam(PROPOSAL_DATE_WITHIN).isEmpty()) {
            parameters.add(new RangeDateParameter(PROPOSAL_DATE_WITHIN, params
                    .getHttpParam(PROPOSAL_DATE_WITHIN)));
        }
        if (params.getHttpParam(INITIAL_DATE_WITHIN) != null
                && !params.getHttpParam(INITIAL_DATE_WITHIN).isEmpty()) {
            parameters.add(new RangeDateParameter(INITIAL_DATE_WITHIN, params
                    .getHttpParam(INITIAL_DATE_WITHIN)));
        }
        if (params.getHttpParam(FILL_DATE_WITHIN) != null
                && !params.getHttpParam(FILL_DATE_WITHIN).isEmpty()) {
            parameters.add(new RangeDateParameter(FILL_DATE_WITHIN, params
                    .getHttpParam(FILL_DATE_WITHIN)));
        }
        if (params.getHttpParam(ELY) != null
                && !params.getHttpParam(ELY).isEmpty()) {
            parameters.add(new ListIdParameter(ELY, params.getHttpParam(ELY)));
        }
        if (params.getHttpParam(SUBREGION) != null
                && !params.getHttpParam(SUBREGION).isEmpty()) {
            parameters.add(new ListIdParameter(SUBREGION, params
                    .getHttpParam(SUBREGION)));
        }
        if (params.getHttpParam(COUNTY) != null
                && !params.getHttpParam(COUNTY).isEmpty()) {
            parameters.add(new ListIdParameter(COUNTY, params
                    .getHttpParam(COUNTY)));
        }
        if (params.getHttpParam(GREATER_AREA) != null
                && !params.getHttpParam(GREATER_AREA).isEmpty()) {
            parameters.add(new ListIdParameter(GREATER_AREA, params
                    .getHttpParam(GREATER_AREA)));
        }
        if (params.getHttpParam(ADMINISTRATIVE_COURT) != null
                && !params.getHttpParam(ADMINISTRATIVE_COURT).isEmpty()) {
            parameters.add(new ListIdParameter(ADMINISTRATIVE_COURT, params
                    .getHttpParam(ADMINISTRATIVE_COURT)));
        }
        if (params.getHttpParam(MUNICIPALITY) != null
                && !params.getHttpParam(MUNICIPALITY).isEmpty()) {
            parameters.add(new ListIdParameter(MUNICIPALITY, params
                    .getHttpParam(MUNICIPALITY)));
        }
        try {
            String main = null;
            if (keywords == null || keywords.length == 0) {
                main = apiService.getPlans(parameters);
            } else {
                //Search for each keyword and drop duplicate plans
                Map<Integer, JSONObject> plans = new HashMap<Integer, JSONObject>();
                for (String keyword : keywords) {
                    CommonParameter parameter = new CommonParameter(KEYWORD, keyword);
                    parameters.add(parameter);

                    JSONArray result = new JSONArray(apiService.getPlans(parameters));

                    for (int i = 0; i < result.length(); ++i) {
                        plans.put(result.getJSONObject(i).getInt("id"), result.getJSONObject(i));
                    }

                    parameters.remove(parameter);
                }
                JSONArray planArray = new JSONArray();
                for (Integer id : plans.keySet()) {
                    planArray.put(plans.get(id));
                }
                main = planArray.toString();
            }
            final HttpServletResponse response = params.getResponse();
            response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
            response.setCharacterEncoding(OSKARI_ENCODING);
            ResponseHelper.writeResponse(params, main);
        } catch (Exception e) {
            throw new ActionException(e.getMessage(), e);
        }

    }

}
