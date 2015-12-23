package fi.nls.oskari.control.twowaystats.requests;

import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.PropertyUtil;

public class TwowayRequest {

    private static final Logger log = LogFactory.getLogger(TwowayRequest.class);

    protected static String _baseUrl;

    private String _indicator = "";
    private String _type = "";
    private String[] _years = new String[0];
    private String _version = "";
    private String _group = "";
    private String _gender = "";
    private String _workFilter = "";
    private String _homeFilter = "";
    private User _user;
    private String _requestBody = "";
    private boolean _useCache = false;

    private static Map<String, Class> _requests = new HashMap<String, Class>();
    static {
        // register possible actions
        registerAction(Indicators.class);
        registerAction(IndicatorMetadata.class);
        registerAction(IndicatorData.class);

        _baseUrl = PropertyUtil.get("szopa.baseurl");
    }

    public boolean getUseCache() {
        return _useCache;
    }

    public void setUseCache(boolean useCache) {
        this._useCache = useCache;
    }

    public String getCacheKey() {
        return this.getUrl() + "|" + this.getRequestBody();
    }

    public String getName() {
        return null;
    }

    public String getRequestSpecificParams() {
        return "";
    }

    private static void registerAction(final Class req) {
        try {
            log.debug("Adding req ", req);
            _requests.put(getInstance(req).getName(), req); // .getClass()
        } catch (Exception ex) {
            log.error(ex, "Error adding action! " + req);
        }
    }

    public static TwowayRequest getInstance(final String action) {
        Class c = _requests.get(action);
        if (c != null) {
            return getInstance(c);
        }
        throw new RuntimeException("Unregistered action requested:" + action);
    }

    private static TwowayRequest getInstance(final Class req) {
        try {
            return (TwowayRequest) req.newInstance();
        } catch (Exception ignored) {
        }
        throw new RuntimeException(
                "Unable to craft request instance, shouldn't happen...");
    }

    public String getVersion() {
        return _version;
    }

    public void setVersion(final String version) {
        this._version = version;
    }

    public String[] getYears() {
        return _years;
    }

    public void setYears(String[] years) {
        if (years != null && years.length > 0) {
            _years = years;
        }
    }

    public String getIndicator() {
        return _indicator;
    }

    public void setIndicator(String indicator) {
        this._indicator = indicator;
    }

    public String getType() {
        return _type;
    }

    public void setType(String type) {
        this._type = type;
    }

    public String getGroup() {
        return _group;
    }

    public void setGroup(String group) {
        this._group = group;
    }

    public String getGender() {
        return _gender;
    }

    public void setGender(String gender) {
        this._gender = gender;
    }

    public String getWorkFilter() {
        return _workFilter;
    }

    public void setWorkFilter(String workFilter) {
        this._workFilter = workFilter;
    }

    public String getHomeFilter() {
        return _homeFilter;
    }

    public void setHomeFilter(String homeFilter) {
        this._homeFilter = homeFilter;
    }

    public String getRequestBody() {
        return _requestBody;
    }

    public void setRequestBody(String requestBody) {
        this._requestBody = requestBody;
    }

    public String getUrl() {
        StringWriter writer = new StringWriter();
        writer.write(_baseUrl);
        writer.write("/");
        writer.write(getVersion());
        writer.write(getRequestSpecificParams());
        return writer.toString();
    }

    public String getData() throws ActionException {
        final String cachedData = TryGetCacheDataIfDesired();
        if (cachedData != null)
            return cachedData;

        HttpURLConnection con = null;
        try {
            final String url = getUrl();
            log.info("Request " + url);
            final String requestBody = getRequestBody();
            con = IOHelper.getConnection(url);
            if (requestBody.length() > 0) {
                con.setRequestProperty("Content-Type", "application/json");
                IOHelper.writeToConnection(con, requestBody);
            }
            final String data = IOHelper.readString(con.getInputStream());
            final String converted = ConvertData(data);
            CacheDataIfDesired(converted);
            return converted;
        } catch (Exception e) {
            throw new ActionException("Couldn't proxy request to Szopa server",
                    e);
        } finally {
            try {
                con.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    protected String ConvertData(String data) throws ActionException {
        return data;
    }

    protected String TryGetCacheDataIfDesired() {
        if (this.getUseCache()) {
            String key = this.getCacheKey();
            if (key != null) {
                final String cachedData = JedisManager.get(key);
                if (cachedData != null && !cachedData.isEmpty()) {
                    return cachedData;
                }
            }
            log.info("not in cache", key);
        }

        return null;
    }

    protected void CacheDataIfDesired(String data) {
        if (this.getUseCache()) {
            String key = this.getCacheKey();
            if (key != null) {
                JedisManager.setex(key, JedisManager.EXPIRY_TIME_DAY, data);
            }
        }
    }

    public User getUser() {
        return _user;
    }

    public void setUser(User user) {
        this._user = user;
    }
}
