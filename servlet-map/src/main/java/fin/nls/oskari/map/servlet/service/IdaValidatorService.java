package fin.nls.oskari.map.servlet.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;

public class IdaValidatorService {
    static {
        PropertyUtil.loadProperties("/oskari.properties");
        PropertyUtil.loadProperties("/oskari-ext.properties");
    }

    private final static Logger log = LogFactory
            .getLogger(IdaValidatorService.class);

    private String _serviceName;
    private String _validateUrl;
    private String _authenticationCookieDomain;
    private String[] _idaCookies = new String[] { "Tunnus", "Jarjestelmat",
            "Roolit" };
    private String[] _oivaCookies = new String[] { "OivaUserName" };

    public IdaValidatorService() {
        _serviceName = PropertyUtil.get("ida.serviceName");
        _validateUrl = PropertyUtil.get("ida.validatorUrl");
        _authenticationCookieDomain = PropertyUtil.get(
                "ida.authenticationCookieDomain",
                PropertyUtil.get("oskari.map.url", "/"));

        log.info("Started with [" + _serviceName + "] [" + _validateUrl + "]");
    }

    public User tryGetUser(HttpServletRequest httpRequest) {

        CloseableHttpClient client = null;
        User user = null;

        if (this.checkIdaCookies(httpRequest)) {
            try {
                RequestConfig requestConfig = RequestConfig.custom()
                        .setExpectContinueEnabled(true).setConnectTimeout(100)
                        .build();
                client = HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig).build();
                HttpGet httpGet = new HttpGet(getValidateUrl());
                httpGet.setHeader("Cookie", httpRequest.getHeader("Cookie"));
                CloseableHttpResponse httpResponse = client.execute(httpGet);
                String responseString = getResponseAsString(httpResponse);

                user = this.getUserFromResult(responseString);

                log.info(responseString);

                log.info("IDA validation successful");
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
        } else if (this.checkOivaCookies(httpRequest)) {
            for (Cookie cookie : httpRequest.getCookies()) {
                if ("OivaUserName".equals(cookie.getName())) {
                    log.info("Found Oiva cookie");
                    try {
                        user = this.getOivaUserIdentity(URLDecoder.decode(
                                cookie.getValue(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        log.debug("OIVA cookie reading failed", e);
                    }
                    log.info("OIVA validation successful");
                }
            }
        }

        return user;
    }

    private boolean checkIdaCookies(HttpServletRequest httpRequest) {
        Vector<String> expectedCookies = new Vector<String>();
        for (String idaCookie : _idaCookies) {
            expectedCookies.add(idaCookie);
        }

        for (Cookie cookie : httpRequest.getCookies()) {
            expectedCookies.removeElement(cookie.getName());
        }

        return expectedCookies.size() == 0;
    }

    private boolean checkOivaCookies(HttpServletRequest httpRequest) {
        Vector<String> expectedCookies = new Vector<String>();
        for (String oivaCookie : _oivaCookies) {
            expectedCookies.add(oivaCookie);
        }

        for (Cookie cookie : httpRequest.getCookies()) {
            expectedCookies.removeElement(cookie.getName());
        }

        return expectedCookies.size() == 0;
    }

    public Cookie[] getLogoutCookies() {
        Cookie[] result = new Cookie[_idaCookies.length + _oivaCookies.length];

        for (int i = 0; i < _idaCookies.length; i++) {
            Cookie cookie = new Cookie(_idaCookies[i], null);
            cookie.setMaxAge(0);
            cookie.setValue(null);
            cookie.setPath("/");
            cookie.setDomain(this.getAuthenticationCookieDomain());
            result[i] = cookie;
        }

        for (int i = 0; i < _oivaCookies.length; i++) {
            Cookie cookie = new Cookie(_oivaCookies[i], null);
            cookie.setMaxAge(0);
            cookie.setValue(null);
            cookie.setPath("/");
            cookie.setDomain(this.getAuthenticationCookieDomain());
            result[i + _idaCookies.length] = cookie;
        }

        return result;
    }

    private String getServiceName() {
        return this._serviceName;
    }

    public String getValidateUrl() {
        return this._validateUrl;
    }

    public String getAuthenticationCookieDomain() {
        return this._authenticationCookieDomain;
    }

    private User getUserFromResult(String jsonData) {
        User identity = null;
        try {
            JSONObject json = new JSONObject(jsonData);
            final String userName = json.getString("Login");
            if (userName == null || userName.length() == 0) {
                return null;
            }

            final List<String> roles = new ArrayList<String>();
            JSONArray rolesArray = json.getJSONArray("Roles");
            for (int i = 0; i < rolesArray.length(); i++) {
                String itemName = rolesArray.getString(i);
                roles.add(itemName.trim());
            }

            final List<String> systems = new ArrayList<String>();
            JSONArray systemsArray = json.getJSONArray("Systems");
            for (int i = 0; i < systemsArray.length(); i++) {
                String itemName = systemsArray.getString(i);
                systems.add(itemName.trim());
            }
            if (!systems.contains(getServiceName())) {
                return null;
            }

            identity = new User();
            identity.setScreenname(userName);

            int i = 0;
            for (String role : roles) {
                identity.addRole(i, role);
                i++;
            }

        } catch (JSONException e) {
            log.error(e, "Error parsing IDA validation data");
        }

        return identity;
    }

    private User getOivaUserIdentity(String userName) {
        User identity = null;

        final List<String> roles = new ArrayList<String>();

        roles.add("liiteri_user");

        if (userName == null || userName.length() == 0) {
            return null;
        }

        identity = new User();
        identity.setScreenname(userName);
        identity.setTosAccepted(new Date()); //OIVA users have already accepted TOS when creating OIVA account

        int i = 0;
        for (String role : roles) {
            identity.addRole(i, role);
            i++;
        }

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
