package pl.sito.liiteri.sharing;

import org.json.JSONObject;

import fi.nls.oskari.util.JSONHelper;

public class SharingMappingService implements ISharingMappingService
{

	@Override
	public SharingItem Map(JSONObject json)
	{
		SharingItem item = new SharingItem();
		
		item.setPermissionId(json.optLong("id"));
		item.setCredentialId(json.optLong("externalId"));
		item.setEmail(json.optString("email"));
		item.setResourceId(json.optLong("resourceId"));
		
		String credentialTypeString = json.optString("externalType", null);
		if (credentialTypeString != null)
			item.setCredentialType(SharingItem.CredentialType.valueOf(credentialTypeString));
		String resourceTypeString = json.optString("resourceType", null);
		if (resourceTypeString != null)
			item.setResourceType(SharingItem.ResourceType.valueOf(resourceTypeString));
		
		return item;
	}

	@Override
	public JSONObject Map(SharingItem item)
	{
		JSONObject result = new JSONObject();
		JSONHelper.putValue(result, "id", item.getPermissionId());
		JSONHelper.putValue(result, "externalType", item.getCredentialType().toString());
		JSONHelper.putValue(result, "externalId", item.getCredentialId());
		JSONHelper.putValue(result, "email", item.getEmail());
		JSONHelper.putValue(result, "resourceType", item.getResourceType().toString());
		JSONHelper.putValue(result, "resourceId", item.getResourceId());
		
		return result;
	}
	
}
