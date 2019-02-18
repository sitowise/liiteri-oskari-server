package fi.nls.oskari.control.twowaystats;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import fi.mml.portti.domain.permissions.Permissions;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.twowaystats.requests.TwowayRequest;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetTwowayData")
public class GetTwowayDataHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(GetTwowayDataHandler.class);

    private final FilterParser _filterParser = FilterParser.getInstance();

    private static final String PARM_ACTION = "action";

    private static final String PARM_INDICATOR = "indicator";
    private static final String PARM_VERSION = "version";
    private static final String PARM_TYPE = "type";
    private static final String PARM_YEARS = "years"; // many
    private static final String PARM_GENDER = "gender"; // total | male | female
    private static final String PARM_GROUP = "group";
    private static final String PARM_GEOMETRYFILTER = "geometryFilter";
    private static final String PARM_FILTER = "filter";

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";

    @Override
    public void handleAction(final ActionParameters params)
            throws ActionException {

        log.info("GetTwowayData request");

        final TwowayRequest request = getRequest(params);
        User user = request.getUser();

        if (user == null) {
            throw new ActionException("No permissions");
        }
        PermissionsService permissionsService = new PermissionsServiceIbatisImpl();
        final Set<String> permissionsList = permissionsService
                .getResourcesWithGrantedPermissions("operation", user,
                        Permissions.PERMISSION_TYPE_EXECUTE);
        boolean gridDataAllowed = false;
        for(String permission : permissionsList) {
            if("statistics+grid".equals(permission)) {
                gridDataAllowed = true;
            }
        }
        if (!gridDataAllowed) {
            throw new ActionException("No permissions");
        }

        final String data = request.getData();

        final HttpServletResponse response = params.getResponse();
        response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
        response.setCharacterEncoding(OSKARI_ENCODING);
        ResponseHelper.writeResponse(params, data);
    }

    private TwowayRequest getRequest(final ActionParameters params)
            throws ActionException {

        final TwowayRequest req = TwowayRequest.getInstance(params
                .getRequiredParam(PARM_ACTION));

        req.setVersion(params.getHttpParam(PARM_VERSION, "v1"));
        req.setIndicator(params.getHttpParam(PARM_INDICATOR, ""));
        req.setType(params.getHttpParam(PARM_TYPE, ""));
        req.setYears(params.getRequest().getParameterValues(PARM_YEARS));
        req.setGender(params.getHttpParam(PARM_GENDER, ""));
        req.setGroup(params.getHttpParam(PARM_GROUP, ""));
        req.setHomeFilter(getFilter(params, "home"));
        req.setWorkFilter(getFilter(params, "work"));

        User user = params.getUser();
        if (user != null) {
            req.setUser(user);
        }

        return req;
    }

    private String getFilter(final ActionParameters params, final String type)
            throws ActionException {
        String filter = "";

        String standardFilterParam = params.getHttpParam(PARM_FILTER, "");
        String parsedStandardFilter = _filterParser.parseStandardFilter(
                standardFilterParam, type, params.getUser());
        if (!parsedStandardFilter.isEmpty()) {
            filter += parsedStandardFilter;
        }
        String geometryFilterParam = params.getHttpParam(PARM_GEOMETRYFILTER,
                "");
        String parsedGeometryFilter = _filterParser.parseGeometryFilter(
                geometryFilterParam, type, params.getUser());
        if (!parsedGeometryFilter.isEmpty()) {

            if (!parsedStandardFilter.isEmpty())
                filter += " AND ( ";

            filter += parsedGeometryFilter;

            if (!parsedStandardFilter.isEmpty())
                filter += " )";
        }
        return filter;
    }

}
