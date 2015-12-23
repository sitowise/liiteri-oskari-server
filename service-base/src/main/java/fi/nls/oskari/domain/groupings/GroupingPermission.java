package fi.nls.oskari.domain.groupings;

import java.util.List;

public class GroupingPermission {
	private long id;
	private String name;
	private long externalId;
	private long oskariGroupingId;
	private String externalType;
	private String email;
	private boolean isTheme;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getExternalId() {
		return externalId;
	}

	public void setExternalId(long externalId) {
		this.externalId = externalId;

	}

	public long getOskariGroupingId() {
		return oskariGroupingId;
	}

	public void setOskariGroupingId(long oskariGroupingId) {
		this.oskariGroupingId = oskariGroupingId;

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;

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

	public boolean isTheme() {
		return isTheme;
	}

	public void setTheme(boolean isTheme) {
		this.isTheme = isTheme;
	}

}
