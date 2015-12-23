package fi.nls.oskari.control.szopa;

import javax.servlet.http.HttpServletResponse;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.szopa.requests.SzopaRequest;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
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

        final SzopaRequest request = getRequest(params);
        final String data = request.getData();

        final HttpServletResponse response = params.getResponse();
        response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
        response.setCharacterEncoding(OSKARI_ENCODING);
        ResponseHelper.writeResponse(params, data);
    }

    private SzopaRequest getRequest(final ActionParameters params)
            throws ActionException {

        final SzopaRequest req = SzopaRequest.getInstance(params
                .getRequiredParam(PARM_ACTION));

        req.setIndicator(params.getHttpParam(PARM_INDICATOR, ""));
        req.setFormat(params.getHttpParam(PARM_FORMAT, ""));
        req.setYears(params.getRequest().getParameterValues(PARM_YEARS));
        req.setGroup(params.getHttpParam(PARM_GROUP, ""));
        req.setFilter(getFilter(params, params.getUser()));
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

    private String getFilter(final ActionParameters params, User user)
            throws ActionException {
        String filter = "";

        String standardFilterParam = params.getHttpParam(PARM_FILTER, "");
        String parsedStandardFilter = _filterParser.parseStandardFilter(
                standardFilterParam, user);
        if (!parsedStandardFilter.isEmpty()) {
            filter += parsedStandardFilter;
        }
        String geometryFilterParam = params.getHttpParam(PARM_GEOMETRYFILTER,
                "");
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
