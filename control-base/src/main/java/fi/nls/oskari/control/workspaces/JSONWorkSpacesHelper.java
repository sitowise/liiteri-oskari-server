package fi.nls.oskari.control.workspaces;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.domain.groupings.Grouping;
import fi.nls.oskari.domain.groupings.GroupingPermission;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.domain.groupings.GroupingThemeData;
import fi.nls.oskari.domain.workspaces.*;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;

public class JSONWorkSpacesHelper {

    private static final Logger log = LogFactory
            .getLogger(JSONWorkSpacesHelper.class);

    public static final JSONObject createWorkSpacesJSONOutput(
            List<WorkSpace> workspaces, List<WorkSpace> externalWorkSpaces,
            List<WorkSpace> hiddenWorkSpaces) {
        JSONArray arr = new JSONArray();
        for (WorkSpace workspace : workspaces) {
            JSONObject ob = createWorkSpacesJSONOutput(workspace, true);
            JSONArray arrSharing = new JSONArray();
            for (WorkSpaceSharing sharing : workspace.getWorkSpaceSharing()) {
                JSONObject obSharing = new JSONObject();
                JSONHelper.putValue(obSharing, "id", sharing.getId());
                JSONHelper.putValue(obSharing, "externalType",
                        sharing.getExternalType());
                JSONHelper.putValue(obSharing, "externalId",
                        sharing.getExternalId());
                JSONHelper.putValue(obSharing, "email", sharing.getEmail());

                arrSharing.put(obSharing);
            }
            JSONHelper.putValue(ob, "users", arrSharing);
            arr.put(ob);
        }

        for (WorkSpace w : externalWorkSpaces) {
            arr.put(createWorkSpacesJSONOutput(w, false));
        }

        for (WorkSpace w : hiddenWorkSpaces) {
            arr.put(createWorkSpacesJSONOutput(w, false));
        }
        JSONObject ob = new JSONObject();
        JSONHelper.putValue(ob, "workspaces", arr);
        return ob;
    }

    /*
     * public static final WorkSpace buildWorkSpaceObject(long id, String
     * name,String settings, Date expirationDate, long userId,String Users)
     * throws JSONException {
     * 
     * WorkSpace workSpace = new WorkSpace(); workSpace.setName(name);
     * workSpace.setId(id); workSpace.setSettings(settings);
     * workSpace.setUserId(userId); SimpleDateFormat formatter = new
     * SimpleDateFormat("yyyy-MM-dd");
     * 
     * try { workSpace.setExpirationDate(formatter.parse(mainJSON
     * .getString("expirationDate"))); } catch (ParseException e) { throw new
     * JSONException("Error during date parsing"); }
     * 
     * JSONArray users = mainJSON.getJSONArray("users");
     * 
     * for (int i = 0; i < users.length(); i++) {
     * workSpace.getWorkSpaceSharing().add(
     * createWorkSpaceSharing(users.getJSONObject(i)));
     * 
     * } return workSpace; }
     */

    private static JSONObject createWorkSpacesJSONOutput(WorkSpace workSpace,
            boolean editable) {
        JSONObject obExt = new JSONObject();
        JSONHelper.putValue(obExt, "id", workSpace.getId());
        JSONHelper.putValue(obExt, "name", workSpace.getName());
        JSONHelper.putValue(obExt, "workspace", workSpace.getSettings());
        JSONHelper.putValue(obExt, "expirationDate", new SimpleDateFormat(
                "yyyy-MM-dd").format(workSpace.getExpirationDate()));
        JSONHelper.putValue(obExt, "userId", workSpace.getUserId());
        JSONHelper.putValue(obExt, "edit", editable);
        return obExt;
    }

    public static final List<WorkSpaceSharing> findWorkSpaceSharing(
            long workSpaceId, List<WorkSpaceSharing> sharing) {
        List<WorkSpaceSharing> list = new ArrayList<WorkSpaceSharing>();
        for (WorkSpaceSharing s : sharing) {
            if (s.getWorkSpaceId() == workSpaceId)
                list.add(s);
        }
        return list;
    }

    public static final List<WorkSpaceSharing> getWorkSpaceSharingObjectList(
            String json) throws JSONException {
        List<WorkSpaceSharing> sharingList = new ArrayList<WorkSpaceSharing>();
        if (json != null && !json.isEmpty()) {
            JSONArray sharing = JSONHelper.createJSONArray(json);
            if (sharing != null) {
                for (int i = 0; i < sharing.length(); i++) {
                    sharingList.add(buildWorkSapceSharing(sharing
                            .getJSONObject(i)));
                }
            }
        }
        return sharingList;
    }

    private static WorkSpaceSharing buildWorkSapceSharing(JSONObject sharing)
            throws JSONException {
        WorkSpaceSharing s = new WorkSpaceSharing();

        if (sharing.has("externalId")) {
            s.setExternalId(sharing.getLong("externalId"));
        } else {
            // in this case there is given only email address (user ID is
            // unknown yet)
            s.setExternalId(0);
        }

        s.setId(sharing.optLong("id", 0));
        s.setEmail(sharing.getString("email"));
        s.setExternalType(sharing.getString("externalType"));

        return s;
    }

    public static final JSONObject createJSONMessageObject(Long id,
            String message) {
        JSONObject ob = new JSONObject();
        JSONHelper.putValue(ob, "message", message);
        if (id != null) {
            JSONHelper.putValue(ob, "id", id);

        }
        return ob;
    }

    private static GroupingTheme buildGroupingThemes(JSONObject layer,
            boolean isStatistic) throws JSONException {

        GroupingTheme theme = new GroupingTheme();
        theme.setName(isStatistic ? "Default stats name" : "Default theme name");
        theme.setThemeType(isStatistic ? 1 : 0);
        theme.setStatus(2);
        theme.setPublic(true);
        GroupingThemeData data = new GroupingThemeData();
        data.setName(isStatistic ? "Default layer theme data  name"
                : "default stats theme data name");
        data.setDataId(layer.getLong("id"));
        theme.setThemeData(Arrays.asList(data));

        return theme;
    }

    public static Grouping transformWorkSpace(Grouping res, String settings,
            String name, long userId) throws JSONException {

        if (res == null) {
            res = new Grouping();
        }

        res.setStatus(2);
        res.setName("_ServicePackage_from_WorkSpace_" + name);
        res.setUserGroup("");

        if (settings != null && !settings.isEmpty()) {
            JSONObject ob = JSONHelper.createJSONObject(settings);
            ob.remove("servicePackage");

            /*
             * JSONArray layers = JSONHelper.getJSONArray(ob, "selectedLayers");
             * if (layers != null) { List<GroupingTheme> themes = new
             * ArrayList<GroupingTheme>(); for (int i = 0; i < layers.length();
             * i++) {
             * 
             * themes.add(buildGroupingThemes(layers.getJSONObject(i), false));
             * } if (themes.size() > 0) { res.setThemes(themes); } }
             * 
             * JSONObject statistics = JSONHelper.getJSONObject(ob,
             * "statistics"); if (statistics != null) { JSONObject state =
             * JSONHelper.getJSONObject(ob, "state"); if (state != null) {
             * JSONArray indicators = JSONHelper.getJSONArray(ob, "indicators");
             * if (indicators != null) { List<GroupingTheme> stats = new
             * ArrayList<GroupingTheme>(); for (int i = 0; i <
             * indicators.length(); i++) {
             * 
             * stats.add(buildGroupingThemes( indicators.getJSONObject(i),
             * true)); } if (stats.size() > 0) { res.setThemes(stats); } } } }
             */
            // /Addin permission to current user - not sure if needed already or
            // should be setted later
            if (userId > 0) {
                GroupingPermission per = new GroupingPermission();
                per.setExternalId(userId);
                per.setExternalType("USER");
                res.setPermissions(Arrays.asList(per));
            }

            res.setMapState(ob.toString());
        }

        res.setStatus(2);
        res.setName("_ServicePackage_from_WorkSpace_" + name);
        res.setUserGroup("");

        return res;
    }

    public static int getServicePackageId(String settings) {
        int servicePackageId = -1;

        if (settings != null && !settings.isEmpty()) {
            JSONObject ob = JSONHelper.createJSONObject(settings);

            try {
                servicePackageId = ob.getInt("servicePackage");
            } catch (JSONException e) {
                return -1;
            }
        }

        return servicePackageId;
    }
}
