package fi.nls.oskari.domain;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.PropertyUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Collection;

/**
 * Internal model for user role.
 * Admin role name can be configured with property "oskari.user.role.admin" in PropertyUtil
 * and defaults to "Administrator".
 * Default logged in user role name can be configured with property "oskari.user.role.loggedIn" in PropertyUtil
 * and defaults to "User".
 */
public class Role implements Serializable {

	public static final String GROUPINGS_ADMIN = "liiteri_groupings_admin";
	public static final String ENV_USER = "liiteri_env_user";
	public static final String AUTHORIZED_USER = "liiteri_authorized_user";
	public static final String USER = "liiteri_user";
	
    private static final Logger log = LogFactory.getLogger(Role.class);
    private static Role ADMIN_ROLE = null;
    private static Role USER_ROLE = null;
    public static final String DEFAULT_ADMIN_ROLE_NAME = "Admin";
    public static final String DEFAULT_USER_ROLE_NAME = "User";
    private final static String KEY_ROLE_ID = "id";
    private final static String KEY_ROLE_NAME = "name";

    private long id;
    private String name;

    public static boolean hasRoleWithName(Collection<Role> roles, final String name) {
        if(roles == null || name == null) {
            return false;
        }
        for (Role r : roles) {
            if (r.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deprecated - Use getAdminRole() instead.
     * @return
     */
    @Deprecated
    public static String getAdminRoleName() {
        ADMIN_ROLE = getAdminRole();
        if(ADMIN_ROLE == null) {
            return PropertyUtil.get("oskari.user.role.admin", DEFAULT_ADMIN_ROLE_NAME).trim();
        }
        return getAdminRole().getName();
    }

    /**
     * Returns Admin role
     * @return
     */
    public static Role getAdminRole() {
        if(ADMIN_ROLE == null) {
            // default to Administrator
            final String rolename = PropertyUtil.get("oskari.user.role.admin", DEFAULT_ADMIN_ROLE_NAME).trim();
            ADMIN_ROLE = getRoleByName(rolename);
        }
        return ADMIN_ROLE;
    }

    /**
     * Returns default role for logged in users
     * @return
     */
    public static Role getDefaultUserRole() {
        if(USER_ROLE == null) {
            // default to User
            final String rolename = PropertyUtil.get("oskari.user.role.loggedIn", DEFAULT_USER_ROLE_NAME).trim();
            USER_ROLE = getRoleByName(rolename);
        }
        return USER_ROLE;
    }

    private static Role getRoleByName(final String rolename) {
        try {
            return UserService.getInstance().getRoleByName(rolename);
        } catch (ServiceException ex) {
            log.error(ex, "Error getting UserService");
        }
        return null;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public boolean isAdminRole() {
        return (getAdminRoleName().equals(getName()));
    }

    public JSONObject toJSON() {
        try {
            JSONObject roleData = new JSONObject();
            roleData.put(KEY_ROLE_ID, getId());
            roleData.put(KEY_ROLE_NAME, getName());

            return roleData;
        } catch (JSONException jsonex) {
            log.warn("Unable to construct JSON role data:", this);
        }
        return null;
    }

    public static Role parse(JSONObject json) {

        try {
            Role role = new Role();
            role.setId(json.optInt(KEY_ROLE_ID));
            role.setName(json.optString(KEY_ROLE_NAME));

            return role;
        } catch (Exception jsonex) {
            log.warn("Unable to parse role from JSON:", json);
        }
        return null;
    }
}
