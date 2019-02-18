package fi.nls.oskari.workspaces.service;

import java.util.List;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.verification.VerificationMode;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import pl.sito.liiteri.sharing.ISharingService;
import pl.sito.liiteri.sharing.SharingItem;
import pl.sito.liiteri.sharing.SharingItem.CredentialType;
import pl.sito.liiteri.sharing.SharingItem.ResourceType;
import pl.sito.liiteri.sharing.SharingItem.Status;

import com.vividsolutions.jts.util.Assert;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.argThat;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.domain.workspaces.WorkSpaceSharing;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {WorkspaceService.class})
public class TestWorkspaceService
{
	private WorkspaceService service;
	
	private WorkSpaceDbService _domainService;
	private WorkSpaceRoleSettingsDbService _roleService;
	private UserService _userService;
	private ISharingService _sharingService;
	
    @Before
    public void setUp() throws Exception {        
        _domainService = mock(WorkSpaceDbService.class);
        _roleService = mock(WorkSpaceRoleSettingsDbService.class);
        _userService = mock(UserService.class);
        _sharingService = mock(ISharingService.class);    
        service = new WorkspaceService(_roleService, _domainService, _userService, _sharingService);
    }
    
    @Test
    public void testCanAddWorkspace_Limit_Correct() throws Exception {
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role");  
        
    	doReturn(5).when(_roleService).getMaxWorkSpaceAmount(user.getId());
    	doReturn(4l).when(_domainService).getVisibleWorkSpacesAmount(user.getId());
    	
    	boolean result = service.canAddWorkspace(user.getId());
    	Assert.equals(true, result);
    	
    	verify(_roleService).getMaxWorkSpaceAmount(user.getId());
    	verify(_domainService).getVisibleWorkSpacesAmount(user.getId());
    }
    
    @Test
    public void testCanAddWorkspace_Limit_Incorrect() throws Exception {
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role");  
        
    	doReturn(5).when(_roleService).getMaxWorkSpaceAmount(user.getId());
    	doReturn(5l).when(_domainService).getVisibleWorkSpacesAmount(user.getId());
    	
    	boolean result = service.canAddWorkspace(user.getId());
    	Assert.equals(false, result);
    	
    	verify(_roleService).getMaxWorkSpaceAmount(user.getId());
    	verify(_domainService).getVisibleWorkSpacesAmount(user.getId());
    }
    
    @Test
    public void testCanAddWorkspace_Limit_Incorrect2() throws Exception {
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role");  
        
    	doReturn(5).when(_roleService).getMaxWorkSpaceAmount(user.getId());
    	doReturn(6l).when(_domainService).getVisibleWorkSpacesAmount(user.getId());
    	
    	boolean result = service.canAddWorkspace(user.getId());
    	Assert.equals(false, result);
    	
    	verify(_roleService).getMaxWorkSpaceAmount(user.getId());
    	verify(_domainService).getVisibleWorkSpacesAmount(user.getId());
    }
    
    @Test
    public void testAddWorkspace() throws Exception {
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role"); 
    	WorkSpace data = new WorkSpace();
    	data.setName("ws_name");
    	data.setUserId(user.getId());
    	List<WorkSpaceSharing> sharings = new Vector<WorkSpaceSharing>();
    	sharings.add(new WorkSpaceSharing());
    	sharings.add(new WorkSpaceSharing());
    	sharings.get(0).setEmail("email1@aa.pl");
    	sharings.get(1).setEmail("email2@aa.pl");
    	data.setWorkSpaceSharing(sharings);
    	
    	doReturn(1).when(_domainService).insert(data);
    	doReturn(user).when(_userService).getUser(user.getId());
    	
    	service.addWorkspace(data);    	
    	
    	verify(_domainService).insert(data);
    	verify(_userService).getUser(user.getId());
    	verify(_sharingService, times(1)).InviteToSharing(argThat(new EmailArgumentMatcher(sharings.get(0).getEmail())));
    	verify(_sharingService, times(1)).InviteToSharing(argThat(new EmailArgumentMatcher(sharings.get(1).getEmail())));
    }
    
    @Test
    public void testAddWorkspace_NoSharings() throws Exception {
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role"); 
    	WorkSpace data = new WorkSpace();
    	data.setName("ws_name");
    	data.setUserId(user.getId());
    	List<WorkSpaceSharing> sharings = new Vector<WorkSpaceSharing>();
    	data.setWorkSpaceSharing(sharings);
    	
    	doReturn(1).when(_domainService).insert(data);
    	doReturn(user).when(_userService).getUser(user.getId());
    	
    	service.addWorkspace(data);
    	
    	Assert.equals(0, data.getWorkSpaceSharing().size());
    	
    	verify(_domainService).insert(data);
    	verify(_sharingService, never()).InviteToSharing((any(SharingItem.class)));
    	verify(_userService).getUser(user.getId());
    	
    }
    
    @Test(expected = ServiceException.class)
    public void testUpdateWorkspace_EmptyId() throws Exception {
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role"); 
    	WorkSpace data = new WorkSpace();
    	data.setId(0);
    	data.setName("ws_name");
    	data.setUserId(user.getId());
    	List<WorkSpaceSharing> sharings = new Vector<WorkSpaceSharing>();
    	data.setWorkSpaceSharing(sharings);    	
    	
    	service.updateWorkspace(data);
    	
    	fail("Shouldn't go that far");
    }
    
    @Test
    public void testUpdateWorkspace() throws Exception {
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role"); 
    	WorkSpace data = new WorkSpace();
    	data.setId(222l);
    	data.setName("ws_name");
    	data.setUserId(user.getId());
    	List<WorkSpaceSharing> sharings = new Vector<WorkSpaceSharing>();
    	sharings.add(new WorkSpaceSharing());
    	sharings.add(new WorkSpaceSharing());
    	sharings.get(0).setEmail("email1_TO_BE_INSERTED@aa.pl");
    	sharings.get(1).setEmail("email2_TO_BE_UPDATED@aa.pl");
    	sharings.get(1).setId(22l);
    	data.setWorkSpaceSharing(sharings);
    	
    	WorkSpace oldData = new WorkSpace();
    	oldData.setId(222l);
    	oldData.setName("ws_name");
    	oldData.setUserId(user.getId());
    	List<SharingItem> oldSharings = new Vector<SharingItem>();
    	oldSharings.add(new SharingItem());
    	oldSharings.add(new SharingItem());
    	oldSharings.get(0).setEmail("email2_TO_BE_UPDATED@aa.pl");
    	oldSharings.get(0).setPermissionId(22l);
    	oldSharings.get(0).setCredentialId(123l);
    	oldSharings.get(0).setCredentialType(CredentialType.USER);
    	oldSharings.get(0).setStatus(Status.PENDING);
    	oldSharings.get(1).setEmail("email3_TO_BE_DELETED@aa.pl");
    	oldSharings.get(1).setPermissionId(23l);
    	oldSharings.get(1).setCredentialId(124l);
    	oldSharings.get(1).setCredentialType(CredentialType.USER);
    	oldSharings.get(1).setStatus(Status.PENDING);
    	
    	doReturn(oldData).when(_domainService).find((int)data.getId());
    	doReturn(oldSharings).when(_sharingService).GetSharings(ResourceType.WORKSPACE, data.getId());
    	doReturn(user).when(_userService).getUser(user.getId());
    	
    	service.updateWorkspace(data);    	
    	
    	verify(_domainService).find((int)data.getId());
    	verify(_sharingService).GetSharings(ResourceType.WORKSPACE, data.getId());
    	verify(_domainService).update(data);
    	
    	List<WorkSpaceSharing> newSharings = new Vector<WorkSpaceSharing>();
    	newSharings.add(sharings.get(0));
    	verify(_sharingService).InviteToSharing(argThat(new EmailArgumentMatcher(sharings.get(0).getEmail())));
    	verify(_sharingService, never()).InviteToSharing(argThat(new EmailArgumentMatcher(oldSharings.get(0).getEmail())));
    	verify(_sharingService, never()).InviteToSharing(argThat(new EmailArgumentMatcher(oldSharings.get(1).getEmail())));
    	verify(_sharingService).CancelSharing(oldSharings.get(1).getPermissionId());
    	verify(_sharingService, never()).CancelSharing(sharings.get(0).getId());
    	verify(_sharingService, never()).CancelSharing(sharings.get(1).getId());
    }
    
    class EmailArgumentMatcher extends ArgumentMatcher<SharingItem> {
    	private String _email;
    	
    	public EmailArgumentMatcher(String email) {
    		_email = email;
    	}
    	
    	public boolean matches(Object o) {
    		if (o instanceof SharingItem) {
    			SharingItem item = (SharingItem) o;
    			return _email.equals(item.getEmail());
    		}
    		return false;
    	}
    }
}
