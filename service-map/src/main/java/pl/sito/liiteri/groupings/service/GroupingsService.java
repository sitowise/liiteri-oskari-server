package pl.sito.liiteri.groupings.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import pl.sito.liiteri.groupings.domain.UserTheme;
import pl.sito.liiteri.sharing.ISharingService;
import pl.sito.liiteri.sharing.SharingItem;
import pl.sito.liiteri.sharing.SharingService;
import pl.sito.liiteri.sharing.SharingItem.CredentialType;
import pl.sito.liiteri.sharing.SharingItem.ResourceType;
import pl.sito.liiteri.sharing.SharingItem.Status;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.groupings.Grouping;
import fi.nls.oskari.domain.groupings.GroupingPermission;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.domain.groupings.GroupingThemeData;
import fi.nls.oskari.groupings.db.GroupingDbService;
import fi.nls.oskari.groupings.db.GroupingServiceIbatisImpl;
import fi.nls.oskari.groupings.db.GroupingThemeDataServiceIbatisImpl;
import fi.nls.oskari.groupings.db.GroupingThemeDbService;
import fi.nls.oskari.groupings.db.GroupingThemeServiceIbatisImpl;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;

public class GroupingsService
{
    private static class GroupingsServiceHolder {
        static final GroupingsService INSTANCE = new GroupingsService();
    }
	
	public static GroupingsService getInstance() {
		return GroupingsServiceHolder.INSTANCE;
	}
	
	private static final Logger log = LogFactory.getLogger(GroupingsService.class);
	
	private GroupingDbService groupingService = new GroupingServiceIbatisImpl();
	private GroupingThemeDbService groupingThemesService = new GroupingThemeServiceIbatisImpl();
	private GroupingThemeDataServiceIbatisImpl groupingThemeDataService = new GroupingThemeDataServiceIbatisImpl();
	private ISharingService _sharingService;
	private UserService _userService;
	
	public GroupingsService(GroupingDbService groupingService, GroupingThemeDbService groupingThemesService, GroupingThemeDataServiceIbatisImpl groupingThemeDataService,
			ISharingService sharingService, UserService userService) {
		this.groupingService = groupingService;
		this.groupingThemesService = groupingThemesService;
		this.groupingThemeDataService = groupingThemeDataService;
		this._sharingService = sharingService;		
		this._userService = userService;
	}
	
	private GroupingsService() {
		this(new GroupingServiceIbatisImpl(),new GroupingThemeServiceIbatisImpl(),new GroupingThemeDataServiceIbatisImpl(),SharingService.getInstance(), null);
		
        try {
        	this._userService = UserService.getInstance();
        } catch (ServiceException e) {
        	log.error(e);
        } 
	}
	
	public long addGroupingTheme(GroupingTheme groupingTheme, User user) throws ServiceException {
		long id = groupingService.insertUnbindedMainTheme(groupingTheme);
		
		for (GroupingPermission sharing : groupingTheme.getPermissions())
		{
			SharingItem item = Map(sharing, user, id);			
			_sharingService.InviteToSharing(item);
		}
		
		return id;
	}
	
	public void updateGroupingTheme(GroupingTheme groupingTheme, User user) throws ServiceException {
		groupingService.updateUnbindedMainTheme(groupingTheme);
		
		List<SharingItem> oldSharings = _sharingService.GetSharings(ResourceType.THEME, groupingTheme.getId());
		HashMap<Long, SharingItem> oldSharingsMap = new HashMap<Long, SharingItem>();
		for (SharingItem sharingItem : oldSharings)
		{
			if (sharingItem.getStatus() == Status.CANCELED || sharingItem.getCredentialType() != CredentialType.USER)
				continue;			
			oldSharingsMap.put(sharingItem.getPermissionId(), sharingItem);
		}
				
		for (GroupingPermission sharing : groupingTheme.getPermissions())
		{
			if (sharing.getId() == 0) {
				SharingItem item = Map(sharing, user, groupingTheme.getId());
				_sharingService.InviteToSharing(item);
			} else {
				oldSharingsMap.remove(sharing.getId());
			}					
		}
		
		for (Long sharingId : oldSharingsMap.keySet())
		{
			_sharingService.CancelSharing(sharingId);
		}	
	}
	
	public List<GroupingTheme> getGroupingThemesForUser(User user) throws ServiceException {
		List<SharingItem> servicePackageItems = _sharingService.GetSharingsForUser(ResourceType.SERVICE_PACKAGE, user);
		List<SharingItem> themeItems = _sharingService.GetSharingsForUser(ResourceType.THEME, user);
		List<Long> servicePackageIds = new Vector<Long>();
		List<Long> themeIds = new Vector<Long>();
		for (SharingItem item : servicePackageItems) {
			if (item.getStatus() == Status.CANCELED)
				continue;
			
			servicePackageIds.add(item.getResourceId());
		}
		for (SharingItem item : themeItems) {
			if (item.getStatus() == Status.CANCELED)
				continue;
			
			themeIds.add(item.getResourceId());
		}	
				
		
		return groupingThemesService.findByIds(servicePackageIds, themeIds, true);	
	}
	
	public void deleteGroupingTheme(long id) throws ServiceException {
		groupingService.deleteUnbindedeMainTheme(id);
		_sharingService.DeleteSharings(ResourceType.THEME, id);
	}
	

	
	public long addServicePackage(Grouping grouping, User user) throws ServiceException {
		long id = groupingService.insertGrouping(grouping);
		
		for (GroupingPermission sharing : grouping.getPermissions())
		{
			SharingItem item = Map(sharing, user, id);			
			if (item.getCredentialType() == CredentialType.ROLE) {
				_sharingService.ShareItem(item);
			}
			else {
				_sharingService.InviteToSharing(item);	
			}			
		}
		
		return id;
	}
	
	public void updateServicePackage(Grouping servicePackage, User user) throws ServiceException {
		groupingService.updateGrouping(servicePackage);
		
		List<SharingItem> oldSharings = _sharingService.GetSharings(ResourceType.SERVICE_PACKAGE, servicePackage.getId());
		HashMap<Long, SharingItem> oldSharingsUsersMap = new HashMap<Long, SharingItem>();
		HashMap<Long, SharingItem> oldSharingsRolesMap = new HashMap<Long, SharingItem>();
		for (SharingItem sharingItem : oldSharings)
		{
			if (sharingItem.getStatus() == Status.CANCELED)
				continue;			
			if (sharingItem.getCredentialType() == CredentialType.USER)
				oldSharingsUsersMap.put(sharingItem.getPermissionId(), sharingItem);
			if (sharingItem.getCredentialType() == CredentialType.ROLE)
				oldSharingsRolesMap.put(sharingItem.getPermissionId(), sharingItem);
		}
				
		for (GroupingPermission sharing : servicePackage.getPermissions())
		{
			if (sharing.getId() == 0) {
				SharingItem item = Map(sharing, user, servicePackage.getId());
				if (item.getCredentialType() == CredentialType.ROLE) {
					_sharingService.ShareItem(item);
				}
				else {
					_sharingService.InviteToSharing(item);	
				}	
			} else {
				
				if (sharing.getExternalType().equals("USER"))
					oldSharingsUsersMap.remove(sharing.getId());
				else if (sharing.getExternalType().equals("ROLE"))
					oldSharingsRolesMap.remove(sharing.getId());
			}					
		}
		
		for (Long sharingId : oldSharingsUsersMap.keySet())
		{
			_sharingService.CancelSharing(sharingId);
		}	
		for (Long sharingId : oldSharingsRolesMap.keySet())
		{
			_sharingService.CancelSharing(sharingId);
		}	
	}
	
	public List<Grouping> getServicePackagesForUser(User user) throws ServiceException {
		List<SharingItem> items = _sharingService.GetSharingsForUser(ResourceType.SERVICE_PACKAGE, user);
		List<Long> ids = new Vector<Long>();
		for (SharingItem item : items) {
			if (item.getStatus() == Status.CANCELED)
				continue;
			
			ids.add(item.getResourceId());
		}		
				
		return ids.size() > 0 ? groupingService.findByIds(ids) : new Vector<Grouping>();
	}
	
	public void deleteServicePackage(long id) throws ServiceException {
		groupingService.deleteGrouping(id);
		_sharingService.DeleteSharings(ResourceType.SERVICE_PACKAGE, id);
	}
	
	public List<GroupingPermission> getAllUserPermissions() throws ServiceException {
		List<GroupingPermission> result = new Vector<GroupingPermission>();
		List<SharingItem> items = _sharingService.GetSharingsByCredentialType(ResourceType.THEME, CredentialType.USER);
		items.addAll(_sharingService.GetSharingsByCredentialType(ResourceType.SERVICE_PACKAGE, CredentialType.USER));
		List<User> users = _userService.getUsers();
		HashMap<Long, String> nameMap = new HashMap<Long, String>();
		for (User user : users)
		{
			nameMap.put(user.getId(), user.getScreenname());
		}
		
		for (SharingItem item : items)
		{
			if (item.getStatus().equals(Status.CANCELED))
				continue;		
			GroupingPermission permission = Map(item);
			if (nameMap.containsKey(item.getCredentialId()))
				permission.setName(nameMap.get(item.getCredentialId()));			
			result.add(permission);
		}
		
		return result;
	}
	
	public List<GroupingPermission> getAllRolePermissions() throws ServiceException {
		List<GroupingPermission> result = new Vector<GroupingPermission>();
		List<SharingItem> items = _sharingService.GetSharingsByCredentialType(ResourceType.THEME, CredentialType.ROLE);
		items.addAll(_sharingService.GetSharingsByCredentialType(ResourceType.SERVICE_PACKAGE, CredentialType.ROLE));
		Role[] roles = _userService.getRoles();
		HashMap<Long, String> nameMap = new HashMap<Long, String>();
		for (Role role : roles)
		{
			nameMap.put(role.getId(), role.getName());
		}
		
		for (SharingItem item : items)
		{
			if (item.getStatus().equals(Status.CANCELED))
				continue;	
			GroupingPermission permission = Map(item);
			if (nameMap.containsKey(item.getCredentialId()))
				permission.setName(nameMap.get(item.getCredentialId()));			
			result.add(permission);
		}
		
		return result;
	}
	
	public List<UserTheme> GetUserThemes(User user) 
	{
		ArrayList<UserTheme> result = new ArrayList<UserTheme>();
		List<GroupingTheme> themes = new ArrayList<GroupingTheme>();
		
		try
		{
			themes = getGroupingThemesForUser(user);			
		} catch (ServiceException e)
		{
			log.error(e, "Cannot get themes");
		}
		
		for (GroupingTheme groupingTheme : themes)
		{
			if (groupingTheme.getThemeType() != 0)
				continue;
			
			UserTheme theme = new UserTheme();
			theme.setId(groupingTheme.getId());
			theme.setName(groupingTheme.getName());
			
			result.add(theme);
		}	
		
		return result;
	}
	
	public List<UserTheme> GetUserThemesWithData(User user) 
	{		
		List<UserTheme> themes = GetUserThemes(user);
		
		for (UserTheme userTheme : themes)
		{
			try
			{
				List<GroupingThemeData> data = groupingThemeDataService.getDataForThemeId(userTheme.getId());
				for (GroupingThemeData groupingThemeData : data)
				{
					//layer
					if (groupingThemeData.getDataType() == 0) {
						long layerId = groupingThemeData.getDataId();
						userTheme.getLayerIds().add(layerId);
					}
				}
			} catch (ServiceException e)
			{
				log.error(e, "Cannot get theme data");
			}
		}
		
		return themes;
	}	
	
	public void AddLayerToUserTheme(long layerId, long userThemeId) {
		
		GroupingThemeData data = new GroupingThemeData();
		data.setDataId(layerId);
		data.setOskariGroupingThemeId(userThemeId);
		data.setDataType(0);
		data.setName("Added by adminlayer");
		
		groupingThemeDataService.insert(data);
	}
	
	private GroupingPermission Map(SharingItem item) {
		GroupingPermission sharingItem = new GroupingPermission();			
		sharingItem.setEmail(item.getEmail());
		sharingItem.setId(item.getPermissionId());
		sharingItem.setOskariGroupingId(item.getResourceId());
		sharingItem.setExternalId(item.getCredentialId());
		sharingItem.setExternalType(item.getCredentialType() != null ? item.getCredentialType().toString() : "USER");
		sharingItem.setTheme(item.getResourceType() == ResourceType.THEME);
		return sharingItem;
	}
	
	private SharingItem Map(GroupingPermission sharing, User user, long id) {
		SharingItem item = new SharingItem();
		item.setSender(user);
		item.setEmail(sharing.getEmail());
		item.setResourceType(sharing.isTheme() ? ResourceType.THEME : ResourceType.SERVICE_PACKAGE);
		item.setResourceId(id);		
		item.setPermissionId(sharing.getId());			
		item.setCredentialId(sharing.getExternalId());
		if (sharing.getExternalType().equals("USER"))
			item.setCredentialType(CredentialType.USER);
		else if (sharing.getExternalType().equals("ROLE"))
			item.setCredentialType(CredentialType.ROLE);
		else
			item.setCredentialType(CredentialType.USER);
		
		return item;
	}
}
