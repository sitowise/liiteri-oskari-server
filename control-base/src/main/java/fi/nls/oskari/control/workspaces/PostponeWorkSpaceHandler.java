package fi.nls.oskari.control.workspaces;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;
import fi.nls.oskari.workspaces.service.WorkspaceService;

@OskariActionRoute("PostponeWorkspace")
public class PostponeWorkSpaceHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(PostponeWorkSpaceHandler.class);

    private WorkspaceService _service;

    private static final String WORKSPACE_ID = "id";

    private static final String EXPIRATION_TIME_IN_DAYS = "workspaces.expirationTimeInDays";

    private static final String WORKSPACE_STATUS_SAVED = "SAVED";

    // UNIT TESTS
    public void setService(final WorkspaceService service) {
        this._service = service;
    }

    @Override
    public void init() {
        super.init();
        if (_service == null) {
            setService(WorkspaceService.getInstance());
        }
    }

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        User user = params.getUser();
        if (user.isGuest())
            throw new ActionDeniedException("User is not logged");

        Long id = Long.parseLong(params.getHttpParam(WORKSPACE_ID), 10);
        WorkSpace workspace = _service.getWorkspace(id, false);

        if (workspace == null || workspace.getUserId() != user.getId()) {
            throw new ActionDeniedException("Ei muokkausoikeutta");
        }

        Date dtExpDate;
        Calendar dtExpCalendar = Calendar.getInstance();
        dtExpCalendar.add(Calendar.DAY_OF_YEAR, Integer.parseInt(PropertyUtil.get(EXPIRATION_TIME_IN_DAYS, "60")));
        dtExpDate = dtExpCalendar.getTime();

        workspace.setExpirationDate(dtExpDate);
        workspace.setStatus(WORKSPACE_STATUS_SAVED);

        JSONObject ret = JSONWorkSpacesHelper.createJSONMessageObject(id, "success");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            ret.put("expirationDate", dateFormat.format(dtExpDate));
            _service.updateWorkspace(workspace, true);
        } catch (ServiceException | JSONException e) {
            log.debug(e, "Postpone workspace failed");
            throw new ActionException("Virhe työtilan tallennuksessa", e);
        }

        ResponseHelper.writeResponse(params, ret);
    }

}
