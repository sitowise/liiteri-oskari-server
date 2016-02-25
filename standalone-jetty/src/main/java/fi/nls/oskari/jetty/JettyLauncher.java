package fi.nls.oskari.jetty;

import fi.nls.oskari.map.servlet.IdaPrincipalAuthenticationFilter;
import fi.nls.oskari.map.servlet.JaasAuthenticationFilter;
import fi.nls.oskari.map.servlet.MapFullServlet;
import fi.nls.oskari.map.servlet.OskariContextInitializer;
import fi.nls.oskari.map.servlet.OskariRequestFilter;
import fi.nls.oskari.map.servlet.PrincipalAuthenticationFilter;
import fi.nls.oskari.map.servlet.MapFullServletContextListener;
import fi.nls.oskari.util.PropertyUtil;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.naming.NamingException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.servlet.DispatcherType;

import java.util.EnumSet;
import java.util.HashMap;

public class JettyLauncher {
    public static Server launch(int serverPort,
                                String oskariClientVersion,
                                String jndiDriverClassName,
                                String jndiDbUrl,
                                String jndiDbUsername,
                                String jndiDbPassword,
                                String jndiDbPoolName,
                                String authenticationType) throws Exception {
        Server server = new Server(serverPort);

        WebAppContext webapp = createServletContext(oskariClientVersion, jndiDriverClassName, jndiDbUrl, jndiDbUsername, jndiDbPassword, jndiDbPoolName, authenticationType);
        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(webapp);

        server.setHandler(handlerList);
        return server;
    }

    public static WebAppContext createServletContext(String oskariClientVersion,
                                                      String jndiDriverClassName,
                                                      String jndiDbUrl,
                                                      String jndiDbUsername,
                                                      String jndiDbPassword,
                                                      String jndiDbPoolName,
                                                      String authenticationType) throws Exception {
        WebAppContext servletContext = new WebAppContext();
        servletContext.setConfigurationClasses(new String[]{"org.eclipse.jetty.plus.webapp.EnvConfiguration", "org.eclipse.jetty.plus.webapp.PlusConfiguration"});
        //servletContext.setResourceBase("src/main/webapp");
        servletContext.setContextPath("/");
        servletContext.addEventListener(new OskariContextInitializer());

        // setup JSP/static resources
        servletContext.setBaseResource(createResourceCollection());
        // OskariRequestFilter needs to be run before map-servlet.
        // TODO: find a way to map filter to servlet instead of urls like this:
        // <filter-mapping><filter-name>oskariRequestFilter</filter-name><servlet-name>mapFullServlet</servlet-name></filter-mapping>
        servletContext.addFilter(OskariRequestFilter.class, "/", EnumSet.noneOf(DispatcherType.class));
        servletContext.addFilter(OskariRequestFilter.class, "/j_security_check", EnumSet.noneOf(DispatcherType.class));
        servletContext.addFilter(OskariRequestFilter.class, PropertyUtil.get("auth.logout.url", "/logout"), EnumSet.noneOf(DispatcherType.class));

        servletContext.addServlet(createFrontEndServlet(), "/Oskari/*");
        servletContext.addServlet(JspServlet.class, "*.jsp");
        servletContext.addServlet(DebugServlet.class, "/debug");

        
        // ServletContextListener
        servletContext.addEventListener(new MapFullServletContextListener());

        // TODO: replace these with actual impls
        servletContext.addServlet(NotImplementedYetServlet.class, "/transport/*");
        servletContext.addServlet(NotImplementedYetServlet.class, "/geoserver/*");
        // map servlet
        servletContext.addServlet(createMapServlet(oskariClientVersion), "/");

        setupDatabaseConnectionInContext(servletContext, jndiDriverClassName, jndiDbUrl, jndiDbUsername, jndiDbPassword, jndiDbPoolName);
        setupDatabaseConnectionInContext(servletContext, jndiDriverClassName, jndiDbUrl, jndiDbUsername, jndiDbPassword, "jdbc/omat_paikatPool");
        setupDatabaseConnectionInContext(servletContext, jndiDriverClassName, jndiDbUrl, jndiDbUsername, jndiDbPassword, "jdbc/analysisPool");
        setupDatabaseConnectionInContext(servletContext, jndiDriverClassName, jndiDbUrl, jndiDbUsername, jndiDbPassword, "jdbc/userlayerPool");

        setupJaasInContext(servletContext, jndiDbPoolName, authenticationType);

        return servletContext;
    }

    private static void setupJaasInContext(WebAppContext servletContext, String jndiDbPoolName, String authenticationType) {
    	if ("ida".equals(authenticationType)) {
    		Configuration.setConfiguration(new IDAJNDILoginConfiguration());
    		servletContext.setSecurityHandler(createIdaJaasSecurityHandler());
            servletContext.addFilter(IdaPrincipalAuthenticationFilter.class, "/", EnumSet.noneOf(DispatcherType.class));
    	}
    	else {
    		Configuration.setConfiguration(new JNDILoginConfiguration(jndiDbPoolName));
    		servletContext.setSecurityHandler(createJaasSecurityHandler());
            servletContext.addFilter(JaasAuthenticationFilter.class, "/", EnumSet.noneOf(DispatcherType.class));
    	}
        servletContext.addFilter(PrincipalAuthenticationFilter.class, PropertyUtil.get("auth.logout.url", "/logout"), EnumSet.noneOf(DispatcherType.class));
    }

    private static Resource createResourceCollection() throws Exception {
        final String[] paths = {
                PropertyUtil.get("oskari.server.jsp.location", "../webapp-map/src/main/webapp"),
                PropertyUtil.get("oskari.client.location", "../..")
        };
        try {
            return new ResourceCollection(paths);
        }
        catch (Exception ex) {
            // TODO: maybe a bit more generic paths handling
            System.out.println("Problem with resource paths: " + paths[0] + " and " + paths[1]);
            throw ex;
        }
    }

    private static ServletHolder createFrontEndServlet() {
        ServletHolder holder = new ServletHolder(DefaultServlet.class);
        holder.setInitParameter("useFileMappedBuffer", "false");
        return holder;
    }

    private static ServletHolder createMapServlet(String oskariClientVersion) {
        ServletHolder holder = new ServletHolder(MapFullServlet.class);
        holder.setInitParameter("version", oskariClientVersion);
        return holder;
    }

    private static void setupDatabaseConnectionInContext(WebAppContext servletContext,
                                                         String jndiDriverClassName,
                                                         String jndiDbUrl,
                                                         String jndiDbUsername,
                                                         String jndiDbPassword,
                                                         String jndiDbPoolName) throws NamingException {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jndiDriverClassName);
        dataSource.setUrl(jndiDbUrl);
        dataSource.setUsername(jndiDbUsername);
        dataSource.setPassword(jndiDbPassword);
        new EnvEntry(servletContext, jndiDbPoolName, dataSource, true);
    }

    private static SecurityHandler createJaasSecurityHandler() {
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        JAASLoginService loginService = new JAASLoginService();
        loginService.setName("OskariRealm");
        loginService.setLoginModuleName("oskariLoginModule");
        securityHandler.setLoginService(loginService);
        // the last boolean param needs to be false on FormAuthenticator or
        // we'll lose everything that's been put to request on failed login (login form url/error msg on failed login)
        // TODO: mapping OskariRequestFilter to mapServlet instead of path should fix this
        securityHandler.setAuthenticator(new FormAuthenticator("/", "/?loginState=failed", false));
        securityHandler.setRealmName("OskariRealm");
        return securityHandler;
    }
    
    private static SecurityHandler createIdaJaasSecurityHandler() {
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        JAASLoginService loginService = new JAASLoginService();
        loginService.setName("OskariRealm");
        loginService.setLoginModuleName("oskariLoginModule");
        loginService.setRoleClassNames(new String[] { "fi.nls.oskari.ida.authentication.principal.IdaRolePrincipal", "org.eclipse.jetty.jaas.JAASRole"});
        securityHandler.setLoginService(loginService);
        securityHandler.setAuthenticator(new FormAuthenticator("/", "/?loginState=failed", true));
        securityHandler.setRealmName("OskariRealm");
        return securityHandler;
    }

    private static class JNDILoginConfiguration extends Configuration {
        private final String jndiDbPoolName;

        public JNDILoginConfiguration(String jndiDbPoolName) {
            this.jndiDbPoolName = jndiDbPoolName;
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            HashMap<String, String> loginModuleOptions = new HashMap<String, String>();
            loginModuleOptions.put("debug", "true");
            loginModuleOptions.put("dbJNDIName", jndiDbPoolName);
            loginModuleOptions.put("userTable", "oskari_jaas_users");
            loginModuleOptions.put("userField", "login");
            loginModuleOptions.put("credentialField", "password");
            loginModuleOptions.put("userRoleTable", "oskari_jaas_roles");
            loginModuleOptions.put("userRoleUserField", "login");
            loginModuleOptions.put("userRoleRoleField", "role");

            return new AppConfigurationEntry[] {
                    new AppConfigurationEntry("org.eclipse.jetty.jaas.spi.DataSourceLoginModule",
                            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, loginModuleOptions)                                                      
            };
        }
    }
    
    private static class IDAJNDILoginConfiguration extends Configuration {

        public IDAJNDILoginConfiguration() {
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {                    
                    HashMap<String, String> loginModuleOptions = new HashMap<String, String>();
                    loginModuleOptions.put("debug", "true");
                    loginModuleOptions.put("loginUrl", "http://map11.sito.fi/Idatest/authenticate");                                      

                    return new AppConfigurationEntry[] {
                            new AppConfigurationEntry("fi.nls.oskari.ida.authentication.IdaLoggingModule",
                                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, loginModuleOptions)                    
                    
            };
        }
    }
}
