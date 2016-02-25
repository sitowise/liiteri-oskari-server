package fi.nls.oskari.control.data;

import fi.mml.portti.service.search.SearchCriteria;
import fi.nls.oskari.SearchWorker;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Get search result of ELF Geolocator request
 * <p/>
 * e.g. request oskari-map?action_route=GetGeoLocatorSearchResult&lang=fi&epsg=EPSG:3035&term=Helsinki&filter=&fuzzy=true&exonym=false"
 */
@OskariActionRoute("GetGeoLocatorSearchResult")
public class GetGeoLocatorSearchResultHandler extends ActionHandler {

    private static final String PARAM_TERM = "term";
    private static final String PARAM_REGION = "region";
    private static final String PARAM_COUNTRY = "country";
    private static final String PARAM_FILTER = "filter";
    private static final String PARAM_NORMAL = "normal";
    private static final String PARAM_FUZZY = "fuzzy";
    private static final String PARAM_EXONYM = "exonym";
    private static final String PARAM_EPSG_KEY = "epsg";
    private static final String PARAM_LON = "lon";
    private static final String PARAM_LAT = "lat";

    private final static Logger log = LogFactory.getLogger(GetGeoLocatorSearchResultHandler.class);



    private String[] channels = new String[0];

    public void init()
    {
        channels = PropertyUtil.getCommaSeparatedList("actionhandler.GetSearchResult.channels");

    }


    public void handleAction(final ActionParameters params) throws ActionException {

        log.debug("in handle action");

        final String search = params.getHttpParam(PARAM_TERM,"foo");
        if (search == null) {
            throw new ActionParamsException("Search string was null");
        }

        final String epsg = params.getHttpParam(PARAM_EPSG_KEY);

        final String error = SearchWorker.checkLegalSearch(search);

        if (!SearchWorker.STR_TRUE.equals(error)) {
            // write error message key
            ResponseHelper.writeResponse(params, error);
        } else {
            final Locale locale = params.getLocale();

            final SearchCriteria sc = new SearchCriteria();
            sc.setSearchString(search);
            sc.setSRS(epsg);  // eg. EPSG:3067

            sc.setLocale(locale.getLanguage());
            sc.addParam(PARAM_REGION, params.getHttpParam(PARAM_REGION, ""));
            sc.addParam(PARAM_COUNTRY, params.getHttpParam(PARAM_COUNTRY, ""));
            sc.addParam(PARAM_FILTER, params.getHttpParam(PARAM_FILTER, "false"));
            sc.addParam(PARAM_NORMAL, params.getHttpParam(PARAM_NORMAL, "false"));
            sc.addParam(PARAM_FUZZY, params.getHttpParam(PARAM_FUZZY, "false"));
            sc.addParam(PARAM_EXONYM, params.getHttpParam(PARAM_EXONYM, "false"));
            sc.addParam(PARAM_LON, params.getHttpParam(PARAM_LON, ""));
            sc.addParam(PARAM_LAT, params.getHttpParam(PARAM_LAT, ""));

            for (String channelId : channels) {
                sc.addChannel(channelId);
            }

            final JSONObject result = SearchWorker.doSearch(sc);
            ResponseHelper.writeResponse(params, result);
        }
    }
}
