package fi.nls.oskari.control.urbanplanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.urbanplanning.params.IdParameter;
import fi.nls.oskari.urbanplanning.params.MultiSelectListParameter;
import fi.nls.oskari.urbanplanning.service.UrbanPlanningApiService;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetUrbanPlanMarkings")
public class GetUrbanPlanMarkingHandler extends ActionHandler {
    private static final Logger log = LogFactory
            .getLogger(GetUrbanPlanMarkingHandler.class);
    private static final UrbanPlanningApiService apiService = new UrbanPlanningApiService();
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";
    private static final String MARK_TYPE = "type";
    private static final String MUNICIPALITY_ID = "municipalityId";
    private static final String MAIN_MARK_NAME = "mainMarkName";
    private static final String MARK_NAME = "name";
    private static final String AREA_TYPE = "areaType";

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        // TODO Auto-generated method stub
        List<CommonParameter> parameters = new ArrayList<CommonParameter>();
        CommonParameter type = null;
        MultiSelectListParameter areaType = null;
        CommonParameter mainMarkName = null;
        IdParameter municipalityId = null;
        if (params.getHttpParam(MUNICIPALITY_ID) != null
                && !params.getHttpParam(MUNICIPALITY_ID).isEmpty()) {
            municipalityId = new IdParameter(MUNICIPALITY_ID,
                    params.getHttpParam(MUNICIPALITY_ID));
        }
        if (params.getHttpParam(MAIN_MARK_NAME) != null
                && !params.getHttpParam(MAIN_MARK_NAME).isEmpty()) {
            mainMarkName = new CommonParameter(MAIN_MARK_NAME,
                    params.getHttpParam(MAIN_MARK_NAME));
        }
        if (params.getHttpParam(MARK_NAME) != null
                && !params.getHttpParam(MARK_NAME).isEmpty()) {
            parameters.add(new CommonParameter(MARK_NAME, params
                    .getHttpParam(MARK_NAME)));
        }
        if (params.getHttpParam(MARK_TYPE) != null
                && !params.getHttpParam(MARK_TYPE).isEmpty()) {

            type = new CommonParameter(MARK_TYPE,
                    params.getHttpParam(MARK_TYPE));
        }
        if (params.getHttpParam(AREA_TYPE) != null
                && !params.getHttpParam(AREA_TYPE).isEmpty()) {

            // areaType = new CommonParameter(AREA_TYPE,
            // params.getHttpParam(AREA_TYPE));

            areaType = new MultiSelectListParameter(AREA_TYPE,
                    params.getHttpParam(AREA_TYPE), new ArrayList<String>(
                            Arrays.asList("undergroundAreas",
                                    "areaReservations")));
        }

        try {
            String main = apiService.getMarkings(parameters, type,
                    municipalityId, areaType, mainMarkName);
            final HttpServletResponse response = params.getResponse();
            response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
            response.setCharacterEncoding(OSKARI_ENCODING);
            ResponseHelper.writeResponse(params, main);
        } catch (Exception e) {

            throw new ActionException(
                    "Error during getting urban planning data", e);
        }
    }

}
