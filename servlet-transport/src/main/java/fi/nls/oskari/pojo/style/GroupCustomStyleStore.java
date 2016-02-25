package fi.nls.oskari.pojo.style;

import java.util.HashMap;

public class GroupCustomStyleStore extends CustomStyleStore {

	private HashMap<String, CustomStyleStore> subStyles = new HashMap<String, CustomStyleStore>();	
	
	public GroupCustomStyleStore() {
		setType("group");
	}

	public HashMap<String, CustomStyleStore> getSubStyles() {
		return subStyles;
	}

	public void setSubStyles(HashMap<String, CustomStyleStore> subStyles) {
		this.subStyles = subStyles;
	}
	
	public void addSubStyle(String key, CustomStyleStore subStyle) {
		this.subStyles.put(key, subStyle);
	}
}
