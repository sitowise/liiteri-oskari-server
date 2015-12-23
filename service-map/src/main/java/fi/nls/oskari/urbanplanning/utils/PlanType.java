package fi.nls.oskari.urbanplanning.utils;

import java.util.HashMap;

public enum PlanType {
	MAIN(1, "Asemakaava (ei ranta-asemak. ja kokonaan maanalaisia asemak.)"), COASTLINE(
			2, "Ranta-asemakaava"), UNDERGROUND(3,
			"Asemakaava, jossa maanalaista tilaa");
	private int value;
	private String name;

	private PlanType(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	private static HashMap<Integer, PlanType> valueMap = new HashMap<Integer, PlanType>(
			3);
	private static HashMap<String, PlanType> nameMap = new HashMap<String, PlanType>(
			3);

	static {
		for (PlanType type : PlanType.values()) {
			valueMap.put(type.value, type);
		}

		for (PlanType type : PlanType.values()) {
			nameMap.put(type.name, type);
		}
	}

	// constructor and getCodeValue left out

	public static PlanType getInstanceFromValue(int value) {
		return valueMap.get(value);
	}

	public static PlanType getInstanceFromName(String name) {
		return nameMap.get(name);
	}
	
	public static int getElementCount()
	{
		return valueMap.size();
	}
}
