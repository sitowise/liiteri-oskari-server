package fi.nls.oskari.control.permission;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.user.MybatisRoleService;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetRoles")
public class GetRolesHandler extends ActionHandler {
	private static final MybatisRoleService roleService = new MybatisRoleService();

	@Override
	public void handleAction(ActionParameters params) throws ActionException {

		List<Role> roles = roleService.findAll();
		JSONObject ob = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		for (Role r : roles) {

			jsonArray.put(JSONPermissionHelper.createPermissionJSONObject(
					r.getName(), r.getId()));
		}
		JSONHelper.putValue(ob, "Roles", jsonArray);
		ResponseHelper.writeResponse(params, ob);
	}

}
