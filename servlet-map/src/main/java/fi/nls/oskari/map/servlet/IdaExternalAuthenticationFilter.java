package fi.nls.oskari.map.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.user.IbatisRoleService;
import fi.nls.oskari.util.PropertyUtil;
import fin.nls.oskari.map.servlet.service.IdaValidatorService;

public class IdaExternalAuthenticationFilter implements Filter {
    private final static Logger log = LogFactory
            .getLogger(IdaExternalAuthenticationFilter.class);

    private final String KEY_USER = User.class.getName();

    private IbatisRoleService roleService = null;
    private IdaValidatorService validatorService = null;

    private boolean addMissingUsers = true;
    private Map<String, Role> externalRolesMapping = null;

    private String loggedOutPage;
    private boolean idaAuthentication = false;

    @Override
    public void destroy() {
        externalRolesMapping.clear();
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        log.debug("Initializating IdaExternalAuthenticationFilter filter");

        loggedOutPage = PropertyUtil.get("auth.loggedout.page",
                PropertyUtil.get("oskari.map.url", "/"));

        String authenticationType = PropertyUtil.get("oskari.authentication",
                "default");
        if ("ida".equals(authenticationType))
            idaAuthentication = true;

        validatorService = new IdaValidatorService();
        roleService = new IbatisRoleService();
        externalRolesMapping = new HashMap<String, Role>();
        try {
            List<Role> roles = roleService.findAll();
            for (Role role : roles) {
                externalRolesMapping.put(role.getName(), role);
            }
        } catch (Exception e) {
            log.error(e, "Error getting UserService. Is it configured?");
            addMissingUsers = false;
        }

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        final User user = getLoggedUser(httpRequest);

        if ("logout".equals(httpRequest.getParameter("action"))) {
            doHandleLogout(httpRequest, httpResponse, user);
            return;
        }

        try {
            setupSession(httpRequest, user);
        } catch (Exception e) {
            log.error(e, "Session setup failed");
        }

        if ("failed".equals(httpRequest.getParameter("loginState"))) {
            httpRequest.setAttribute("loginState", "failed");
        }

        chain.doFilter(request, response);
    }

    private void doHandleLogout(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, User user) throws IOException {
        log.info("Logout action "
                + (user != null ? user.getScreenname() : "[NULL USER]"));
        final HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        Cookie[] logoutCookies = validatorService.getLogoutCookies();
        for (Cookie cookie : logoutCookies) {
            httpResponse.addCookie(cookie);
        }

        httpResponse.sendRedirect(loggedOutPage);
    }

    private void setupSession(HttpServletRequest httpRequest, User user)
            throws Exception {
        HttpSession session = httpRequest.getSession(false);
        if (session != null && user != null && !user.isGuest()) {
            log.info("User is logged " + user.getScreenname());
            return;
        }

        user = validatorService.tryGetUser(httpRequest);
        if (user == null) {
            log.info("User is not logged");
        } else {
            log.info("User was in cookies " + user.getScreenname());
            User loadedUser = UserService.getInstance().getUser(
                    user.getScreenname());
            if (addMissingUsers && loadedUser == null) {
                log.debug("Add missing user");
                loadedUser = addUser(user);
            }
            if (loadedUser != null) {
                log.info("Starting session for " + loadedUser.getScreenname());
                httpRequest.getSession(true).setAttribute(KEY_USER, loadedUser);
            }
        }
    }

    private User getLoggedUser(final HttpServletRequest httpRequest) {
        final HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            return (User) session.getAttribute(KEY_USER);
        }
        return null;
    }

    private User addUser(final User userStub) throws Exception {
        final User user = new User();
        user.setScreenname(userStub.getScreenname());
        user.setEmail(userStub.getScreenname());
        user.setFirstname(null);
        user.setLastname(null);
        user.setUuid(UserService.getInstance().generateUuid(
                user.getScreenname()));
        user.setTosAccepted(userStub.getTosAccepted());

        for (Role role : userStub.getRoles()) {
            if (externalRolesMapping.containsKey(role.getName())) {
                user.addRole(externalRolesMapping.get(role.getName()));
            } else {
                log.warn("Role [" + role.getName()
                        + "] is not present in Liiteri and won't be added");
            }
        }

        log.info("Adding new user to database " + user.getScreenname());

        return UserService.getInstance().createUser(user);
    }
}