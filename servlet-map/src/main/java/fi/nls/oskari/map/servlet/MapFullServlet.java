package fi.nls.oskari.map.servlet;

import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.control.*;
import fi.nls.oskari.control.view.GetAppSetupHandler;
import fi.nls.oskari.control.view.modifier.param.ParamControl;
import fi.nls.oskari.db.DBHandler;
import fi.nls.oskari.domain.GuestUser;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.view.View;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.view.ViewService;
import fi.nls.oskari.map.view.ViewServiceIbatisImpl;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Example implementation for oskari-server endpoint.
 */
public class MapFullServlet extends HttpServlet {

    static {
        // populate properties before initializing logger since logger implementation is
        // configured in properties
        PropertyUtil.loadProperties("/oskari.properties");
        PropertyUtil.loadProperties("/oskari-ext.properties");
    }

    private static final String KEY_REDIS_HOSTNAME = "redis.hostname";
    private static final String KEY_REDIS_PORT = "redis.port";
    private static final String KEY_REDIS_POOL_SIZE = "redis.pool.size";

    private final static String PROPERTY_DEVELOPMENT = "development";
    private final static String PROPERTY_VERSION = "oskari.client.version";
    private final static String PROPERTY_DOMAIN = "oskari.domain";
    private final static String KEY_PRELOADED = "preloaded";
    private final static String KEY_PATH = "path";
    private final static String KEY_VERSION = "version";

    private final static String KEY_BASE_URL = "baseUrl";
    private final static String KEY_AJAX_URL = "ajaxUrl";
    private final static String KEY_CONTROL_PARAMS = "controlParams";       

    private final ViewService viewService = new ViewServiceIbatisImpl();
    private boolean isDevelopmentMode = false;
    private String version = null;
    private String baseUrl = null;
    private final Set<String> paramHandlers = new HashSet<String>();

    private static final long serialVersionUID = 1L;

    private final static Logger log = LogFactory.getLogger(MapFullServlet.class);

    /**
     * @see HttpServlet#HttpServlet()
     */
    public MapFullServlet() {

    }

    @Override
    public void init() {

        // create initial content
        if("true".equals(PropertyUtil.getOptional("oskari.init.db"))) {
            DBHandler.createContentIfNotCreated();
        }

        // init jedis
        JedisManager.connect(ConversionHelper.getInt(PropertyUtil
                .get(KEY_REDIS_POOL_SIZE), 30), PropertyUtil
                .get(KEY_REDIS_HOSTNAME, "localhost"), ConversionHelper.getInt(PropertyUtil
                .get(KEY_REDIS_PORT), 6379));

        // Action route initialization
        ActionControl.addDefaultControls();
        // check control params to pass for getappsetup
        paramHandlers.addAll(ParamControl.getHandlerKeys());
        log.debug("Checking for params", paramHandlers);

        // check if we have development flag -> serve non-minified js
        isDevelopmentMode = "true".equals(PropertyUtil.get(PROPERTY_DEVELOPMENT));
        // Get version from init params or properties, prefer version from properties and default to init param
        version = PropertyUtil.get(PROPERTY_VERSION, getServletConfig().getInitParameter(KEY_VERSION));
        baseUrl = PropertyUtil.get(PROPERTY_DOMAIN, "/");            
    }

    /**
     * Handles ajax requests if request has parameter "action_route" or
     * renders a map view (also handles login/logout if "action" parameter
     */
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException {
        final ActionParameters params = this.getActionParameters(request,
                response);

        if (request.getParameter("action_route") != null) {
            // calling an ajax route
            handleActionRoute(params);
        } else {
            // JSP
            try {
                final String viewJSP = setupRenderParameters(params);
                if(viewJSP == null) {
                    // view not found
                    return;
                }
                log.debug("Forward to", viewJSP);
                request.getRequestDispatcher(viewJSP).forward(request, response);
            }
            catch (IOException ignored) {}
        }
    }

    /**
     * Handles action routes mapping through ActionControl.routeAction and handles errors for routes.
     * @param params
     */
    private void handleActionRoute(final ActionParameters params) {

        final String route = params.getHttpParam("action_route");
        if(!ActionControl.hasAction(route)) {
            ResponseHelper.writeError(params, "No such route registered: " + route, HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }
        try {
            ActionControl.routeAction(route, params);
            // TODO:  HANDLE THE EXCEPTION, LOG USER AGENT ETC. on exceptions
        } catch (ActionParamsException e) {
            // For cases where we dont want a stack trace
            log.error("Couldn't handle action:", route, ". Message: ", e.getMessage(), ". Parameters: ", params.getRequest().getParameterMap());
            ResponseHelper.writeError(params, e.getMessage(), HttpServletResponse.SC_NOT_IMPLEMENTED, e.getOptions());
        } catch (ActionDeniedException e) {
            // User tried to execute action he/she is not authorized to execute or session had expired
            if(params.getUser().isGuest()) {
                log.error("Session expired - Action was denied:", route, ". Parameters: ", params.getRequest().getParameterMap());
            }
            else {
                log.error("Action was denied:", route, ", Error msg:", e.getMessage(), ". User: ", params.getUser(), ". Parameters: ", params.getRequest().getParameterMap());
            }
            ResponseHelper.writeError(params, e.getMessage(), HttpServletResponse.SC_FORBIDDEN, e.getOptions());
        } catch (ActionException e) {
            // Internal failure -> print stack trace
            log.error(e, "Couldn't handle action:", route, ". Parameters: ", params.getRequest().getParameterMap());
            ResponseHelper.writeError(params, e.getMessage());
        }
    }

    /**
     * Sets up request attributes expected by JSP to link correct Oskari application based on view
     * and construct configuration elements for GetAppSetup action route.
     * @param params
     * @return path for forwarding to correct JSP (based on the view used)
     * @throws ServletException
     */
    private String setupRenderParameters(final ActionParameters params) throws ServletException {

        try {
            HttpServletRequest request = params.getRequest();
            final long viewId = ConversionHelper.getLong(params.getHttpParam("viewId"),
                    viewService.getDefaultViewId(params.getUser()));

            final View view = viewService.getViewWithConf(viewId);
            if (view == null) {
                ResponseHelper.writeError(params, "No such view (id:" + viewId + ")");
                return null;
            }
            log.debug("Serving view with id:", view.getId());
            log.debug("View:", view.getDevelopmentPath(), "/", view.getApplication(), "/", view.getPage());
            request.setAttribute("viewId", view.getId());

            // viewJSP might change if using dev override
            String viewJSP = view.getPage();
            log.debug("Using JSP:", viewJSP, "with view:", view);

            // construct control params
            final JSONObject controlParams = getControlParams(params);
            JSONHelper.putValue(controlParams, "viewId", view.getId());
            JSONHelper.putValue(controlParams, "ssl", request.getParameter("ssl"));
            request.setAttribute(KEY_CONTROL_PARAMS, controlParams.toString());

            request.setAttribute(KEY_PRELOADED, !isDevelopmentMode);
            if (isDevelopmentMode) {
                request.setAttribute(KEY_PATH, view.getDevelopmentPath() + "/" + view.getApplication());
            } else {
                request.setAttribute(KEY_PATH, "/" + version + "/" + view.getApplication());
            }
            request.setAttribute("application", view.getApplication());
            request.setAttribute("viewName", view.getName());
            request.setAttribute("language", params.getLocale().getLanguage());

            request.setAttribute(KEY_AJAX_URL,
                    PropertyUtil.get(params.getLocale(), GetAppSetupHandler.PROPERTY_AJAXURL));
            request.setAttribute("urlPrefix", "");
            request.setAttribute(KEY_BASE_URL, baseUrl);

            // in dev-mode app/page can be overridden
            if (isDevelopmentMode) {
                // check if we want to override the page & app
                final String app = params.getHttpParam("app");
                final String page = params.getHttpParam("page");
                if (page != null && app != null) {
                    log.debug("Using dev-override!!! \nUsing JSP:", page, "with application:", app);
                    request.setAttribute(KEY_PATH, app);
                    request.setAttribute("application", app);
                    viewJSP = page;
                }
            }

            // return jsp for the requested view
            return "/" + viewJSP + ".jsp";
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    /**
     * Checks all viewmodifiers registered in the system that are handling parameters
     * and constructs a controlParams JSON to be passed on to GetAppSetup action route.
     * @param params
     * @return
     */
    private JSONObject getControlParams(final ActionParameters params) {
        final JSONObject p = new JSONObject();
        for (String key : paramHandlers) {
            JSONHelper.putValue(p, key, params.getHttpParam(key, null));
        }
        return p;
    }

    /**
     * Passes requests to doGet().
     */
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException {
        doGet(request, response);
    }

    /**
     * Wraps request to ActionParameters object that is used by action routes.
     * Populates user information etc to the request.
     * @param request
     * @param response
     * @return
     */
    private ActionParameters getActionParameters(
            final HttpServletRequest request, final HttpServletResponse response) {

        final ActionParameters params = new ActionParameters();
        params.setRequest(request);
        params.setResponse(response);

        // determine user locale
        String lang = request.getParameter("lang");
        if ((lang == null) || (lang.isEmpty())) {
            params.setLocale(new Locale(PropertyUtil.getDefaultLanguage()));
        } else {
            params.setLocale(new Locale(lang));
        }

        final HttpSession session = request.getSession();
        User user = (User) session.getAttribute(User.class.getName());
        if (user == null) {
            try {
                UserService service = UserService.getInstance();
                user = service.getGuestUser();
            }
            catch (Exception ex) {
                user = new GuestUser();
            }
        }
        params.setUser(user);
        return params;
    }

    @Override
    public void destroy() {
        ActionControl.teardown();
        super.destroy();
    }
}
