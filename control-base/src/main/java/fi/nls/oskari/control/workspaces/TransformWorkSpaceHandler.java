package fi.nls.oskari.control.workspaces;

import java.util.Arrays;
import java.util.List;

import pl.sito.liiteri.groupings.service.GroupingsService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.groupings.Grouping;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.domain.groupings.GroupingThemeData;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.groupings.db.GroupingServiceIbatisImpl;
import fi.nls.oskari.groupings.db.GroupingThemeDataServiceIbatisImpl;
import fi.nls.oskari.groupings.db.GroupingThemeServiceIbatisImpl;
import fi.nls.oskari.groupings.utils.GroupingCollectionHelper;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.ResponseHelper;
import fi.nls.oskari.workspaces.service.WorkspaceService;

@OskariActionRoute("TransformWorkspace")
public class TransformWorkSpaceHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(TransformWorkSpaceHandler.class);
    private WorkspaceService _service;
    // private static final UserService userService = new DatabaseUserService();
    private static final String WORKSPACE_ID = "id";
    private static final GroupingsService _groupingService = GroupingsService
            .getInstance();
    private static final GroupingServiceIbatisImpl groupingServiceIbatis = new GroupingServiceIbatisImpl();
    private static final GroupingThemeServiceIbatisImpl groupingThemesService = new GroupingThemeServiceIbatisImpl();
    private static final GroupingThemeDataServiceIbatisImpl groupingThemeDataService = new GroupingThemeDataServiceIbatisImpl();

    private final static String[] AUTHORIZED_ROLES = new String[] { Role.GROUPINGS_ADMIN };

    // UNIT TESTS
    public void setService(final WorkspaceService service) {
        this._service = service;
    }

    @Override
    public void handleAction(ActionParameters params) throws ActionException {

        User user = params.getUser();
        if (!user.isAdmin() && !user.hasAnyRoleIn(AUTHORIZED_ROLES))
            throw new ActionDeniedException("Access denied!");
        Long id;
        String message = null;
        long newServicePackageId = 0;
        try {
            id = Long.parseLong(params.getHttpParam(WORKSPACE_ID));
        } catch (Exception e) {
            throw new ActionException(
                    "Error during getting workspace id paramaeter");
        }
        long userId = user.getId();
        WorkSpace ws = _service.getWorkspace(id);
        Grouping grouping = null;

        try {
            long originalGroupingId = JSONWorkSpacesHelper
                    .getServicePackageId(ws.getSettings());

            if (originalGroupingId != -1) {
                List<Grouping> groupings = groupingServiceIbatis
                        .findByIds(Arrays.asList(originalGroupingId));

                if (groupings != null && groupings.size() > 0) {
                    grouping = groupings.get(0);
                    List<GroupingTheme> themes = groupingThemesService
                            .findAll();
                    List<GroupingThemeData> data = groupingThemeDataService
                            .findAll();

                    grouping = GroupingCollectionHelper
                            .createServicePackageStructure(grouping, themes,
                                    data);
                }
            }

            grouping = JSONWorkSpacesHelper.transformWorkSpace(grouping,
                    ws.getSettings(), ws.getName(), userId);

        } catch (Exception e) {
            throw new ActionException(
                    "Error during adding service package to database");
        }
        try {
            newServicePackageId = _groupingService.addServicePackage(grouping,
                    user);
            message = "New service package has been created";
        } catch (Exception e) {
            throw new ActionException(
                    "Error during adding service package to database");
        }
        ResponseHelper.writeResponse(params, JSONWorkSpacesHelper
                .createJSONMessageObject(newServicePackageId, message));
    }

    @Override
    public void init() {
        super.init();
        if (_service == null) {
            setService(WorkspaceService.getInstance());
        }
    }

}