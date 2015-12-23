package fi.nls.oskari.control.szopa;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.esri.core.geometry.Envelope;

import pl.sito.liiteri.stats.domain.GridStatsResult;
import pl.sito.liiteri.stats.domain.GridStatsResultItem;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.szopa.requests.SzopaRequest;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class IndicatorService {
    private static class IndicatorServiceHolder {
        static final IndicatorService INSTANCE = new IndicatorService();
    }

    public static IndicatorService getInstance() {
        return IndicatorServiceHolder.INSTANCE;
    }

    private static final Logger log = LogFactory
            .getLogger(IndicatorService.class);
    private final FilterParser filterParser = FilterParser.getInstance();

    private final String REQUEST_NAME = "data";
    private final String PRIVACY_LIMIT_KEY = "PrivacyLimitTriggered";
    private final String VALUE_KEY = "primary value";
    private final String COORDINATES_KEY = "alternativeId";
    private final String ID_KEY = "region";

    protected IndicatorService() {

    }

    // public GridStatsResult getGridIndicatorDataWithMap(String id, String[]
    // years, int gridSize, String geometryFilter) throws ActionException {
    // GridStatsResult result = getGridIndicatorData(id, years, 250,
    // geometryFilter);
    // if (gridSize != 250) {
    // result = MapResult(result, gridSize/250);
    // }
    //
    // return result;
    // }

    public GridStatsResult getGridIndicatorData(String id, String[] years,
            int gridSize, String filter, String geometryFilter, Envelope bbox,
            User user) throws ActionException {
        GridStatsResult result = new GridStatsResult();

        String group;
        if (gridSize >= 1000)
            group = "grid" + (gridSize / 1000) + "km";
        else
            group = "grid" + (gridSize) + "m";

        final String filterString = getFilter(filter, geometryFilter, bbox,
                group, user);
        final SzopaRequest req = SzopaRequest.getInstance(REQUEST_NAME);
        req.setIndicator(id);
        req.setYears(years);
        req.setGroup(group);
        req.setFilter(filterString);
        req.setVersion("v1");
        req.setUseCache(true);
        req.setUser(user);

        long start = System.nanoTime();
        String strResult = req.getData();
        long end = System.nanoTime();
        log.info("Get request data " + (end - start) / 1000000.0 + "ms");

        JSONArray array = (JSONArray) JSONValue.parse(strResult);
        if (array != null) {
            for (Object objItem : array) {
                JSONObject item = (JSONObject) objItem;
                GridStatsResultItem resultItem = new GridStatsResultItem();
                double value = 0;
                try {
                    Object privacyLimit = item.get(PRIVACY_LIMIT_KEY);
                    if (privacyLimit != null
                            && Boolean.parseBoolean(privacyLimit.toString()) == true) {
                        // skip - privacy limit triggered
                        continue;
                    }
                    Object itemValue = item.get(VALUE_KEY);
                    if (itemValue == null) {
                        // skip - null value
                        continue;
                    }

                    value = Double.parseDouble(itemValue.toString());
                } catch (Exception e) {
                    log.warn(e, "Cannot parse item");
                    continue;
                }
                resultItem.setValue(value);

                String encodedCoordinates = (String) item.get(COORDINATES_KEY);
                if (encodedCoordinates != null && !encodedCoordinates.isEmpty()
                        && encodedCoordinates.length() > 6) {
                    int north = Integer.parseInt(encodedCoordinates
                            .substring(6));
                    int easting = Integer.parseInt(encodedCoordinates
                            .substring(0, 6));
                    resultItem.setNorthing(north);
                    resultItem.setEasting(easting);
                }

                long resultItemId = Long.parseLong(item.get(ID_KEY).toString()
                        .split(":")[1]);
                resultItem.setId(resultItemId);

                result.AddItem(resultItem);
            }
        }

        return result;
    }

    private String getFilter(final String filter, final String geometryFilter,
            final Envelope bbox, final String gridSizeString, User user)
            throws ActionException {
        String result = "";
        boolean isEmpty = true;

        String parsedStandardFilter = filterParser.parseStandardFilter(filter,
                user);
        if (!parsedStandardFilter.isEmpty()) {
            result += parsedStandardFilter;
            isEmpty = false;
        }

        if (bbox != null) {
            StringBuilder buf = new StringBuilder();
            buf.append("POLYGON((");
            buf.append(bbox.getXMin());
            buf.append(" ");
            buf.append(bbox.getYMin());
            buf.append(",");
            buf.append(bbox.getXMin());
            buf.append(" ");
            buf.append(bbox.getYMax());
            buf.append(",");
            buf.append(bbox.getXMax());
            buf.append(" ");
            buf.append(bbox.getYMax());
            buf.append(",");
            buf.append(bbox.getXMax());
            buf.append(" ");
            buf.append(bbox.getYMin());
            buf.append(",");
            buf.append(bbox.getXMin());
            buf.append(" ");
            buf.append(bbox.getYMin());
            buf.append("))");

            String parsedBboxGeometryFilter = filterParser.parseGeometryFilter(
                    buf.toString(), gridSizeString, user);
            if (!parsedBboxGeometryFilter.isEmpty()) {

                if (!isEmpty)
                    result += " AND ( ";

                result += parsedBboxGeometryFilter;

                if (!isEmpty)
                    result += " )";

                isEmpty = false;
            }
        }

        String parsedGeometryFilter = filterParser.parseGeometryFilter(
                geometryFilter, user);
        if (!parsedGeometryFilter.isEmpty()) {

            if (!isEmpty)
                result += " AND ( ";

            result += parsedGeometryFilter;

            if (!isEmpty)
                result += " )";

            isEmpty = false;
        }
        return result;
    }
}
