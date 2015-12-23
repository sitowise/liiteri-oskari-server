package fi.nls.oskari.domain.map;

public class UserGisDataRoleSettings {
	private long id;
	private int datasetMaxSizeMb;
	private int datasetAmountLimit;
	private long roleId;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getDatasetMaxSizeMb() {
		return datasetMaxSizeMb;
	}
	public void setDatasetMaxSizeMb(int datasetMaxSizeMb) {
		this.datasetMaxSizeMb = datasetMaxSizeMb;
	}
	public int getDatasetAmountLimit() {
		return datasetAmountLimit;
	}
	public void setDatasetAmountLimit(int datasetAmountLimit) {
		this.datasetAmountLimit = datasetAmountLimit;
	}
	public long getRoleId() {
		return roleId;
	}
	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}
}
