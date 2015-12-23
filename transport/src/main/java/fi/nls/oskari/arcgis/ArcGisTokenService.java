package fi.nls.oskari.arcgis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.github.kevinsawicki.http.HttpRequest;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.utils.HttpHelper;

public class ArcGisTokenService {
	private static final Logger log = LogFactory.getLogger(ArcGisTokenService.class);
	private static final Pattern _pattern = Pattern.compile("^(.*)/arcgis.*$");
	private static final String _serviceUrl = PropertyUtil.get("arcgis.tokenservice.url", null);
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";
    
    private static class ArcgisTokenServiceHolder {
        static final ArcGisTokenService INSTANCE = new ArcGisTokenService();
    }
	
	public static ArcGisTokenService getInstance() {
		return ArcgisTokenServiceHolder.INSTANCE;
	}
	
	protected ArcGisTokenService() {
	}
	
	@SuppressWarnings("unchecked")
	public String getTokenForLayer(String url) 
	{
		String serverUrl = getServerAddressFromUrl(url);
		String requestUrl = _serviceUrl + "&serverUrl=" + serverUrl;
		HttpRequest request = HttpHelper.getRequest(requestUrl, VALUE_CONTENT_TYPE_JSON, null, null);		
		String requestBody = request.body(OSKARI_ENCODING);
		
		JSONObject json = (JSONObject) JSONValue.parse(requestBody);
		if (json.containsKey("token"))
			return (String) json.get("token");
		
		return null;
	}
	
	private String getServerAddressFromUrl(String url) {
		
		Matcher matcher = _pattern.matcher(url);
		
		if (matcher.matches()) {
			return matcher.group(1);
		}

		log.warn("Cannot find server from url " + url);

        return null;
	}
}
