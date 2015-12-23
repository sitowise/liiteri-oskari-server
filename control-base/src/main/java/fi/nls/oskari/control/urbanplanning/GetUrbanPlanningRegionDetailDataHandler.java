package fi.nls.oskari.control.urbanplanning;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.urbanplanning.params.*;
import fi.nls.oskari.urbanplanning.params.IdParameter;
import fi.nls.oskari.urbanplanning.service.UrbanPlanningApiService;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetUrbanPlanningRegionDetailData")
public class GetUrbanPlanningRegionDetailDataHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(GetUrbanPlanningRegionDetailDataHandler.class);
    private static final String REGION_TYPE = "regionType";

    private static final String ELY = "ely";
    private static final String GREATER_AREA = "greaterArea";
    private static final String ADMINISTRATIVE_COURT = "administrativeCourt";
    private static final String COUNTY = "county";
    private static final String SUB_REGION = "subRegion";
    private static final String MUNICIPALITY = "municipality";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";

    private static final UrbanPlanningApiService apiService = new UrbanPlanningApiService();

    @Override
    public void handleAction(ActionParameters params) throws ActionException {

        if (params.getHttpParam("limit") != null) {
            String resp = "[]";
            final HttpServletResponse response = params.getResponse();
            response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
            response.setCharacterEncoding(OSKARI_ENCODING);
            ResponseHelper.writeResponse(params, resp);
            return;
        }

        List<CommonParameter> parameters = new ArrayList<CommonParameter>();
        String region = params.getHttpParam(REGION_TYPE);
        if (region.equals(COUNTY)) {
            if (params.getHttpParam(GREATER_AREA) != null
                    && !params.getHttpParam(GREATER_AREA).isEmpty()) {
                parameters.add(new ListIdParameter(GREATER_AREA, params
                        .getHttpParam(GREATER_AREA)));
            }
            if (params.getHttpParam(ADMINISTRATIVE_COURT) != null
                    && !params.getHttpParam(ADMINISTRATIVE_COURT).isEmpty()) {
                parameters.add(new IdParameter(ADMINISTRATIVE_COURT, params
                        .getHttpParam(ADMINISTRATIVE_COURT)));
            }
            if (params.getHttpParam(ELY) != null
                    && !params.getHttpParam(ELY).isEmpty()) {
                parameters.add(new CommonParameter(ELY, params
                        .getHttpParam(ELY)));
            }

        } else if (region.equals(SUB_REGION)) {

            if (params.getHttpParam(COUNTY) != null
                    && !params.getHttpParam(COUNTY).isEmpty()) {
                parameters.add(new IdParameter(COUNTY, params
                        .getHttpParam(COUNTY)));
            }
        } else if (region.equals(MUNICIPALITY)) {
            if (params.getHttpParam(GREATER_AREA) != null
                    && !params.getHttpParam(GREATER_AREA).isEmpty()) {
                parameters.add(new IdParameter(GREATER_AREA, params
                        .getHttpParam(GREATER_AREA)));
            }
            if (params.getHttpParam(ADMINISTRATIVE_COURT) != null
                    && !params.getHttpParam(ADMINISTRATIVE_COURT).isEmpty()) {
                parameters.add(new IdParameter(ADMINISTRATIVE_COURT, params
                        .getHttpParam(ADMINISTRATIVE_COURT)));
            }
            if (params.getHttpParam(ELY) != null
                    && !params.getHttpParam(ELY).isEmpty()) {
                parameters.add(new IdParameter(ELY, params.getHttpParam(ELY)));
            }

            if (params.getHttpParam(COUNTY) != null
                    && !params.getHttpParam(COUNTY).isEmpty()) {
                parameters.add(new IdParameter(COUNTY, params
                        .getHttpParam(COUNTY)));
            }
            if (params.getHttpParam(SUB_REGION) != null
                    && !params.getHttpParam(SUB_REGION).isEmpty()) {
                parameters.add(new IdParameter(SUB_REGION, params
                        .getHttpParam(SUB_REGION)));
            }
        }

        try {
            String main = apiService.getUrbanPlanningRegionDetailData(region,
                    parameters.size() > 0 ? parameters : null);
            final HttpServletResponse response = params.getResponse();
            response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
            response.setCharacterEncoding(OSKARI_ENCODING);
            ResponseHelper.writeResponse(params, main);
        } catch (Exception e) {
            log.error(e);
            throw new ActionException(
                    "Error during getting urban planning detail data");
        }
    }
}
