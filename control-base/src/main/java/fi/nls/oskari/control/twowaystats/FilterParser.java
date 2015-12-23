package fi.nls.oskari.control.twowaystats;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import fi.mml.portti.domain.permissions.Permissions;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.szopa.RegionDefinition;
import fi.nls.oskari.control.szopa.RegionDefinition.RegionType;
import fi.nls.oskari.control.szopa.RegionService;
import fi.nls.oskari.domain.User;

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
            final List<String> permissionsList = permissionsService
                    .getResourcesWithGrantedPermissions("operation", user,
                            Permissions.PERMISSION_TYPE_EXECUTE);

            if (rulesWithFunctionalAreas > 1
                    && !permissionsList
                            .contains("statistics+functional_intersection")) {
                throw new ActionException("No permissions");
            }

            if (rulesWithGrid > 0
                    && !permissionsList.contains("statistics+grid")) {
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

        String[] filterParts = param.split(":");

        if (filterParts.length != 2 || !filterParts[0].equalsIgnoreCase(type)) {
            return "";
        }

        if (user == null) {
            throw new ActionException("No permissions");
        }
        PermissionsService permissionsService = new PermissionsServiceIbatisImpl();
        final List<String> permissionsList = permissionsService
                .getResourcesWithGrantedPermissions("operation", user,
                        Permissions.PERMISSION_TYPE_EXECUTE);

        if (!permissionsList.contains("statistics+grid")) {
            throw new ActionException("No permissions");
        }

        String[] geometryFilterParams = filterParts[1].split("\\|");
        for (int i = 0; i < geometryFilterParams.length; ++i) {
            buffer.append(gridSize + " INTERSECTS '" + geometryFilterParams[i]
                    + "'");
            if (i < geometryFilterParams.length - 1) {
                buffer.append(" OR ");
            }
        }

        return buffer.toString();
    }
}
