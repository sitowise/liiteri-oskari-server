package fi.nls.oskari.control.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetCurrentUserData")
public class GetCurrentUserDataHandler extends ActionHandler {

    private static final Logger log = LogFactory.getLogger(GetCurrentUserDataHandler.class);
    private static final String KEY_UID = "currentUserUid";
    
    private static final String KEY_USER = "user";
    private static final String KEY_FIRSTNAME = "firstName";
    private static final String KEY_LASTNAME = "lastName";
    private static final String KEY_LOGINNAME = "loginName";
    private static final String KEY_NICKNAME = "nickName";
    private static final String KEY_USERUUID = "userUUID";
    private static final String KEY_USERID = "userID";
    private static final String KEY_TOSACCEPTED = "tosAccepted";


    private final static String KEY_ROLE_ID = "id";
    private final static String KEY_ROLE_NAME = "name";
    private final static String KEY_ROLES = "roles";

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
    	User user = params.getUser();    	
    	JSONObject json = getUserJSON(user);    	
        ResponseHelper.writeResponse(params, json.toString());
    }
    
    private JSONObject getUserJSON(final User user) {
        try {
            JSONObject userData = new JSONObject();
            userData.put(KEY_FIRSTNAME, user.getFirstname());
            userData.put(KEY_LASTNAME, user.getLastname());
            userData.put(KEY_LOGINNAME, user.getEmail());
            userData.put(KEY_NICKNAME, user.getScreenname());
            userData.put(KEY_USERUUID, user.getUuid());
            userData.put(KEY_USERID, user.getId());
            userData.put(KEY_TOSACCEPTED, user.getTosAccepted());

            JSONArray roles = getUserRolesJSON(user);
            userData.put(KEY_ROLES, roles);
            return userData;
        } catch (JSONException jsonex) {
            log.warn("Unable to populate user data:", user);
        }
        return null;
    }
    
    private JSONArray getUserRolesJSON(final User user) throws JSONException {
        JSONArray userRoles = new JSONArray();
        for (Role role: user.getRoles()) {
            JSONObject roleData = new JSONObject();
            roleData.put(KEY_ROLE_ID, role.getId());
            roleData.put(KEY_ROLE_NAME, role.getName());
            userRoles.put(roleData);
        }
        return userRoles;
    }
}
