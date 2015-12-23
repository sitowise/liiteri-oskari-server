package fi.nls.oskari.urbanplanning.utils;

import java.util.HashMap;

public enum TimeSelectorType {
	ACCEPTDATE("ACCEPTDATE", "Hyväksymispvm"), SUGGESTIONDATE("SUGGESTIONDATE",
			"Ehdotuspvm"), ANNOUNCEDATE("ANNOUNCEDATE",
			"Vireilletulosta ilm. pvm"), UPDATETIME("UPDATETIME",
			"Täyttämispvm");
	private String value;
	private String name;

	private TimeSelectorType(String value, String name) {
		this.value = value;
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	private static HashMap<String, TimeSelectorType> valueMap = new HashMap<String, TimeSelectorType>(
			4);
	private static HashMap<String, TimeSelectorType> nameMap = new HashMap<String, TimeSelectorType>(
			4);

	static {
		for (TimeSelectorType type : TimeSelectorType.values()) {
			valueMap.put(type.value, type);
		}

		for (TimeSelectorType type : TimeSelectorType.values()) {
			nameMap.put(type.name, type);
		}
	}

	// constructor and getCodeValue left out

	public static TimeSelectorType getInstanceFromValue(String value) {
		return valueMap.get(value);
	}

	public static TimeSelectorType getInstanceFromName(String name) {
		return nameMap.get(name);
	}

	public static int getElementCount() {
		return valueMap.size();
	}
}
