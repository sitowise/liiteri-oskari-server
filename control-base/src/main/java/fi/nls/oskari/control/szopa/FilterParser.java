package fi.nls.oskari.control.szopa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

import fi.mml.portti.domain.permissions.Permissions;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.szopa.RegionDefinition.RegionType;
import fi.nls.oskari.domain.User;

public class FilterParser {
    private static class FilterParserHolder {
        static final FilterParser INSTANCE = new FilterParser();
    }

    public static FilterParser getInstance() {
        return FilterParserHolder.INSTANCE;
    }

    private final RegionService _regionService = RegionService.getInstance();

    private static final String BOOLEAN_KEY = "boolean";
    private static final String FILTER_KEY = "key";
    private static final String FILTER_VALUES = "values";

    public FilterParser() {

    }

    public String parseStandardFilter(String param, User user)
            throws ActionException {
        if (param == null || param.isEmpty())
            return "";

        StringBuffer buffer = new StringBuffer();

        JSONArray array = (JSONArray) JSONValue.parse(param);

        List<String> functionalAreas = new ArrayList<String>();
        for (RegionDefinition r : _regionService
                .GetRegionsOfType(RegionType.FUNCTIONAL)) {
            functionalAreas.add(r.getId());
        }
        int rulesWithFunctionalAreas = 0, rulesWithGrid = 0;

        for (int i = 0; i < array.size(); i++) {
            JSONObject item = (JSONObject) array.get(i);

            if (item.containsKey(BOOLEAN_KEY)) {
                buffer.append(" " + item.get(BOOLEAN_KEY) + " ");
            } else {
                JSONArray valuesArray = (JSONArray) item.get(FILTER_VALUES);
                String key = item.get(FILTER_KEY).toString();
                String apiKey = _regionService.getAPIId(key);
                buffer.append(" (");
                for (int j = 0; j < valuesArray.size(); j++) {
                    if (j != 0)
                        buffer.append(" OR ");
                    buffer.append(apiKey);
                    buffer.append(" = ");
                    buffer.append(valuesArray.get(j));
                }
                buffer.append(")");

                if (functionalAreas.contains(key)) {
                    ++rulesWithFunctionalAreas;
                    ++rulesWithGrid;
                } else if (key.startsWith("grid")) {
                    ++rulesWithGrid;
                }
            }
        }

        if (rulesWithFunctionalAreas > 1 || rulesWithGrid > 0) {
            if (user == null) {
                throw new ActionException("No permissions");
            }
            PermissionsService permissionsService = new PermissionsServiceIbatisImpl();
            final Set<String> permissionsList = permissionsService
                    .getResourcesWithGrantedPermissions("operation", user,
                            Permissions.PERMISSION_TYPE_EXECUTE);
            boolean functionalIntersectionAllowed = false;
            boolean gridDataAllowed = false;
            for(String permission : permissionsList) {
                if("statistics+functional_intersection".equals(permission)) {
                    functionalIntersectionAllowed = true;
                } else if("statistics+grid".equals(permission)) {
                    gridDataAllowed = true;
                }
            }
            if (rulesWithFunctionalAreas > 1
                    && !functionalIntersectionAllowed) {
                throw new ActionException("No permissions");
            }

            if (rulesWithGrid > 0
                    && !gridDataAllowed) {
                throw new ActionException("No permissions");
            }
        }

        return buffer.toString();
    }

    public String parseGeometryFilter(String param, User user)
            throws ActionException {
        return parseGeometryFilter(param, "grid250m", user);
    }

    public String parseGeometryFilter(String param, String gridSize, User user)
            throws ActionException {
        if (param == null || param.isEmpty())
            return "";

        if (user == null) {
            throw new ActionException("No permissions");
        }
        PermissionsService permissionsService = new PermissionsServiceIbatisImpl();
        final Set<String> permissionsList = permissionsService
                .getResourcesWithGrantedPermissions("operation", user,
                        Permissions.PERMISSION_TYPE_EXECUTE);
        boolean gridDataAllowed = false;
        for(String permission : permissionsList) {
            if("statistics+grid".equals(permission)) {
                gridDataAllowed = true;
            }
        }
        if (!gridDataAllowed) {
            throw new ActionException("No permissions");
        }

        StringBuffer buffer = new StringBuffer();

        String[] geometryFilterParams = param.split("\\|");

        GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel(10));
        WKTReader reader = new WKTReader(geomFactory);
        WKTWriter writer = new WKTWriter();

        for (int i = 0; i < geometryFilterParams.length; ++i) {
            try {
                Geometry g = reader.read(geometryFilterParams[i]).buffer(0);

                buffer.append(gridSize + " INTERSECTS '" + writer.write(g)
                        + "'");
            } catch (ParseException e) {
                throw new ActionException("Geometry error", e);
            }

            if (i < geometryFilterParams.length - 1) {
                buffer.append(" OR ");
            }
        }

        return buffer.toString();
    }

    public Map<String, List<String>> getAreasFromFilter(String filter,
            RegionType type) {
        Map<String, List<String>> areas = new HashMap<String, List<String>>();
        if (filter == null || filter.length() == 0) {
            return areas;
        }

        JSONArray array = (JSONArray) JSONValue.parse(filter);

        List<String> areasOfInterest = new ArrayList<String>();
        for (RegionDefinition r : _regionService.GetRegionsOfType(type)) {
            areasOfInterest.add(r.getId());
        }

        for (int i = 0; i < array.size(); i++) {
            JSONObject item = (JSONObject) array.get(i);

            if (!item.containsKey(BOOLEAN_KEY)) {
                JSONArray valuesArray = (JSONArray) item.get(FILTER_VALUES);
                String key = item.get(FILTER_KEY).toString();
                List<String> ids = new ArrayList<String>();

                if (areasOfInterest.contains(key)) {
                    String apiKey = _regionService.getAPIId(key);
                    for (int j = 0; j < valuesArray.size(); j++) {
                        ids.add((String) valuesArray.get(j));
                    }
                    areas.put(apiKey, ids);
                }
            }
        }

        return areas;
    }

    public String getFilterFromAreas(Map<String, List<String>> areas) {
        StringBuffer buffer = new StringBuffer();
        int count = 0;

        for (String key : areas.keySet()) {
            if (count++ != 0) {
                buffer.append(" AND ");
            }

            List<String> values = areas.get(key);
            buffer.append("(");
            for (int j = 0; j < values.size(); j++) {
                if (j != 0)
                    buffer.append(" OR ");
                buffer.append(key);
                buffer.append(" = ");
                buffer.append(values.get(j));
            }
            buffer.append(")");
        }

        return buffer.toString();
    }
}
