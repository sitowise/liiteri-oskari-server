package fi.nls.oskari.pojo.style;

import java.util.HashMap;

import fi.nls.oskari.pojo.WFSCustomStyleStore;

public class UniqueValueCustomStyleStore extends CustomStyleStore {

	private HashMap<String, WFSCustomStyleStore> uniqueValuesInfo = new HashMap<String, WFSCustomStyleStore>();	
	private WFSCustomStyleStore defaultStyle;
	private String field;
	
	public  UniqueValueCustomStyleStore() {
		setType("uniqueValue");
	}
	
	public HashMap<String, WFSCustomStyleStore> getUniqueValuesInfo() {
		return uniqueValuesInfo;
	}
	public void setUniqueValuesInfo(HashMap<String, WFSCustomStyleStore> uniqueValuesInfo) {
		this.uniqueValuesInfo = uniqueValuesInfo;
	}
	public WFSCustomStyleStore getDefaultStyle() {
		return defaultStyle;
	}
	public void setDefaultStyle(WFSCustomStyleStore defaultStyle) {
		this.defaultStyle = defaultStyle;
	}
	
	public void AddUniqueValuesInfo(String value, WFSCustomStyleStore style) {
		this.uniqueValuesInfo.put(value, style);
	}
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
}
