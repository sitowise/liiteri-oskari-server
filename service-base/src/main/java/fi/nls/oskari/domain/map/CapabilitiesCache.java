package fi.nls.oskari.domain.map;

public class CapabilitiesCache {
	
	private int id;
	private int layerId;
	private String data;
	private String version;
	private boolean userWms;
	
	public int geId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getLayerId() {
		return layerId;
	}
	public void setLayerId(int layerId) {
		this.layerId = layerId;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public boolean isUserWms() {
		return userWms;
	}
	public void setUserWms(boolean userWms) {
		this.userWms = userWms;
	}
	
	public String toString(){
		return "layerId:"+ layerId + " version:"+version;
	}

}
