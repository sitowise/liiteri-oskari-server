package fi.nls.oskari.control.urbanplanning;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.AuthorizedActionHandler;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.urbanplanning.service.UrbanPlanningApiService;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetUrbanPlanPeople")
public class GetUrbanPlanPeopleHandler extends AuthorizedActionHandler {

    private static final Logger log = LogFactory
            .getLogger(GetUrbanPlanPeopleHandler.class);
    private static final UrbanPlanningApiService apiService = new UrbanPlanningApiService();
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";

    private static final String SEARCH = "search";
    private static final String PERSON_TYPE = "personType";
    private static final String ELY = "ely";
    private static final String MUNICIPALITY = "municipality";
    private static final String AUTHORIZED_ONLY = "authorizedOnly";

    @Override
    public void handleAuthorizedAction(ActionParameters params)
            throws ActionException {
        List<CommonParameter> parameters = new ArrayList<CommonParameter>();

        if (params.getHttpParam(SEARCH) != null
                && !params.getHttpParam(SEARCH).isEmpty()) {
            parameters.add(new CommonParameter(SEARCH, params
                    .getHttpParam(SEARCH)));
        }

        if (params.getHttpParam(PERSON_TYPE) != null
                && !params.getHttpParam(PERSON_TYPE).isEmpty()) {
            parameters.add(new CommonParameter(PERSON_TYPE, params
                    .getHttpParam(PERSON_TYPE)));
        }

        if (params.getHttpParam(ELY) != null
                && !params.getHttpParam(ELY).isEmpty()) {
            parameters.add(new CommonParameter(ELY, params.getHttpParam(ELY)));
        }

        if (params.getHttpParam(MUNICIPALITY) != null
                && !params.getHttpParam(MUNICIPALITY).isEmpty()) {
            parameters.add(new CommonParameter(MUNICIPALITY, params
                    .getHttpParam(MUNICIPALITY)));
        }

        if (params.getHttpParam(AUTHORIZED_ONLY) != null
                && !params.getHttpParam(AUTHORIZED_ONLY).isEmpty()) {
            parameters.add(new CommonParameter(AUTHORIZED_ONLY, params
                    .getHttpParam(AUTHORIZED_ONLY)));
        }

        try {
            String main = apiService.getPeople(parameters);
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
