package fi.nls.oskari.domain.map;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserGisData {
	private long id;
	private String dataId;
	private String dataType;
	private Date expirationDate;
	private long userId;
	private String status;
	private String downloadServiceUrl;
	private List<UserGisDataSharing> userGisDataSharing = new ArrayList<UserGisDataSharing>();
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getDataId() {
		return dataId;
	}
	public void setDataId(String dataId) {
		this.dataId = dataId;
	}
	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	public Date getExpirationDate() {
		return expirationDate;
	}
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public List<UserGisDataSharing> getUserGisDataSharing() {
		return userGisDataSharing;
	}
	public void setUserGisDataSharing(List<UserGisDataSharing> userGisDataSharing) {
		this.userGisDataSharing = userGisDataSharing;
	}
	public String getDownloadServiceUrl() {
		return downloadServiceUrl;
	}
	public void setDownloadServiceUrl(String downloadServiceUrl) {
		this.downloadServiceUrl = downloadServiceUrl;
	}
}
