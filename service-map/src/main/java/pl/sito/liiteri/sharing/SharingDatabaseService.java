package pl.sito.liiteri.sharing;

import java.sql.SQLException;
import java.util.List;

import com.ibatis.sqlmap.client.SqlMapSession;

import pl.sito.liiteri.sharing.SharingItem.CredentialType;
import pl.sito.liiteri.sharing.SharingItem.ResourceType;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.db.BaseIbatisService;

public class SharingDatabaseService extends BaseIbatisService<SharingItem> implements ISharingDatabaseService
{
	private static final Logger log = LogFactory.getLogger(SharingDatabaseService.class);	

	public SharingDatabaseService() {
		
	}
	
	@Override
	public long Save(SharingItem item)
	{			
		if (item.getPermissionId() == 0) {		
			log.info("INSERT item %s", item.toString());
			long id = (long) this.insert(item);
			return id;
		}					
		else {
			log.info("UPDATE item %s", item.toString());
			this.update(item);
			return item.getPermissionId();
		}			
	}

	@Override
	public List<SharingItem> GetAll(ResourceType resourceType, long resourceId)
	{	
		SharingItem param = new SharingItem();
		param.setResourceId(resourceId);
		param.setResourceType(resourceType);
		
		return this.queryForList(getNameSpace() + ".findByResourceId", param);	
	}
	
	@Override
	public List<SharingItem> GetAllByCredentialType(ResourceType resourceType, CredentialType credentialType) {
		SharingItem param = new SharingItem();
		param.setCredentialType(credentialType);
		param.setResourceType(resourceType);
		
		return this.queryForList(getNameSpace() + ".findByCredentialType", param);
	}
	
	@Override
	public List<SharingItem> GetAllForCredential(ResourceType resourceType, CredentialType credentialType, long credentialId) {
		SharingItem param = new SharingItem();
		param.setCredentialId(credentialId);
		param.setCredentialType(credentialType);
		param.setResourceType(resourceType);
		
		return this.queryForList(getNameSpace() + ".findByCredentialId", param);	
	}
	
	@Override
	public void DeleteAllForUser(ResourceType resourceType, long resourceId, long userId) {
		SharingItem param = new SharingItem();
		param.setCredentialId(userId);
		param.setCredentialType(CredentialType.USER);
		param.setResourceType(resourceType);
		param.setResourceId(resourceId);
		
		final SqlMapSession session = openSession();		
		try {
			session.delete(getNameSpace() + ".deleteByResourceIdAndCredenialId", param);		
		} catch (SQLException e)
		{
			throw new RuntimeException("Failed to query", e);
		} finally {
			try {
				session.close();
			} catch (Exception ignored) {
			}
		}		
	}
	
	@Override
	public void DeleteAll(ResourceType resourceType, long resourceId) {
		SharingItem param = new SharingItem();
		param.setResourceType(resourceType);
		param.setResourceId(resourceId);
		
		final SqlMapSession session = openSession();		
		try {
			session.delete(getNameSpace() + ".deleteByResourceId", param);		
		} catch (SQLException e)
		{
			throw new RuntimeException("Failed to query", e);
		} finally {
			try {
				session.close();
			} catch (Exception ignored) {
			}
		}		
	}

	@Override
	public SharingItem Get(long id)
	{
		return this.find(id);
	}

	@Override
	protected String getNameSpace()
	{
		return "Sharing";
	}		
}
