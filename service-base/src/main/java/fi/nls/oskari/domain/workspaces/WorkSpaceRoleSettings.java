package fi.nls.oskari.domain.workspaces;

public class WorkSpaceRoleSettings {
	private long id;
	private long roleId;
	private int validWorkSpaceDaysLimit;
	private int workSpaceAmountLimit;

	
	public long getId()
	{
		return id;		
	}
	
	public void setId(long id)
	{
		this.id = id;
	}
	
	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public int getValidWorkSpaceDaysLimit() {
		return validWorkSpaceDaysLimit;
	}

	public void setValidWorkSpaceDaysLimit(int validWorkSpaceDaysLimit) {
		this.validWorkSpaceDaysLimit = validWorkSpaceDaysLimit;
	}

	public int getWorkSpaceAmount() {
		return workSpaceAmountLimit;
	}

	public void setWorkSpaceAmount(int workSpaceAmountLimit) {
		this.workSpaceAmountLimit = workSpaceAmountLimit;
	}
}