package pl.sito.liiteri.arcgis;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetArcgisToken")
public class GetArcgisTokenHandler extends ActionHandler {

	private static final Logger log = LogFactory.getLogger(GetArcgisTokenHandler.class);
	
	private final ArcgisTokenService _service = ArcgisTokenService.getInstance();
	
    private static final String PARM_SERVERURL = "serverUrl";
    private static final String HEADER_REFERER = "Referer";

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";
	
    @Override
    public void handleAction(final ActionParameters params) throws ActionException 
    {    	
    	log.debug("GetArcgisToken request received");
    	
    	String url = params.getRequiredParam(PARM_SERVERURL);
    	String referer = params.getRequest().getHeader(HEADER_REFERER);
    	
    	if (referer != null && !referer.isEmpty() && referer.charAt(referer.length()-1) == '/') {
    		referer = referer.substring(0, referer.length() - 1);
    	}    	
    	    	
		try
		{
			ArcgisTokenConfiguration tokenConf = null;
			if (referer != null && !referer.isEmpty()) {
				tokenConf = ArcgisTokenConfiguration.createRefererConfiguration(referer);
			}
			else {
				tokenConf = ArcgisTokenConfiguration.createRequestConfiguration();
			}
			
			
			final ArcgisToken token = _service.getTokenForServer(url, tokenConf);
	    	String data;
			data = token.toJSONString();
			final HttpServletResponse response = params.getResponse();
	        response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
	        response.setCharacterEncoding(OSKARI_ENCODING);
	        ResponseHelper.writeResponse(params, data);
		}
		catch (JSONException e)
		{
			log.error(e, "Cannot convert token to string");
			throw new ActionException("Cannot convert json to string");
		}
		catch (Exception e) 
		{
			log.error(e, "Error during request");
			throw new ActionException("Error during request");
		}
    }    
}
