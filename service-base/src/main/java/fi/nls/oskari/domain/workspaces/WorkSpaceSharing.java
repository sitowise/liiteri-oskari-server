package fi.nls.oskari.domain.workspaces;

public class WorkSpaceSharing {

	private long id;
	private long externalId;
	private long workSpaceId;
	private String externalType;
	private String email;

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getExternalId() {
		return this.externalId;
	}

	public void setExternalId(long externalId) {
		this.externalId = externalId;
	}

	public long getWorkSpaceId() {
		return this.workSpaceId;
	}

	public void setWorkSpaceId(long workSpaceId) {
		this.workSpaceId = workSpaceId;
	}

	public String getExternalType() {
		return externalType;
	}

	public void setExternalType(String externalType) {
		this.externalType = externalType;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
