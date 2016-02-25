package fi.nls.oskari.search.channel;

import fi.mml.portti.service.search.ChannelSearchResult;
import fi.mml.portti.service.search.SearchCriteria;
import fi.nls.oskari.annotation.Oskari;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.search.util.NLSNearestFeatureParser;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.PropertyUtil;

import java.util.Locale;

/**
 * Search channel for NLS nearest feature requests
 * sample request
 * https://ws.nls.fi/maasto/nearestfeature?TYPENAME=oso:Osoitepiste&COORDS=385445,6675125,EPSG:3067&SRSNAME=EPSG:3067&MAXFEATURES=1&BUFFER=1000
 */
@Oskari(NLSNearestFeatureSearchChannel.ID)
public class NLSNearestFeatureSearchChannel extends SearchChannel {

    private Logger log = LogFactory.getLogger(this.getClass());
    private String serviceURL = null;

    public static final String ID = "NLS_NEAREST_FEATURE_CHANNEL";
    private static final String PROPERTY_SERVICE_URL = "search.channel.NLS_NEAREST_FEATURE_CHANNEL.service.url";

    public static final String KEY_COORDS_HOLDER = "_COORDS_";
    public static final String KEY_SRSNAME_HOLDER = "_EPSG_";
    public static final String KEY_MAXFEATURES_HOLDER = "_MAXFEATURES_";
    public static final String KEY_BUFFER_HOLDER = "_BUFFER_";
    public static final String RESPONSE_CLEAN = "<?xml version='1.0' encoding='UTF-8'?>";
    public static final String REQUEST_REVERSEGEOCODE_TEMPLATE = "?TYPENAME=oso:Osoitepiste&COORDS=_COORDS_&SRSNAME=_EPSG_&MAXFEATURES=_MAXFEATURES_&BUFFER=_BUFFER_";
    private static final String PARAM_BUFFER = "buffer";
    private static final String PARAM_MAXFEATURES = "maxfeatures";
    private static final String PARAM_LON = "lon";
    private static final String PARAM_LAT = "lat";

    private NLSNearestFeatureParser nearestFeatureParser = new NLSNearestFeatureParser();

    @Override
    public void init() {
        super.init();
        serviceURL = PropertyUtil.getOptional(PROPERTY_SERVICE_URL);
        log.debug("ServiceURL set to " + serviceURL);
    }

    /**
     * Returns the search raw results.
     *
     * @param searchCriteria Search criteria.
     * @return Result data in JSON format.
     * @throws Exception
     */
    private String getData(SearchCriteria searchCriteria) throws Exception {
        log.debug("getData");
        if (serviceURL == null) {
            log.warn("ServiceURL not configured. Add property with key", PROPERTY_SERVICE_URL);
            return null;
        }

        String request = null;

        StringBuffer buf = new StringBuffer(serviceURL);
        if (hasParam(searchCriteria, PARAM_LON) && hasParam(searchCriteria, PARAM_LAT)) {
            // reverse geocoding
            // Transform lon,lat
            String coords = nearestFeatureParser.transformLonLat(searchCriteria.getParam(PARAM_LON).toString(), searchCriteria.getParam(PARAM_LAT).toString(), searchCriteria.getSRS());
            if (coords == null) {
                log.warn("Invalid lon/lat coordinates ", searchCriteria.getParam(PARAM_LON).toString(), " ", searchCriteria.getParam(PARAM_LAT).toString());
                return null;
            }
            request = REQUEST_REVERSEGEOCODE_TEMPLATE.replace(KEY_COORDS_HOLDER, coords);
        }
        // Search distance
        request = request.replace(KEY_BUFFER_HOLDER, searchCriteria.getParam(PARAM_BUFFER).toString());
        // Max features in response
        request = request.replace(KEY_MAXFEATURES_HOLDER, searchCriteria.getParam(PARAM_MAXFEATURES).toString());
        // Srs name
        request = request.replace(KEY_SRSNAME_HOLDER, searchCriteria.getSRS());
        buf.append(request);


        return IOHelper.readString(getConnection(buf.toString()));
    }

    /**
     * Check if criteria has named extra parameter and it's not empty
     *
     * @param sc
     * @param param
     * @return
     */
    private boolean hasParam(SearchCriteria sc, final String param) {
        final Object obj = sc.getParam(param);
        return obj != null && !obj.toString().isEmpty();
    }

    /**
     * Returns the channel search results.
     *
     * @param searchCriteria Search criteria.
     * @return Search results.
     */
    public ChannelSearchResult doSearch(SearchCriteria searchCriteria) {
        try {
            String data = getData(searchCriteria);

            // Clean xml version for geotools parser for faster par
            data = data.replace(RESPONSE_CLEAN, "");
            log.debug("DATA: " + data);
            // Language
            Locale locale = new Locale(searchCriteria.getLocale());
            String lang3 = locale.getISO3Language();
            return nearestFeatureParser.parse(data, searchCriteria.getSRS(), lang3);

        } catch (Exception e) {
            log.error(e, "Failed to search locations from register of NLS nearest feature service");
            return new ChannelSearchResult();
        }
    }
}
