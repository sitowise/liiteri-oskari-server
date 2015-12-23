package fi.nls.oskari.ida.authentication;

import fi.nls.oskari.ida.authentication.principal.IdaUserPrincipal;
import fi.nls.oskari.log.LogFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;

import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import fi.nls.oskari.log.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by urho.tamminen on 25.2.2015.
 */
public class IdaAuthenticator implements Authenticator {

    private String _serviceName;
    private String _validateUrl;

    private static Logger log = LogFactory.getLogger(IdaAuthenticator.class);

    public IdaAuthenticator() {
    }

    public IdaAuthenticator(String serviceName, String validateUrl) {
        if (serviceName != null) {
            setServiceName(serviceName);
        }
        if (validateUrl != null) {
            setValidateUrl(validateUrl);
        }

    }

    @Override
    public void setConfiguration(AuthConfiguration authConfiguration) {
    }

    public void setServiceName(String name) {
        _serviceName = name;
    }

    public String getServiceName() {
        return _serviceName;
    }

    public void setValidateUrl(String name) {
        _validateUrl = name;
    }

    public String getValidateUrl() {
        return _validateUrl;
    }

    @Override
    public String getAuthMethod() {
        return null;
    }

    @Override
    public void prepareRequest(ServletRequest servletRequest) {
    }

    @Override
    public Authentication validateRequest(ServletRequest servletRequest,
            ServletResponse servletResponse, boolean b)
            throws ServerAuthException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;

        // let's keep the LiiteriIdaValidator results in session
        HttpSession session = request.getSession(true);

        if (session == null) {
            log.warn("getSession(true) returned null!");
            return null;
        }

        final String KEY_AUTH = this.getClass().getName();

        if (session.getAttribute(KEY_AUTH) != null) {
            UserIdentity identity = this.getUserIdentity((String) session
                    .getAttribute(KEY_AUTH));
            // log.info("validation found in session");
            return new UserAuthentication(this.toString(), identity);
        }

        CloseableHttpClient client = null;
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setExpectContinueEnabled(true).setConnectTimeout(100)
                    .build();
            // .setProxy(new HttpHost("127.0.0.1", 8888))
            client = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig).build();
            HttpGet httpGet = new HttpGet(getValidateUrl());
            httpGet.setHeader("Cookie", request.getHeader("Cookie"));
            CloseableHttpResponse httpResponse = client.execute(httpGet);
            String responseString = getResponseAsString(httpResponse);

            UserIdentity identity = this.getUserIdentity(responseString);

            if (identity == null) {
                // IDA authentication failed
                // Check if we have OivaUserName cookie
                for (Cookie cookie : request.getCookies()) {
                    if ("OivaUserName".equals(cookie.getName())) {
                        log.info("Found Oiva cookie");
                        try {
                            identity = this.getOivaUserIdentity(URLDecoder.decode(cookie
                                    .getValue(), "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            log.debug("OIVA cookie reading failed", e);
                        }

                        if (identity == null) {
                            return Authentication.UNAUTHENTICATED;
                        }

                        log.info("OIVA Validation successful");
                        return new UserAuthentication(this.toString(), identity);
                    }
                }
                
                return Authentication.UNAUTHENTICATED;
            }

            log.info(responseString);
            session.setAttribute(KEY_AUTH, responseString);

            log.info("IDA Validation successful");
            return new UserAuthentication(this.toString(), identity);
        } catch (ClientProtocolException e) {
            log.error(e, "Error sending request");
        } catch (IOException e) {
            log.error(e, "Error sending request");
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    log.error(e, "Cannot close HTTP client");
                }
            }
        }

        return Authentication.UNAUTHENTICATED;
    }

    @Override
    public boolean secureResponse(ServletRequest servletRequest,
            ServletResponse servletResponse, boolean b, Authentication.User user)
            throws ServerAuthException {
        return false;
    }

    private UserIdentity getUserIdentity(String jsonData) {
        UserIdentity identity = null;
        try {
            JSONObject json = new JSONObject(jsonData);
            final String userName = json.getString("Login");

            final List<String> roles = new ArrayList<String>();
            JSONArray rolesArray = json.getJSONArray("Roles");
            for (int i = 0; i < rolesArray.length(); i++) {
                String itemName = rolesArray.getString(i);
                roles.add(itemName);
            }

            final List<String> systems = new ArrayList<String>();
            JSONArray systemsArray = json.getJSONArray("Systems");
            for (int i = 0; i < systemsArray.length(); i++) {
                String itemName = systemsArray.getString(i);
                systems.add(itemName);
            }
            if (!systems.contains(getServiceName())) {
                return null;
            }

            if (userName == null || userName.length() == 0) {
                return null;
            }

            IdaUserPrincipal principal = new IdaUserPrincipal(userName);
            Subject subject = new Subject();
            subject.getPrincipals().add(principal);
            identity = new IdaUserIdentity(subject, principal, roles);
        } catch (JSONException e) {
            log.error(e, "Error parsing IDA validation data");
        }

        return identity;
    }

    private UserIdentity getOivaUserIdentity(String userName) {
        UserIdentity identity = null;

        final List<String> roles = new ArrayList<String>();

        roles.add("liiteri_user");

        if (userName == null || userName.length() == 0) {
            return null;
        }

        IdaUserPrincipal principal = new IdaUserPrincipal(userName);
        Subject subject = new Subject();
        subject.getPrincipals().add(principal);
        identity = new IdaUserIdentity(subject, principal, roles);

        return identity;
    }

    private String getResponseAsString(CloseableHttpResponse response)
            throws IllegalStateException, IOException {
        InputStream stream = response.getEntity().getContent();
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer);
        return writer.toString();
    }
}