package pl.sito.liiteri.sharing;

import java.util.List;

import pl.sito.liiteri.sharing.SharingItem.CredentialType;
import pl.sito.liiteri.sharing.SharingItem.ResourceType;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.service.ServiceException;

public interface ISharingService
{
	public SharingItem InviteToSharing(SharingItem item);
	public SharingItem CancelSharing(long id) throws ServiceException;
	public SharingItem ShareItem(SharingItem item) throws ServiceException;
	public SharingItem ShareToUser(long id, String token, User user) throws ServiceException;
	
	public List<SharingItem> GetSharings(SharingItem.ResourceType resourceType, long resourceId);
	public List<SharingItem> GetSharingsForUser(SharingItem.ResourceType resourceType, User user);
	public List<SharingItem> GetSharingsByCredentialType(SharingItem.ResourceType resourceType, CredentialType credentialType);
	
	public void DeleteSharings(ResourceType resourceType, long resourceId);
	public void DeleteSharingsForUser(ResourceType resourceType, long resourceId, User user);
}
