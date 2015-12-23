package fi.nls.oskari.control.groupings;

import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.szopa.requests.SzopaRequest;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.domain.groupings.GroupingThemeData;
import fi.nls.oskari.groupings.db.GroupingThemeDataServiceIbatisImpl;
import fi.nls.oskari.groupings.db.GroupingThemeDbService;
import fi.nls.oskari.groupings.db.GroupingThemeServiceIbatisImpl;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetUnbindedThemes")
public class GetUnbindedThemesHandler extends ActionHandler {

	private static final Logger log = LogFactory.getLogger(GetUnbindedThemesHandler.class);
	private static final GroupingThemeDbService groupingThemeService = new GroupingThemeServiceIbatisImpl();
	private static final GroupingThemeDataServiceIbatisImpl groupingThemeDataService = new GroupingThemeDataServiceIbatisImpl();

	
	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		List<GroupingTheme> themes;
		List<GroupingThemeData> data;
		themes = groupingThemeService.findAll();
		 data = groupingThemeDataService.findAll();
		 try {
				HashMap<Long, String> indicatorNames = new HashMap<Long,String>();
		        final SzopaRequest req = SzopaRequest.getInstance("indicators");
		        
		        req.setIndicator("");
		        req.setFormat("");
		        
		       	req.setVersion("v1"); 
		       	
		       	JSONArray indicators = JSONHelper.createJSONArray(req.getData());
		       	
		       	for(int i = 0; i < indicators.length(); ++i) {
		       		JSONObject obj;
					try {
						obj = indicators.getJSONObject(i);

						indicatorNames.put(obj.getLong("id"), obj.getJSONObject("name").toString());
					} catch (JSONException e) {
						log.debug("Error reading indicator", e);
					}
		       	}
	    	   JSONObject main = JSONGroupingsHelper.createUnbindedThemesJSONObject(themes, data, indicatorNames); 	   
			ResponseHelper.writeResponse(params,main);
		} catch (Exception e) {
	      
			throw new ActionException("Error during creating JSON groupings object");
	    }
		
		
	}

}
