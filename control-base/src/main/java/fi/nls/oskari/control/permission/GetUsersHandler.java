package fi.nls.oskari.control.permission;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.user.MybatisUserService;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetUsers")
public class GetUsersHandler extends ActionHandler {
	   private static final MybatisUserService userService = new MybatisUserService();
	@Override
	public void handleAction(ActionParameters params) throws ActionException {


		List<User> users = userService.findAll();
		JSONObject ob = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		for(User u:users)
		{

			jsonArray.put(JSONPermissionHelper.createPermissionJSONObject(
					u.getScreenname(), u.getId()));
		}
		JSONHelper.putValue(ob, "Users",jsonArray );
		ResponseHelper.writeResponse(params,ob);
		
	}
}
