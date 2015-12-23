package pl.sito.liiteri.sharing;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import pl.sito.liiteri.sharing.SharingItem.CredentialType;
import pl.sito.liiteri.sharing.SharingItem.ResourceType;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;

public class SharingService implements ISharingService
{
    private static class SharingServiceHolder {
        static final SharingService INSTANCE = new SharingService();
    }
	
	public static SharingService getInstance() {
		return SharingServiceHolder.INSTANCE;
	}
	
	private static final Logger log = LogFactory.getLogger(SharingService.class);
	
	private final SecureRandom random = new SecureRandom();
	
	private final INotificationService _notificationService;
	private final INotificationFactory _notificationFactory;
	private final ISharingDatabaseService _sharingDatabaseService;
	
	private SharingService() {
		this(new SharingDatabaseService(), new EmailNotificationService(), new NotificationFactory());
	}
	
	public SharingService(ISharingDatabaseService sharingDatabaseService, INotificationService notificationService, INotificationFactory notificationFactory) {
		_sharingDatabaseService = sharingDatabaseService;
		_notificationService = notificationService;
		_notificationFactory = notificationFactory;
	}
	
	@Override
	public SharingItem InviteToSharing(SharingItem item) {		
		log.info(String.format("InviteToSharing [%s]", item));
		
		item.setStatus(SharingItem.Status.PENDING);
		item.setToken(generateToken());
		long permissionId = _sharingDatabaseService.Save(item);
		item.setPermissionId(permissionId);		
		NotificationItem notificationItem = _notificationFactory.Create(item);
		_notificationService.SendNotification(notificationItem);
		
		return item;
	}
	
	@Override
	public SharingItem ShareItem(SharingItem item) {
		log.info(String.format("ShareItem [%s]", item));
		
		item.setStatus(SharingItem.Status.SHARED);
		_sharingDatabaseService.Save(item);
		return item;
	}
	
	@Override
	public SharingItem CancelSharing(long id) throws ServiceException {
		log.info(String.format("CancelSharing [%s]", id));
		SharingItem item = _sharingDatabaseService.Get(id);
		if (item == null)
			throw new ServiceException(String.format("Cannot find sharing with id [%s]", id));
		if (item.getStatus() == SharingItem.Status.CANCELED)
			throw new ServiceException(String.format("Sharing with id [%s] has been already cancelled", id));
		
		item.setStatus(SharingItem.Status.CANCELED);
		_sharingDatabaseService.Save(item);
		
		return item;
	}	
	
	@Override
	public SharingItem ShareToUser(long id, String token, User user) throws ServiceException {		
		log.info(String.format("ShareToUser [%s], [%s]", id, user));
		
		SharingItem item = _sharingDatabaseService.Get(id);
		if (item == null)
			throw new ServiceException(String.format("Cannot find sharing with id [%s]", id));
		if (item.getStatus() == SharingItem.Status.CANCELED)
			throw new ServiceException(String.format("Sharing with id [%s] has been cancelled", id));
		if (item.getStatus() == SharingItem.Status.SHARED)
			throw new ServiceException(String.format("Sharing with id [%s] is already shared", id));
		if (!item.getToken().equals(token))
			throw new ServiceException("Incorrect sharing token");
		
		item.setStatus(SharingItem.Status.SHARED);
		item.setCredentialId(user.getId());
		item.setCredentialType(CredentialType.USER);
		_sharingDatabaseService.Save(item);
		
		return item;
	}

	@Override
	public List<SharingItem> GetSharings(ResourceType resourceType,
			long resourceId)
	{
		return _sharingDatabaseService.GetAll(resourceType, resourceId);
	}
	
	@Override
	public List<SharingItem> GetSharingsForUser(ResourceType resourceType, User user)
	{
		HashMap<Long, SharingItem> map = new HashMap<Long, SharingItem>();
		List<SharingItem> items = _sharingDatabaseService.GetAllForCredential(resourceType, CredentialType.USER, user.getId());
		for (SharingItem item : items)
		{
			map.put(item.getPermissionId(), item);
		}
		for (Role role : user.getRoles())
		{
			items = _sharingDatabaseService.GetAllForCredential(resourceType, CredentialType.ROLE, role.getId());	
			for (SharingItem item : items)
			{
				map.put(item.getPermissionId(), item);
			}
		}
		
		return new Vector<SharingItem>(map.values());
	}
	
	@Override
	public List<SharingItem> GetSharingsByCredentialType(SharingItem.ResourceType resourceType, CredentialType credentialType) {
		return _sharingDatabaseService.GetAllByCredentialType(resourceType, credentialType);
	}
	
	@Override
	public void DeleteSharings(ResourceType resourceType, long resourceId)
	{
		_sharingDatabaseService.DeleteAll(resourceType, resourceId);
		
	}

	@Override
	public void DeleteSharingsForUser(ResourceType resourceType, long resourceId, User user)
	{
		_sharingDatabaseService.DeleteAllForUser(resourceType, resourceId, user.getId());		
	}
	
	
	
	private String generateToken() {
		return new BigInteger(230, random).toString(32);
	}
}
