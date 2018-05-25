package fi.nls.oskari.control.twowaystats;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
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
import fi.nls.oskari.control.szopa.RegionDefinition;
import fi.nls.oskari.control.szopa.RegionDefinition.RegionType;
import fi.nls.oskari.control.szopa.RegionService;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.util.JSONHelper;

public class FilterParser {
    private static class FilterParserHolder {
        static final FilterParser INSTANCE = new FilterParser();
    }

    public static FilterParser getInstance() {
        return FilterParserHolder.INSTANCE;
    }

    private final RegionService _regionService = RegionService.getInstance();

    private static final String FILTER_KEY = "key";
    private static final String FILTER_VALUES = "values";
    private static final String TYPE_KEY = "type";

    public FilterParser() {

    }

    public String parseStandardFilter(String param, String type, User user)
            throws ActionException {
        if (param == null || param.isEmpty() || type == null || type.isEmpty())
            return "";

        StringBuffer buffer = new StringBuffer();

        JSONArray array = (JSONArray) JSONValue.parse(param);

        int rules = 0;

        List<String> functionalAreas = new ArrayList<String>();
        for (RegionDefinition r : _regionService
                .GetRegionsOfType(RegionType.FUNCTIONAL)) {
            functionalAreas.add(r.getId());
        }
        int rulesWithFunctionalAreas = 0, rulesWithGrid = 0;

        for (int i = 0; i < array.size(); i++) {
            JSONObject item = (JSONObject) array.get(i);

            if (item.containsKey(TYPE_KEY)
                    && item.get(TYPE_KEY).toString().equalsIgnoreCase(type)) {

                if (rules++ > 0) {
                    buffer.append(" AND ");
                }

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

    public String parseGeometryFilter(String param, String type, User user)
            throws ActionException {
        return parseGeometryFilter(param, "grid250m", type, user);
    }

    public String parseGeometryFilter(String param, String gridSize,
            String type, User user) throws ActionException {
        if (param == null || param.isEmpty() || type == null || type.isEmpty())
            return "";

        StringBuffer buffer = new StringBuffer();

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

        org.json.JSONArray geomFilters = JSONHelper.createJSONArray(param);
        GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel(10));
        WKTReader reader = new WKTReader(geomFactory);
        WKTWriter writer = new WKTWriter();
        List<String> filterGeometries = new ArrayList<String>();
        try {
            for (int i = 0; i < geomFilters.length(); ++i) {
                org.json.JSONObject filterObject = geomFilters.getJSONObject(i);
                if(type.equalsIgnoreCase(filterObject.getString("direction"))) {
                    Geometry g = reader.read(filterObject.getString("geom")).buffer(0);
                    filterGeometries.add(writer.write(g));
                }
            }
        } catch (JSONException | ParseException e) {
            throw new ActionException("Could not handle geometry filters", e);
        }

        for (int i = 0; i < filterGeometries.size(); ++i) {
            buffer.append(gridSize + " INTERSECTS '" + filterGeometries.get(i)
                    + "'");
            if (i < filterGeometries.size() - 1) {
                buffer.append(" OR ");
            }
        }

        return buffer.toString();
    }
}
