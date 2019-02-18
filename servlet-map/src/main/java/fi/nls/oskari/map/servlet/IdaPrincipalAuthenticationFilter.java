package fi.nls.oskari.map.servlet;

import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.user.MybatisRoleService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdaPrincipalAuthenticationFilter implements Filter
{
	private final static Logger log = LogFactory
			.getLogger(IdaPrincipalAuthenticationFilter.class);

	private final String KEY_USER = User.class.getName();

	private MybatisRoleService roleService = null;

	private boolean addMissingUsers = true;
	private Map<String, Role> externalRolesMapping = null;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		log.debug("Initializating IdaPrincipalAuthenticationFilter filter");

		roleService = new MybatisRoleService();
		externalRolesMapping = new HashMap<String, Role>();
		try
		{			
			List<Role> roles = roleService.findAll();
			for (Role role : roles)
			{
				externalRolesMapping.put(role.getName(), role);
			}
		} catch (Exception e)
		{
			log.error(e, "Error getting UserService. Is it configured?");
			addMissingUsers = false;
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException
	{
		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		final User user = getLoggedInUser(httpRequest);
		try
		{
			setupSession(httpRequest, user);
		} catch (Exception e)
		{
			log.error(e, "Session setup failed");
		}

		chain.doFilter(request, response);
	}

	private void setupSession(HttpServletRequest httpRequest, User user)
			throws Exception
	{
		final Principal userPrincipal = httpRequest.getUserPrincipal();

		if (userPrincipal != null)
		{
			HttpSession session = httpRequest.getSession(false);
			if (session == null || user == null || user.isGuest())
			{
				log.debug("Getting user from service with principal name:",
						userPrincipal.getName());
				User loadedUser = UserService.getInstance().getUser(
						userPrincipal.getName());
				log.debug("Got user from service:", loadedUser);

				if (addMissingUsers && loadedUser == null)
				{
					log.debug("Add missing user");
					loadedUser = addUser(httpRequest);
				}

				if (loadedUser != null)
				{
					log.info("Starting session for "
							+ loadedUser.getScreenname());
					debugUser("User added to session", loadedUser);
					httpRequest.getSession(true).setAttribute(KEY_USER,
							loadedUser);
				}
			} else
			{
				debugUser("User present in session", user);
			}
		}
	}

	private void debugUser(String message, User user)
	{
		log.debug(message + " User " + user.getScreenname());
		for (Role role : user.getRoles())
		{
			log.debug("Role " + role.getName());
		}
	}

	public User addUser(final HttpServletRequest httpRequest) throws Exception
	{
		final User user = new User();
		user.setScreenname(httpRequest.getUserPrincipal().getName());
		user.setFirstname(null);
		user.setLastname(null);
		user.setUuid(UserService.getInstance().generateUuid(
				user.getScreenname()));
		for (String extRoleName : externalRolesMapping.keySet())
		{
			if (httpRequest.isUserInRole(extRoleName))
			{
				user.addRole(externalRolesMapping.get(extRoleName));
			}
		}

		log.info("Adding new user " + user.getScreenname());

		return UserService.getInstance().createUser(user);
	}

	private User getLoggedInUser(final HttpServletRequest httpRequest)
	{
		final HttpSession session = httpRequest.getSession(false);
		if (session != null)
		{
			return (User) session.getAttribute(KEY_USER);
		}
		return null;
	}

	@Override
	public void destroy()
	{
	}
}
