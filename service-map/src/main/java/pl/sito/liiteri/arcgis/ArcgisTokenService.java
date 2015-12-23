package pl.sito.liiteri.arcgis;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import pl.sito.liiteri.utils.HttpUtils;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;

//TODO: send request using json
//TODO: get token url using request to http://sampleserver3.arcgisonline.com/arcgis/rest/info http://paikkatieto.ymparisto.fi/arcgis/rest/info

public class ArcgisTokenService
{
    private static class ArcgisTokenServiceHolder {
        static final ArcgisTokenService INSTANCE = new ArcgisTokenService();
    }
	
	public static ArcgisTokenService getInstance() {
		return ArcgisTokenServiceHolder.INSTANCE;
	}
	
	private static final Logger log = LogFactory.getLogger(ArcgisTokenService.class);
	
	private static final String[] ValidReferers = PropertyUtil.getCommaSeparatedList("liiteri.arcgis.token.referers");
	
	private HashMap<String, ArcgisToken> _tokenLookup = new HashMap<String, ArcgisToken>();
	private HashMap<String, ArcgisServerConfiguration> _configurationLookup = new HashMap<String, ArcgisServerConfiguration>();
	
	protected ArcgisTokenService() {
		LoadConfigurationFromProperties();
	}
	
	private void LoadConfigurationFromProperties() {
		String serverUrl = PropertyUtil.get("liiteri.arcgis.url");
		String serverUsername = PropertyUtil.get("liiteri.arcgis.user");
		String serverPassword = PropertyUtil.get("liiteri.arcgis.password");
		
		String statisticServerUrl = PropertyUtil.get("liiteri.statistics.arcgis.url");
		String statisticServerUsername = PropertyUtil.get("liiteri.statistics.arcgis.user");
		String statisticServerPassword = PropertyUtil.get("liiteri.statistics.arcgis.password");
		
		_configurationLookup.put(serverUrl, new ArcgisServerConfiguration(serverUsername, serverPassword, serverUrl));
		
		if (!serverUrl.equals(statisticServerUrl)) {
			_configurationLookup.put(statisticServerUrl, new ArcgisServerConfiguration(statisticServerUsername, statisticServerPassword, statisticServerUrl));	
		}						
	}	
	
	public ArcgisToken getTokenForUrl(String url, ArcgisTokenConfiguration configuration) 
	{	
		log.info("Getting token for url [%s]", url);
		String serverUrl = GetServerUrl(url);
		
		return getTokenForServer(serverUrl, configuration);
	}
	
	public ArcgisToken getTokenForServer(String serverUrl, ArcgisTokenConfiguration configuration) 
	{	
		ArcgisToken token = ArcgisToken.EMPTY;
		log.info("Getting token for serverUrl [%s]", serverUrl);
		
		String msg = "";
		if (!Validate(configuration, msg)) {
			log.error("Invalid parameters for getting token", msg);
			return token;
		}
		
		/* double-checked lock */
		ArcgisToken tokenFromCache = this.getDataFromCache(serverUrl, configuration);			
		if (tokenFromCache == null || System.currentTimeMillis() > tokenFromCache.getExpiration()) 
		{			
			synchronized(this) {
				tokenFromCache = this.getDataFromCache(serverUrl, configuration);
				if (tokenFromCache == null || System.currentTimeMillis() > tokenFromCache.getExpiration()) 
				{
					log.info(String.format("Token for [%s] is not in cache", serverUrl));
					token = this.getTokenFromServer(serverUrl, configuration);
					this.cacheData(serverUrl, configuration, token);					
				}
				else {
					log.debug(String.format("Token for [%s] is in cache", serverUrl));
					token = tokenFromCache;		
				}
			}						
		}	
		else 
		{
			log.debug(String.format("Token for [%s] is in cache", serverUrl));
			token = tokenFromCache;
		}
		
		return token;
	}
	
	private String GetServerUrl(String url) {
		String result = url;
		
		try {
			URL urlObj = new URL(url);
			result = urlObj.getProtocol() + "://" + urlObj.getHost();
		}
		catch (MalformedURLException mue) {
			/* suppress */
		}		
		
		log.debug("Mapped [%s] to [%s]", url, result);
		
		return result;
	}
	
	private boolean Validate(final ArcgisTokenConfiguration configuration, String msg)
	{
		boolean valid = true;
		
		if (configuration.getTokenType() == ArcgisTokenConfiguration.TokenType.Referer) {
			String referer = configuration.getReferer();
			if (referer != null && !referer.isEmpty()) {
				msg = "invalid referer " + referer;
				valid = false;
				for(String validReferer: ValidReferers) {
				    if (validReferer.equals(referer)) {
				    	valid = true;
				    	break;
				    }
				}
			}
		}				
			
		return valid;
	}
	
	private ArcgisToken getTokenFromServer(String url, ArcgisTokenConfiguration tokenConf) {
		ArcgisToken token = ArcgisToken.EMPTY;
		ArcgisServerConfiguration configuration = _configurationLookup.get(url);		
		
		if (configuration != null) 
		{
			String tokenUrl = configuration.getTokenUrl();
			try
			{
				HashMap<String, String> params = this.getRequestParameters(configuration, tokenConf);
				String data = HttpUtils.sendPost(tokenUrl, params);
				token = new ArcgisToken(data);
			} 
			catch (Exception e)
			{
				log.error("Cannot get token from server", e);
			}			
		}
		else 
		{
			log.warn(String.format("There is no configuration for server [%s]", url));
		}
		
		return token;		
	}			

	private HashMap<String, String> getRequestParameters(ArcgisServerConfiguration configuration, ArcgisTokenConfiguration tokenConf)
	{
		HashMap<String, String> postData = new HashMap<String, String>();
		postData.put("username", configuration.getUsername());
		postData.put("password", configuration.getPassword());
		postData.put("f", "json");
		postData.put("expiration", "60");
		
		switch (tokenConf.getTokenType()) 
		{
			case Referer:
				postData.put("client", "referer");
				postData.put("referer", tokenConf.getReferer());
				postData.put("ip", "");
				break;
			case Ip:
				postData.put("client", "ip");
				postData.put("referer", "");
				postData.put("ip", tokenConf.getIp());
				break;
			case Request:
				postData.put("client", "requestip");
				postData.put("referer", "");
				postData.put("ip", "");
				break;
			default:
				break; 
		}
		
		return postData;		
	}

	private synchronized ArcgisToken getDataFromCache(String url, ArcgisTokenConfiguration tokenConf) {
		String key = url + ":" + tokenConf.toString();
		return _tokenLookup.get(key);
	}
	
	private synchronized void cacheData(String url, ArcgisTokenConfiguration tokenConf, ArcgisToken token) {
		String key = url + ":" + tokenConf.toString();
		_tokenLookup.put(key, token);
	}
}
