package fi.nls.oskari.control.groupings;

import fi.mml.map.mapwindow.util.OskariLayerWorker;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.sito.liiteri.groupings.domain.UserTheme;
import pl.sito.liiteri.groupings.service.GroupingsService;

import java.util.*;

/**
 * Get WMS map layers
 */
@OskariActionRoute("GetUserThemes")
public class GetUserThemesHandler extends ActionHandler 
{
    /** Logger */
    private static Logger log = LogFactory.getLogger(GetUserThemesHandler.class);

    final static String LANGUAGE_ATTRIBUTE = "lang";
    private static final String PARM_LAYER_ID = "layer_id";
    
    private GroupingsService groupingsService = GroupingsService.getInstance();
    private String[] languages;
    
    @Override 
    public void init() {
    	super.init();
    	
    	languages = PropertyUtil.getSupportedLanguages();
    }

    @Override
    public void handleAction(ActionParameters params) throws ActionException 
    {    	    
    	//final String lang = params.getHttpParam(LANGUAGE_ATTRIBUTE, params.getLocale().getLanguage());
    	
    	List<UserTheme> themes = groupingsService.GetUserThemesWithData(params.getUser());
    	
    	JSONObject json = new JSONObject();
    	JSONArray array = new JSONArray();
		try
		{
			for (UserTheme userTheme : themes)
			{
				JSONObject userThemeJson = new JSONObject();
				userThemeJson.put("id", userTheme.getId());
				JSONObject nameJson = new JSONObject();
				for (String language : languages)
				{
					nameJson.put(language, userTheme.getName());
				}

				userThemeJson.put("name", nameJson);
				
				userThemeJson.put("layerIds", new JSONArray(userTheme.getLayerIds()));
				
				array.put(userThemeJson);
			}
			json.put("userThemes", array);

		} 
		catch (JSONException e)
		{
			log.error(e);
			throw new ActionException("Cannot get user themes");
		}
		
		ResponseHelper.writeResponseAsJson(params, json);
    }
}
