package fi.nls.oskari.control.urbanplanning;

import javax.servlet.http.HttpServletResponse;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.urbanplanning.params.ListIdParameter;
import fi.nls.oskari.urbanplanning.service.UrbanPlanningApiService;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetUrbanPlanSummary")
public class GetUrbanPlanSummaryHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(GetUrbanPlanSummaryHandler.class);
    private static final UrbanPlanningApiService apiService = new UrbanPlanningApiService();
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";

    private static final String ID = "ids";

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        // TODO Auto-generated method stub
        ListIdParameter idParam = null;
        if (params.getHttpParam(ID) != null
                && !params.getHttpParam(ID).isEmpty()) {
            idParam = new ListIdParameter(ID, params.getHttpParam(ID));
        }
        try {
            String main = apiService.getPlanSummary(idParam);
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
