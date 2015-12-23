package fi.nsl.oskari.control.workspaces;

import java.util.HashMap;
import java.util.Map;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.control.workspaces.SaveWorkSpaceHandler;
import fi.nls.test.control.JSONActionRouteTest;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.workspaces.service.WorkSpaceDbService;
import fi.nls.oskari.workspaces.service.WorkSpaceRoleSettingsDbService;
import fi.nls.oskari.workspaces.service.WorkspaceService;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {SaveWorkSpaceHandler.class})
public class SaveWorkSpaceHandlerTest extends JSONActionRouteTest {

    final private  SaveWorkSpaceHandler handler = new  SaveWorkSpaceHandler();
    
    private static WorkspaceService service;

    @BeforeClass
    public static void addLocales() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    	service = mock(WorkspaceService.class);
        handler.setService(service);
    	
        handler.init();
    }
    
    @Test(expected = ActionParamsException.class)
    public void testHandleActionInvalidParams() throws Exception {
        final ActionParameters params = createActionParams();
        handler.handleAction(params);

        fail("Should not get his far without parameters");
    }

    @Test(expected = ActionDeniedException.class)
    public void testHandleActionGuestUser() throws Exception {

        // setup params
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("workspace", "{}");
        parameters.put("name", "test_workspace");

        final ActionParameters params = createActionParams(parameters);
        handler.handleAction(params);

        fail("Should not get his far with guest user");
    }

    @Test(expected = ActionException.class)
    public void testHandleActionAddNewWorkspace_ExceededLimit() throws Exception {
    	final User user = new User();
        user.addRole(1, "test role");    	
    	doReturn(false).when(service).canAddWorkspace(user.getId());
    	
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("workspace", "{}");
        parameters.put("name", "test_workspace");                
        final ActionParameters params = createActionParams(parameters, user);
        
        try {
        	handler.handleAction(params);
        	fail("Should not get his far because user has reached maximum amount of workspaces");
        }
        catch (ActionException ae) {
        	throw ae;
        }
        finally {
        	verify(service).canAddWorkspace(user.getId());
        	verify(service, never()).addWorkspace((any(WorkSpace.class)));
        }                
    }    
    
    @Test
    public void testHandleActionAddNewWorkspace_NotExceededLimit() throws Exception {
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role");    	
    	doReturn(true).when(service).canAddWorkspace(user.getId());    
    	
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("workspace", "{}");
        parameters.put("name", "test_workspace");                
        final ActionParameters params = createActionParams(parameters, user);
        handler.handleAction(params);
        
        verify(service).canAddWorkspace(user.getId());
        verify(service).addWorkspace((any(WorkSpace.class)));       
    }
    
}
