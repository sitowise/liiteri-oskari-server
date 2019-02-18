package fi.nls.oskari.workspaces.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import pl.sito.liiteri.sharing.ISharingService;
import pl.sito.liiteri.sharing.SharingItem;
import pl.sito.liiteri.sharing.SharingService;
import pl.sito.liiteri.sharing.SharingItem.ResourceType;
import pl.sito.liiteri.sharing.SharingItem.Status;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.workspaces.WorkSpace;
import fi.nls.oskari.domain.workspaces.WorkSpaceSharing;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.PropertyUtil;

public class WorkspaceService
{	
    private static class WorkspaceServiceHolder {
        static final WorkspaceService INSTANCE = new WorkspaceService();
    }
	
	public static WorkspaceService getInstance() {
		return WorkspaceServiceHolder.INSTANCE;
	}
	
	private static final Logger log = LogFactory.getLogger(WorkspaceService.class);
	private WorkSpaceDbService _domainService;
	private WorkSpaceRoleSettingsDbService _roleService;
	private UserService _userService;	
	private ISharingService _sharingService;
    
    public WorkspaceService(WorkSpaceRoleSettingsDbService roleService, WorkSpaceDbService domainService, 
    		UserService userService, ISharingService sharingService) {
    	this._roleService = roleService;
    	this._domainService = domainService;
    	this._userService = userService;
    	this._sharingService = sharingService;
    }
    
    private WorkspaceService() {
    	this(new WorkSpaceRoleSettingsServiceIbatisImpl(), new WorkSpaceServiceIbatisImpl(),
    			null, SharingService.getInstance());    	   
        try {
        	this._userService = UserService.getInstance();
        } catch (ServiceException e) {
        	log.error(e);
        }        	
    }
	
	public boolean canAddWorkspace(long userId) throws ServiceException {
		int allowedWorkSpacesLimit = _roleService.getMaxWorkSpaceAmount(userId);		
		long existedVisibleWorkSpacesAmount = _domainService.getVisibleWorkSpacesAmount(userId);
		
		return existedVisibleWorkSpacesAmount < allowedWorkSpacesLimit;
	}
	
	public void addWorkspace(WorkSpace data) throws ServiceException {		
		int id = _domainService.insert(data);			
		User user = _userService.getUser(data.getUserId());		
		
		for (WorkSpaceSharing sharing : data.getWorkSpaceSharing()) {						
			SharingItem item = Map(sharing, user, id);			
			_sharingService.InviteToSharing(item);
		}				
	}
	
	public void updateWorkspace(WorkSpace data, boolean ignoreSharings) throws ServiceException {
		if (data.getId() == 0)
			throw new ServiceException("Empty workspace id");
		HashMap<Long, WorkSpaceSharing> oldSharingMap = new HashMap<Long, WorkSpaceSharing>();
		User user = null;
		
		if (!ignoreSharings) {
			user = _userService.getUser(data.getUserId());			
			WorkSpace oldObject = this.getWorkspace(data.getId());			
			for (WorkSpaceSharing sharing : oldObject.getWorkSpaceSharing())
				oldSharingMap.put(sharing.getId(), sharing);	
		}				
		
		_domainService.update(data);
		
		if (!ignoreSharings) {
			for (WorkSpaceSharing sharing : data.getWorkSpaceSharing()) {			
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
	}
	
	public void updateWorkspace(WorkSpace data) throws ServiceException {
		 updateWorkspace(data, false);
	}
	
	public WorkSpace getWorkspace(long id) {
		return getWorkspace(id, true);
	}
	
	public WorkSpace getWorkspace(long id, boolean includeSharings) {		
		WorkSpace result = _domainService.find((int)id);
		if (includeSharings) {
			List<SharingItem> items = _sharingService.GetSharings(ResourceType.WORKSPACE, id);		
			List<WorkSpaceSharing> sharing = new Vector<WorkSpaceSharing>();		
			for (SharingItem item : items)
			{
				if (item.getStatus().equals(Status.CANCELED))
					continue;
				
				WorkSpaceSharing sharingItem = Map(item);					
				sharing.add(sharingItem);
			}		
			result.setWorkSpaceSharing(sharing);
		}
		return result;
	}
	
	public WorkSpace getHiddenWorkspace(long id) throws ServiceException {
		return _domainService.getHiddenWorkSpaceById(id);
	}
	
	public void deleteWorkspace(long id) {
		_domainService.delete((int)id);
		_sharingService.DeleteSharings(ResourceType.WORKSPACE, id);
	}
	
	public List<WorkSpace> getExternalWorkspacesForUser(User user, Date expirationDate) {
		Vector<WorkSpace> result = new Vector<WorkSpace>();
		
		List<SharingItem> items = _sharingService.GetSharingsForUser(ResourceType.WORKSPACE, user);	
		
		for (SharingItem item : items)
		{
			if (item.getStatus().equals(Status.CANCELED))
				continue;
			
			WorkSpace workspace = this.getWorkspace(item.getResourceId(), false);
			if (expirationDate.before(workspace.getExpirationDate())) {
				result.add(workspace);
			}				
		}					
		
		return result;
	}
	
	public List<WorkSpace> getWorkspacesForUser(User user, Date expirationDate) throws ServiceException {
		List<WorkSpace> result = _domainService.getUserWorkSpaces(user.getId(), expirationDate);
		for (WorkSpace workspace : result)
		{
			List<SharingItem> items = _sharingService.GetSharings(ResourceType.WORKSPACE, workspace.getId());			
			List<WorkSpaceSharing> sharingList = Map(items);
			workspace.setWorkSpaceSharing(sharingList);
		}		
		return result;
	}
	
	public List<WorkSpace> getVisibleWorkspacesForUser(User user, Date expirationDate) throws ServiceException {
		List<WorkSpace> result = _domainService.getUserVisibleWorkSpaces(user.getId(), expirationDate);
		for (WorkSpace workspace : result)
		{
			List<SharingItem> items = _sharingService.GetSharings(ResourceType.WORKSPACE, workspace.getId());			
			List<WorkSpaceSharing> sharingList = Map(items);
			workspace.setWorkSpaceSharing(sharingList);
		}		
		return result;
	}
	
	public List<WorkSpace> getExpiredWorkspaces(Date date) throws ServiceException {
		return _domainService.getWorkSpacesAfterExpirationDate(date, "SAVED");
	}
	
	private List<WorkSpaceSharing> Map(List<SharingItem> items) {
		List<WorkSpaceSharing> sharingList = new Vector<WorkSpaceSharing>();
		for (SharingItem item : items)
		{
			if (item.getStatus().equals(Status.CANCELED))
				continue;								
			sharingList.add(Map(item));
		}	
		return sharingList;
	}
	
	private WorkSpaceSharing Map(SharingItem item) {
		WorkSpaceSharing sharingItem = new WorkSpaceSharing();			
		sharingItem.setEmail(item.getEmail());
		sharingItem.setId(item.getPermissionId());
		sharingItem.setWorkSpaceId(item.getResourceId());
		sharingItem.setExternalId(item.getCredentialId());
		sharingItem.setExternalType(item.getCredentialType() != null ? item.getCredentialType().toString() : "USER");
		return sharingItem;
	}
	
	private SharingItem Map(WorkSpaceSharing sharing, User user, long workspaceId) {
		SharingItem item = new SharingItem();
		item.setSender(user);
		item.setEmail(sharing.getEmail());
		item.setResourceType(ResourceType.WORKSPACE);
		item.setResourceId(workspaceId);		
		return item;
	}
}
