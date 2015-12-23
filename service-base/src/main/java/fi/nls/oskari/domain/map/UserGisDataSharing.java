package fi.nls.oskari.domain.map;

public class UserGisDataSharing {
	private long id;
	private long datasetId;
	private long externalId;
	private String externalType;
	private String email;
	private boolean emailSent;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getDatasetId() {
		return datasetId;
	}
	public void setDatasetId(long datasetId) {
		this.datasetId = datasetId;
	}
	public long getExternalId() {
		return externalId;
	}
	public void setExternalId(long externalId) {
		this.externalId = externalId;
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
	public boolean isEmailSent() {
		return emailSent;
	}
	public void setEmailSent(boolean emailSent) {
		this.emailSent = emailSent;
	}
	
}
