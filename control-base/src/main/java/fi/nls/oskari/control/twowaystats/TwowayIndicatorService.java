package fi.nls.oskari.control.twowaystats;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import pl.sito.liiteri.stats.domain.GridStatsResult;
import pl.sito.liiteri.stats.domain.GridStatsResultItem;

import com.esri.core.geometry.Envelope;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.twowaystats.FilterParser;
import fi.nls.oskari.control.twowaystats.requests.TwowayRequest;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class TwowayIndicatorService {

    private static class TwowayIndicatorServiceHolder {
        static final TwowayIndicatorService INSTANCE = new TwowayIndicatorService();
    }

    public static TwowayIndicatorService getInstance() {
        return TwowayIndicatorServiceHolder.INSTANCE;
    }

    private static final Logger log = LogFactory
            .getLogger(TwowayIndicatorService.class);
    private final FilterParser filterParser = FilterParser.getInstance();

    protected final String REQUEST_NAME = "data";
    protected final String PRIVACY_LIMIT_KEY = "PrivacyLimitTriggered";
    protected final String VALUE_KEY = "primary value";
    protected final String COORDINATES_KEY = "alternativeId";
    protected final String ID_KEY = "region";

    protected TwowayIndicatorService() {

    }

    public GridStatsResult getGridIndicatorData(String id, String[] years,
            int gridSize, String filter, String geometryFilter, Envelope bbox,
            String type, String direction, String gender, User user)
            throws ActionException {
        GridStatsResult result = new GridStatsResult();

        String group;
        if (gridSize >= 1000)
            group = "grid" + (gridSize / 1000) + "km";
        else
            group = "grid" + (gridSize) + "m";

        final TwowayRequest req = TwowayRequest.getInstance(REQUEST_NAME);
        req.setIndicator(id);
        req.setYears(years);
        req.setGroup(direction + ":" + group);
        req.setGender(gender);
        req.setHomeFilter(getFilter(filter, geometryFilter,
                direction.equals("home") ? bbox : null, group, "home", user));
        req.setWorkFilter(getFilter(filter, geometryFilter,
                direction.equals("work") ? bbox : null, group, "work", user));
        req.setVersion("v1");
        req.setType(type);
        req.setUseCache(true);

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
                
                if(resultItem.getValue() != 0) {
                    result.AddItem(resultItem);
                }
            }
        }

        log.info("result size", result.getItems().size());

        return result;
    }

    protected String getFilter(final String filter,
            final String geometryFilter, final Envelope bbox,
            final String gridSizeString, String type, User user)
            throws ActionException {
        String result = "";
        boolean isEmpty = true;

        String parsedStandardFilter = filterParser.parseStandardFilter(filter,
                type, user);
        if (!parsedStandardFilter.isEmpty()) {
            result += parsedStandardFilter;
            isEmpty = false;
        }

        if (bbox != null) {
            JSONObject bboxFilter = new JSONObject();
            bboxFilter.put("id", "bbox");
            bboxFilter.put("direction", type);
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
            bboxFilter.put("geom", buf.toString());
            JSONArray filterArray = new JSONArray();
            filterArray.add(bboxFilter);

            String parsedBboxGeometryFilter = filterParser.parseGeometryFilter(
                    filterArray.toJSONString(), gridSizeString, type, user);
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
                geometryFilter, type, user);
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
