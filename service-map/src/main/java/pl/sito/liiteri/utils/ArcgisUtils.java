package pl.sito.liiteri.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class ArcgisUtils
{
	private static final Logger log = LogFactory.getLogger(ArcgisUtils.class);
	
    public static int getArcgisId(OskariLayer layer) {
    	int id = -1;
    	
    	try {
    		id = Integer.parseInt(layer.getName());	
    	}
    	catch (NumberFormatException nfe) {
    		/* suppress */
    	}
    	    	
    	String url = layer.getUrl();
    	
    	Pattern pattern = Pattern.compile(".*show:(.*)");
    	Matcher matcher = pattern.matcher(url);
    	if (matcher.matches()) {
    		try {
    			id = Integer.parseInt(matcher.group(1));	
    		}
        	catch (NumberFormatException nfe) {
        		/* suppress */
        	}    		
    	}
    	
    	return id;
    }
    
    public static String getArcgisMapServerUrl(OskariLayer layer) {
    	String url = layer.getUrl();
    	
    	int index = url.indexOf("export?");
    	
    	if (index > -1) {
    		url = url.substring(0, index);
    	}    	
    	
    	return url;
    }
}
