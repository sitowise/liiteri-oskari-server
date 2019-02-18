package fi.nls.oskari.map.userowndata;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.UserGisDataSharing;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.PropertyUtil;
import pl.sito.liiteri.sharing.ISharingService;
import pl.sito.liiteri.sharing.SharingItem;
import pl.sito.liiteri.sharing.SharingItem.ResourceType;
import pl.sito.liiteri.sharing.SharingItem.Status;
import pl.sito.liiteri.sharing.SharingService;

public class GisDataService
{	
    private static class GisDataServiceeHolder {
        static final GisDataService INSTANCE = new GisDataService();
    }
	
	public static GisDataService getInstance() {
		return GisDataServiceeHolder.INSTANCE;
	}
	
	private static final Logger log = LogFactory.getLogger(GisDataService.class);
	
	private int expirationTime;	
	private static final String EXPIRATION_TIME = "userGisData.expirationTimeInDays";
	
	private final GisDataDbService _domainService;
	private final GisDataRoleSettingsDbService _roleService;
	private final ISharingService _sharingService;
	
	public GisDataService(GisDataDbService domainService, GisDataRoleSettingsDbService roleService, ISharingService sharingService) {
		this._domainService = domainService;
		this._roleService = roleService;
		this._sharingService = sharingService;
		
		expirationTime = Integer.parseInt(PropertyUtil.get(EXPIRATION_TIME, "60"));
	}
	
	public GisDataService() {
		this(new GisDataDbServiceImpl(), new GisDataRoleSettingsDbServiceImpl(), SharingService.getInstance());
	}
	
    public long insertUserGisData(User user, String dataId, String dataType, List<UserGisDataSharing> sharingList) throws ServiceException {
    	
    	UserGisData data = createDomainObject(0, user, dataId, dataType, sharingList);	    	
    	int id = _domainService.insert(data);	
    	
		for (UserGisDataSharing sharing : data.getUserGisDataSharing()) {						
			SharingItem item = Map(sharing, user, id);
			_sharingService.InviteToSharing(item);
		}	    	    					
		return id;
    }
    
    public void updateUserGisData(long id, User user, String dataId, String dataType, List<UserGisDataSharing> sharingList) throws ServiceException {
		if (id == 0)
			throw new ServiceException("Empty user gis data id");    	
    	UserGisData data = createDomainObject(id, user, dataId, dataType, sharingList);
    	
    	UserGisData oldData = this.getUserGisData(id);
		HashMap<Long, UserGisDataSharing> oldSharingMap = new HashMap<Long, UserGisDataSharing>();
		for (UserGisDataSharing sharing : oldData.getUserGisDataSharing())
			oldSharingMap.put(sharing.getId(), sharing);
		
		_domainService.update(data);
		
		for (UserGisDataSharing sharing : data.getUserGisDataSharing()) {		
			if (sharing.getId() == 0) {
				SharingItem item = Map(sharing, user, data.getId());
				_sharingService.InviteToSharing(item);
			} else {
				oldSharingMap.remove(sharing.getId());
			}						
		}
		
		for (Long sharingId : oldSharingMap.keySet())
		{
			_sharingService.CancelSharing(sharingId);
		}		
    }
    
	public UserGisData getUserGisData(long id) {	
		return getUserGisData(id, true);						
	}
	
	public UserGisData getUserGisData(long id, boolean includeSharings) {
		UserGisData result = _domainService.find((int)id);
		if (includeSharings) {
			List<SharingItem> items = _sharingService.GetSharings(ResourceType.LAYER, id);		
			List<UserGisDataSharing> sharing = new Vector<UserGisDataSharing>();		
			for (SharingItem item : items)
			{
				if (item.getStatus().equals(Status.CANCELED))
					continue;
				
				UserGisDataSharing sharingItem = Map(item);					
				sharing.add(sharingItem);
			}		
			result.setUserGisDataSharing(sharing);
		}
		return result;
	}
    
	public boolean canUserAddNewDataset(User user) throws ServiceException {
		return _roleService.canUserAddNewDataset(user);
	}
	
	public boolean canUserAddNewDataset(User user, float fileSize) throws ServiceException {
		return _roleService.canUserAddNewDataset(user, fileSize);
	}
	
	public List<UserGisData> getExternalGisDataForUser(User user, Date expirationDate) {
		Vector<UserGisData> result = new Vector<UserGisData>();
		
		List<SharingItem> items = _sharingService.GetSharingsForUser(ResourceType.LAYER, user);	
		
		for (SharingItem item : items)
		{
			if (item.getStatus().equals(Status.CANCELED))
				continue;
			
			UserGisData data = this.getUserGisData(item.getResourceId(), false);
			if (expirationDate.before(data.getExpirationDate())) {
				result.add(data);
			}				
		}					
		
		return result;
	}
	
	private UserGisData createDomainObject(long id, User user, String dataId, String dataType, List<UserGisDataSharing> sharingList) {
		Date dtExpDate;
		Calendar dtExpCalendar = Calendar.getInstance();
		dtExpCalendar.add(Calendar.DAY_OF_YEAR, expirationTime);
		dtExpDate = dtExpCalendar.getTime();
		
		UserGisData userGisData = new UserGisData();
		userGisData.setId(id);
		userGisData.setDataId(dataId);
		userGisData.setDataType(dataType);
		userGisData.setExpirationDate(dtExpDate);
		userGisData.setUserId(user.getId());
		userGisData.setStatus("SAVED");
		if (sharingList != null)
			userGisData.setUserGisDataSharing(sharingList);
		
		return userGisData;
	}   
	
	private UserGisDataSharing Map(SharingItem item) {
		UserGisDataSharing sharingItem = new UserGisDataSharing();			
		sharingItem.setEmail(item.getEmail());
		sharingItem.setId(item.getPermissionId());
		sharingItem.setDatasetId(item.getResourceId());
		sharingItem.setExternalId(item.getCredentialId());
		sharingItem.setExternalType(item.getCredentialType() != null ? item.getCredentialType().toString() : "USER");
		return sharingItem;
	}
	
	private SharingItem Map(UserGisDataSharing sharing, User user, long id) {
		SharingItem item = new SharingItem();
		item.setSender(user);
		item.setEmail(sharing.getEmail());
		item.setResourceType(ResourceType.LAYER);
		item.setResourceId(id);
		return item;
	}
}
