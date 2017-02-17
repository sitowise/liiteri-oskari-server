package fi.nls.oskari.domain.groupings;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Grouping {

	private long id;
	private String name;
	private String label;
	private int status;
	private String description;
	private String userGroup;
	private Date created;
	private Date updated;
	private String mapState;
	private String url;

	private List<GroupingTheme> themes = new ArrayList<GroupingTheme>();
	private List<GroupingPermission> permissions = new ArrayList<GroupingPermission>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;

	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public String getMapState() {
		return mapState;
	}

	public String getUrl() {
		return url;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<GroupingTheme> getThemes() {
		return themes;
	}

	public void setThemes(List<GroupingTheme> themes) {
		this.themes = themes;
	}

	public List<GroupingPermission> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<GroupingPermission> permissions) {
		this.permissions = permissions;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUserGroup() {
		return this.userGroup;
	}

	public void setUserGroup(String userGroup) {
		this.userGroup = userGroup;
	}

	public Date getCreated() {
		return this.created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getUpdated() {
		return this.updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public void setMapState(String mapState) {
		this.mapState = mapState;
	}

}
