package fi.nls.oskari.urbanplanning.utils;

import java.util.HashMap;

public enum PlanApprover {
	V(1, "V-kunnanvaltuusto"), H(2, "H-kunnanhallitus"), L(3, "L-lautakunta");
	private int value;
	private String name;

	private PlanApprover(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	private static HashMap<Integer, PlanApprover> valueMap = new HashMap<Integer, PlanApprover>(
			3);
	private static HashMap<String, PlanApprover> nameMap = new HashMap<String, PlanApprover>(
			3);

	static {
		for (PlanApprover approver : PlanApprover.values()) {
			valueMap.put(approver.value, approver);
		}

		for (PlanApprover approver : PlanApprover.values()) {
			nameMap.put(approver.name, approver);
		}
	}

	// constructor and getCodeValue left out

	public static PlanApprover getInstanceFromValue(int value) {
		return valueMap.get(value);
	}

	public static PlanApprover getInstanceFromName(String name) {
		return nameMap.get(name);
	}
	
	public static int getElementCount()
	{
		return valueMap.size();
	}
}
