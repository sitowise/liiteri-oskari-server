package fi.nls.oskari.control.szopa.requests;

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

public class SzopaRequest {

    private static final Logger log = LogFactory.getLogger(SzopaRequest.class);

    protected static String _baseUrl;

    private String _indicator = "";
    private String _format = "";
    private String[] _years = new String[0];
    private String _version = "";
    private String _group = "";
    private String _filter = "";
    private User _user;
    private String _requestBody = "";
    private String _areaYear = "";
    private String _areaType = "";
    private String _standardFilterParam = "";
    private boolean _useCache = true;

    private static Map<String, Class> _requests = new HashMap<String, Class>();
    static {
        // register possible actions
        registerAction(Indicators.class);
        registerAction(IndicatorMetadata.class);
        registerAction(Regions.class);
        registerAction(IndicatorData.class);
        registerAction(RegionAvailability.class);
        registerAction(FunctionalAreaAvailability.class);

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

    public static SzopaRequest getInstance(final String action) {
        Class c = _requests.get(action);
        if (c != null) {
            return getInstance(c);
        }
        throw new RuntimeException("Unregistered action requested:" + action);
    }

    private static SzopaRequest getInstance(final Class req) {
        try {
            return (SzopaRequest) req.newInstance();
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

    public String getIndicator() {
        return _indicator;
    }

    public void setIndicator(final String indicator) {
        this._indicator = indicator.toLowerCase();
    }

    public String getFormat() {
        return _format;
    }

    public void setFormat(final String format) {
        this._format = format.toLowerCase();
    }

    public String[] getYears() {
        return _years;
    }

    public void setYears(String[] years) {
        if (years != null && years.length > 0) {
            _years = years;
        }
    }

    public String getGroup() {
        return _group;
    }

    public void setGroup(String group) {
        this._group = group;
    }

    public String getFilter() {
        return _filter;
    }

    public void setFilter(String filter) {
        this._filter = filter;
    }

    public String getRequestBody() {
        return this._requestBody;
    }

    public void setRequestBody(String requestBody) {
        this._requestBody = requestBody;
    }

    public String getAreaYear() {
        return _areaYear;
    }

    public void setAreaYear(String areaYear) {
        this._areaYear = areaYear;
    }

    public String getAreaType() {
        return _areaType;
    }

    public void setAreaType(String areaType) {
        this._areaType = areaType;
    }

    public String getStandardFilterParam() {
        return _standardFilterParam;
    }

    public void setStandardFilterParam(String standardFilterParam) {
        this._standardFilterParam = standardFilterParam;
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
                log.info("RequestBody " + requestBody);
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
