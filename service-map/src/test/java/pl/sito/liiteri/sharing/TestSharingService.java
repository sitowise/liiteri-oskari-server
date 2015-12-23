package pl.sito.liiteri.sharing;

import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import pl.sito.liiteri.sharing.SharingItem.CredentialType;
import pl.sito.liiteri.sharing.SharingItem.ResourceType;

import com.vividsolutions.jts.util.Assert;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.domain.workspaces.WorkSpaceSharing;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {SharingService.class})
public class TestSharingService
{
	private ISharingService service;	
	
	private INotificationService _notificationService;
	private ISharingDatabaseService _databaseService;
	private INotificationFactory _notificationFactory;
	
    @Before
    public void setUp() throws Exception {        
    	_databaseService = mock(ISharingDatabaseService.class);
        _notificationService = mock(INotificationService.class);
        _notificationFactory = mock(INotificationFactory.class);   
        service = new SharingService(_databaseService, _notificationService, _notificationFactory);
    }
    
  @Test
  public void test_InviteToSharing() throws Exception {
  	final SharingItem item = getTestItem();    
  	final NotificationItem notificationItem = new NotificationItem();
  	
  	doReturn(notificationItem).when(_notificationFactory).Create(item);
  	
  	service.InviteToSharing(item);  	
  	
  	Assert.equals(SharingItem.Status.PENDING, item.getStatus());
  		
  	verify(_notificationFactory).Create(item);
  	verify(_notificationService).SendNotification(notificationItem);  	
  	verify(_databaseService).Save(item);  
  }
  
  @Test
  public void test_Share() throws Exception {
  	final SharingItem item = getTestItem();
  	final User user = getTestUser();     
  	
  	doReturn(item).when(_databaseService).Get(item.getPermissionId());
  	
  	SharingItem result = service.ShareToUser(item.getPermissionId(), item.getToken(), user);
  	
  	Assert.equals(SharingItem.Status.SHARED, result.getStatus());
  	Assert.equals(SharingItem.CredentialType.USER, result.getCredentialType());
  	Assert.equals(user.getId(), result.getCredentialId());
  		
  	verify(_databaseService).Save(item);
  	verify(_notificationService, never()).SendNotification(any(NotificationItem.class));
  }
  
  @Test(expected = ServiceException.class)
  public void test_Share_IncorrectToken() throws Exception {
  	final SharingItem item = getTestItem();
  	final User user = getTestUser();     
  	
  	doReturn(item).when(_databaseService).Get(item.getPermissionId());
  	
  	SharingItem result = service.ShareToUser(item.getPermissionId(), item.getToken() + "aaa", user);
  	
  	fail("Shouldn't go that far");  
  }
  
  @Test
  public void test_CancelSharing() throws Exception {
  	final SharingItem item = getTestItem();     
  	
  	doReturn(item).when(_databaseService).Get(item.getPermissionId());
  	
  	SharingItem result = service.CancelSharing(item.getPermissionId());  	  	
  	Assert.equals(SharingItem.Status.CANCELED, result.getStatus());
  		
  	verify(_databaseService).Save(item);
  	verify(_notificationService, never()).SendNotification(any(NotificationItem.class));
  }
  
  @Test
  public void test_GetSharings() throws Exception {
	  final SharingItem item1 = getTestItem();
	  final SharingItem item2 = getTestItem();
	  item2.setResourceId(item1.getResourceId());
	  final User user = getTestUser(); 
	  final List<SharingItem> resultItems = new Vector<SharingItem>();
	  resultItems.add(item1);
	  resultItems.add(item2);
	  
	  doReturn(item2).when(_databaseService).Get(item2.getPermissionId());
	  doReturn(new NotificationItem()).when(_notificationFactory).Create(item1);
	  doReturn(new NotificationItem()).when(_notificationFactory).Create(item2);
	  doReturn(resultItems).when(_databaseService).GetAll(item1.getResourceType(), item1.getResourceId());
	  	
	  service.InviteToSharing(item1);
	  service.InviteToSharing(item2);
	  service.ShareToUser(item2.getPermissionId(), item2.getToken(), user);
	  
	  List<SharingItem> items = service.GetSharings(item1.getResourceType(), item1.getResourceId());
  	
	  Assert.isTrue(items != null);
	  Assert.equals(2, items.size());
	  SharingItem item1Db = null, item2Db = null;
	  for (SharingItem sharingItem : items)
	  {
		  if (item1Db == null && sharingItem.getPermissionId() == item1.getPermissionId())
			  item1Db = sharingItem;
		  if (item2Db == null && sharingItem.getPermissionId() == item2.getPermissionId())
			  item2Db = sharingItem;
	  }
	  
	  Assert.isTrue(item1Db != null);
	  Assert.isTrue(item2Db != null);
  		
  	  verify(_databaseService).GetAll(item1.getResourceType(), item1.getResourceId());
  }
  
  @Test
  public void test_GetSharingsForUser() throws Exception {
	  final User user = getTestUser(); 
	  final SharingItem item = getTestItem();	  
	  	  	  
	  List<SharingItem> result = service.GetSharingsForUser(item.getResourceType(), user);
	  
	  verify(_databaseService).GetAllForCredential(item.getResourceType(), CredentialType.USER, user.getId());
	  for (Role role : user.getRoles())
	  {
		  verify(_databaseService).GetAllForCredential(item.getResourceType(), CredentialType.ROLE, role.getId());
	  }
  }
  
  @Test
  public void test_DeleteSharings() throws Exception {
	  final SharingItem item = getTestItem();	  
	  	  	  
	  service.DeleteSharings(item.getResourceType(), item.getResourceId());
	  
	  verify(_databaseService).DeleteAll(item.getResourceType(), item.getResourceId());
  }
  
  @Test
  public void test_DeleteSharingsForUser() throws Exception {
	  final User user = getTestUser(); 
	  final SharingItem item = getTestItem();	  
	  	  	  
	  service.DeleteSharingsForUser(item.getResourceType(), item.getResourceId(), user);
	  
	  verify(_databaseService).DeleteAllForUser(item.getResourceType(), item.getResourceId(), user.getId());
  }
  
  private User getTestUser() {
		final User user = new User();
		user.setId(22l);
	    user.addRole(1, "test role");  
	    return user;
  }
  
  private SharingItem getTestItem() {
	  final SharingItem item = new SharingItem();	  
	  item.setResourceType(ResourceType.LAYER);
	  item.setResourceId(new Random().nextLong());
	  item.setToken("abc");
	  return item;
  }
    
}
