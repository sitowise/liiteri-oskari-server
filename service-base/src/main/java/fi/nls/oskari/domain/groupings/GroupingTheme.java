package fi.nls.oskari.domain.groupings;

import java.util.ArrayList;
import java.util.List;

public class GroupingTheme {

	private long id;
	private Long parentThemeId;
	private Long oskariGroupingId;
	private Long mainThemeId;
	//private Long apiThemeId;
	private int themeType;
	private String name;
	private List<GroupingTheme> subthemes = new ArrayList<GroupingTheme>();
	private List<GroupingThemeData> themeData = new ArrayList<GroupingThemeData>();
	private List<GroupingPermission> permissions = new ArrayList<GroupingPermission>();
	private Boolean isPublic;
	private int status;

	public long getId() {
		return id;

	}

	public Long getParentThemeId() {
		return parentThemeId;

	}

	public Long getOskariGroupingId() {
		return oskariGroupingId;

	}

	public int getThemeType() {
		return themeType;

	}

	public String getName() {
		return name;

	}

	public void setId(long id) {
		this.id = id;

	}

	public void setParentThemeId(Long parentThemeId) {
		this.parentThemeId = parentThemeId;

	}

	public void setOskariGroupingId(Long oskariGroupingId) {
		this.oskariGroupingId = oskariGroupingId;

	}

	public void setThemeType(int themeType) {
		this.themeType = themeType;

	}

	public void setName(String name) {
		this.name = name;
	}

	public List<GroupingTheme> getSubThemes() {
		return subthemes;
	}

	public void setSubThemes(List<GroupingTheme> subthemes) {
		this.subthemes = subthemes;
	}

	public List<GroupingThemeData> getThemeData() {
		return themeData;
	}

	public void setThemeData(List<GroupingThemeData> themeData) {
		this.themeData = themeData;
	}

	public Long getMainThemeId() {
		return mainThemeId;

	}

	public void setMainThemeId(Long mainThemeId) {
		this.mainThemeId = mainThemeId;

	}

	public Boolean isPublic() {
		return isPublic;
	}

	public void setPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}

	public List<GroupingPermission> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<GroupingPermission> permissions) {
		this.permissions = permissions;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	/*public Long getApiThemeId()
	{
		return apiThemeId;
	}
	
	public void setApiThemeId(Long apiThemeId)
	{
		this.apiThemeId = apiThemeId;
	}*/
}
