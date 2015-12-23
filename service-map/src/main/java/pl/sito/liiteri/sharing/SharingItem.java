package pl.sito.liiteri.sharing;

import fi.nls.oskari.domain.User;

public class SharingItem
{
	public enum Status {
		CANCELED,
		INITIAL,
		PENDING,
		SHARED
	}
	
	public enum CredentialType {
		USER,
		ROLE
	}
	
	public enum ResourceType {
		LAYER,
		WORKSPACE,
		THEME,
		SERVICE_PACKAGE
	}
	
	private long permissionId;
	private long resourceId;
	private ResourceType resourceType;
	private long credentialId;
	private CredentialType credentialType;
	private String email;
	private Status status;
	private User sender;
	private String token;
	
	public long getPermissionId()
	{
		return permissionId;
	}
	public void setPermissionId(long permissionId)
	{
		this.permissionId = permissionId;
	}
	public long getResourceId()
	{
		return resourceId;
	}
	public void setResourceId(long resourceId)
	{
		this.resourceId = resourceId;
	}
	public ResourceType getResourceType()
	{
		return resourceType;
	}
	public void setResourceType(ResourceType resourceType)
	{
		this.resourceType = resourceType;
	}
	public long getCredentialId()
	{
		return credentialId;
	}
	public void setCredentialId(long credentialId)
	{
		this.credentialId = credentialId;
	}
	public CredentialType getCredentialType()
	{
		return credentialType;
	}
	public void setCredentialType(CredentialType credentialType)
	{
		this.credentialType = credentialType;
	}
	public String getEmail()
	{
		return email;
	}
	public void setEmail(String email)
	{
		this.email = email;
	}
	public Status getStatus()
	{
		return status;
	}
	public void setStatus(Status status)
	{
		this.status = status;
	}	
	public User getSender()
	{
		return sender;
	}
	public void setSender(User sender)
	{
		this.sender = sender;
	}
	@Override
	public String toString()
	{
		return "SharingItem [permissionId=" + permissionId + ", resourceId="
				+ resourceId + ", resourceType=" + resourceType
				+ ", credentialId=" + credentialId + ", credentialType="
				+ credentialType + ", email=" + email + ", status=" + status
				+ ", sender=" + sender + "]";
	}
	public String getToken()
	{
		return token;
	}
	public void setToken(String token)
	{
		this.token = token;
	}		
	
}
