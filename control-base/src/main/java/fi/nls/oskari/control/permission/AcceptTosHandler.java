package fi.nls.oskari.control.permission;

import java.util.Date;

import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.user.MybatisUserService;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("AcceptTos")
public class AcceptTosHandler extends ActionHandler {
    private static final MybatisUserService userService = new MybatisUserService();

    @Override
    public void handleAction(ActionParameters params) throws ActionException {

        User user = params.getUser();

        if (user.isGuest()) {
            throw new ActionException("Not available for guest");
        }

        user.setTosAccepted(new Date());
        userService.updateUser(user);

        JSONObject ob = new JSONObject();
        JSONHelper.putValue(ob, "status", "ok");
        JSONHelper.putValue(ob, "tosAccepted", user.getTosAccepted());
        ResponseHelper.writeResponse(params, ob);
    }

}
