package fi.nls.oskari.domain.workspaces;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WorkSpace {

	private long id;
	private String name;
	private String settings;
	private long userId;
	private Date expirationDate;
	private String status;
	private Boolean hidden;
	
	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	private List<WorkSpaceSharing> workSpaceSharing = new ArrayList<WorkSpaceSharing>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSettings() {
		return settings;
	}

	public void setSettings(String settings) {
		this.settings = settings;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public List<WorkSpaceSharing> getWorkSpaceSharing() {
		return workSpaceSharing;
	}

	public void setWorkSpaceSharing(List<WorkSpaceSharing> workSpaceSharing) {

		this.workSpaceSharing = workSpaceSharing;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
