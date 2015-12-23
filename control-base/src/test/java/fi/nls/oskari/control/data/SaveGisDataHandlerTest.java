package fi.nls.oskari.control.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.test.control.JSONActionRouteTest;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.map.userowndata.GisDataService;
import fi.nls.oskari.control.data.SaveGisDataHandler;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.UserGisDataSharing;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;


@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {SaveGisDataHandler.class})
public class SaveGisDataHandlerTest extends JSONActionRouteTest {

    final private  SaveGisDataHandler handler = new SaveGisDataHandler();
    
    private static GisDataService service;

    @BeforeClass
    public static void addLocales() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    	service = mock(GisDataService.class);
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
        parameters.put("users", "[]");

        final ActionParameters params = createActionParams(parameters);
        handler.handleAction(params);

        fail("Should not get his far with guest user");
    }
    
    @Test(expected = ActionException.class)
    public void testHandleAction_InvalidParamsFormat() throws Exception {

        // setup params
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("users", "{}");

        final ActionParameters params = createActionParams(parameters);
        handler.handleAction(params);

        fail("Should not get his far with incorrect parameters");
    }

    @Test
    public void testHandleAction_Inserts_WhenIdIsEmpty() throws Exception {
    	final User user = new User();
        user.addRole(1, "test role");           
    	
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("users", "[]");           
        final ActionParameters params = createActionParams(parameters, user);
        
        handler.handleAction(params);
        verify(service).insertUserGisData(eq(user), any(String.class), any(String.class), any(List.class));                    
    }    
    
    @Test
    public void testHandleAction_Updates_WhenIdIsNotEmpty() throws Exception {
    	final User user = new User();
        user.addRole(1, "test role");           
    	
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("users", "[]");
        parameters.put("id", "12"); 
        final ActionParameters params = createActionParams(parameters, user);
        
        handler.handleAction(params);
        verify(service).updateUserGisData(eq(12l), eq(user), any(String.class), any(String.class), any(List.class));                    
    }        
}
