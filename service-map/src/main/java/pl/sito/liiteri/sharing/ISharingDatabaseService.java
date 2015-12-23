package pl.sito.liiteri.sharing;

import java.util.List;

import pl.sito.liiteri.sharing.SharingItem.CredentialType;
import pl.sito.liiteri.sharing.SharingItem.ResourceType;

public interface ISharingDatabaseService
{
	public long Save(SharingItem item);	
	public SharingItem Get(long id);
	public List<SharingItem> GetAll(ResourceType resourceType, long resourceId);
	public List<SharingItem> GetAllForCredential(ResourceType resourceType, CredentialType credentialType, long credentialId);
	public List<SharingItem> GetAllByCredentialType(ResourceType resourceType, CredentialType credentialType);
	
	public void DeleteAllForUser(ResourceType resourceType, long resourceId, long userId);
	public void DeleteAll(ResourceType resourceType, long resourceId);	
}
