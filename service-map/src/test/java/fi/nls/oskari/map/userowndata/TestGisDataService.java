package fi.nls.oskari.map.userowndata;

import java.util.List;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import pl.sito.liiteri.sharing.ISharingService;
import pl.sito.liiteri.sharing.SharingItem;
import pl.sito.liiteri.sharing.SharingItem.CredentialType;
import pl.sito.liiteri.sharing.SharingItem.ResourceType;
import pl.sito.liiteri.sharing.SharingItem.Status;

import com.vividsolutions.jts.util.Assert;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.argThat;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.UserGisDataSharing;
import fi.nls.oskari.service.ServiceException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {GisDataService.class})
public class TestGisDataService
{
	private GisDataService service;
	
	private GisDataDbService _domainService;
	private GisDataRoleSettingsDbService _roleService;
	private ISharingService _sharingService;
	
    @Before
    public void setUp() throws Exception {        
        _domainService = mock(GisDataDbService.class);
        _roleService = mock(GisDataRoleSettingsDbService.class);
        _sharingService = mock(ISharingService.class);                
        service = new GisDataService(_domainService, _roleService, _sharingService);
    }
    
    @Test
    public void testAddUserGisData() throws Exception {
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role"); 
        String dataId = "123";
        String dataType = "emptytype";
    	List<UserGisDataSharing> sharings = new Vector<UserGisDataSharing>();
    	UserGisDataSharing sharing1 = new UserGisDataSharing();
    	sharing1.setEmail("email1@aa.pl");
    	UserGisDataSharing sharing2 = new UserGisDataSharing();
    	sharing2.setEmail("email2@aa.pl");
    	sharings.add(sharing1);
    	sharings.add(sharing2);
    	
    	UserGisDataMatcher matcher = new UserGisDataMatcher(user, dataId, dataType, sharings);
    	
    	doReturn(1).when(_domainService).insert(argThat(matcher));
    	
    	long result = service.insertUserGisData(user, dataId, dataType, sharings);
    	
    	Assert.equals(1l, result);
    	
    	verify(_domainService).insert(argThat(matcher));
    	verify(_sharingService, times(1)).InviteToSharing(argThat(new EmailArgumentMatcher(sharings.get(0).getEmail())));
    	verify(_sharingService, times(1)).InviteToSharing(argThat(new EmailArgumentMatcher(sharings.get(1).getEmail())));
    }

    
    @Test(expected = ServiceException.class)
    public void testUpdateUserGisData_EmptyId() throws Exception {    	
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role"); 
        String dataId = "123";
        String dataType = "emptytype";
        long id = 0;
    	List<UserGisDataSharing> sharings = new Vector<UserGisDataSharing>();
    	UserGisDataSharing sharing1 = new UserGisDataSharing();
    	sharing1.setEmail("email1@aa.pl");
    	UserGisDataSharing sharing2 = new UserGisDataSharing();
    	sharing2.setEmail("email2@aa.pl");
    	sharings.add(sharing1);
    	sharings.add(sharing2);   	
    	
    	service.updateUserGisData(id, user, dataId, dataType, sharings);
    	
    	fail("Shouldn't go that far");
    }
    
    @Test
    public void testUpdateUserGisData() throws Exception {
    	final User user = new User();
    	user.setId(22l);
        user.addRole(1, "test role"); 
        String dataId = "123";
        String dataType = "emptytype";
        long id = 13;
    	List<UserGisDataSharing> sharings = new Vector<UserGisDataSharing>();
    	UserGisDataSharing sharing1 = new UserGisDataSharing();
    	sharing1.setEmail("email1_TO_BE_INSERTED@aa.pl");
    	UserGisDataSharing sharing2 = new UserGisDataSharing();
    	sharing2.setEmail("email2_TO_BE_UPDATED@aa.pl");
    	sharing2.setId(22l);
    	sharings.add(sharing1);
    	sharings.add(sharing2);   
    	    	    	
    	UserGisData oldData = new UserGisData();
    	oldData.setId(22l);
    	oldData.setDataType(dataType);
    	oldData.setDataId(dataId);
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
    	
    	UserGisDataMatcher matcher = new UserGisDataMatcher(user, dataId, dataType, sharings);
    	
    	doReturn(oldData).when(_domainService).find((int)id);
    	doReturn(oldSharings).when(_sharingService).GetSharings(ResourceType.LAYER, id);
    	
    	service.updateUserGisData(id, user, dataId, dataType, sharings);    
    	
    	verify(_domainService).find((int)id);
    	verify(_sharingService).GetSharings(ResourceType.LAYER, id);
    	verify(_domainService).update(argThat(matcher));    
    	
    	List<UserGisDataSharing> newSharings = new Vector<UserGisDataSharing>();
    	newSharings.add(sharing1);

    	verify(_sharingService).InviteToSharing(argThat(new EmailArgumentMatcher(sharings.get(0).getEmail())));
    	verify(_sharingService, never()).InviteToSharing(argThat(new EmailArgumentMatcher(oldSharings.get(0).getEmail())));
    	verify(_sharingService, never()).InviteToSharing(argThat(new EmailArgumentMatcher(oldSharings.get(1).getEmail())));
    	verify(_sharingService).CancelSharing(oldSharings.get(1).getPermissionId());
    	verify(_sharingService, never()).CancelSharing(sharings.get(0).getId());
    	verify(_sharingService, never()).CancelSharing(sharings.get(1).getId());
    }
    
    class UserGisDataMatcher extends ArgumentMatcher<UserGisData> {
    	
    	User user; String dataId; String dataType; List<UserGisDataSharing> sharingList;
    	
    	public UserGisDataMatcher(User user, String dataId, String dataType, List<UserGisDataSharing> sharingList)
		{
    		this.user = user;
    		this.dataId = dataId;
    		this.dataType = dataType;
    		this.sharingList = sharingList;
		}
    	
        public boolean matches(Object obj) {
        	UserGisData data = (UserGisData) obj;
        	boolean result = data.getUserId() == this.user.getId() && data.getDataId().equals(dataId) && data.getDataType().equals(dataType) && data.getUserGisDataSharing().equals(sharingList);
        	
            return result;
        }
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
