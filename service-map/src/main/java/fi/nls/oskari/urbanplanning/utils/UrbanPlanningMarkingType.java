package fi.nls.oskari.urbanplanning.utils;

public enum UrbanPlanningMarkingType {
	STANDARD("standard"), MUNICIPALITY("municipality");

	private String name;

	private UrbanPlanningMarkingType(String name) {

		this.name = name;
	}

	public String getName() {
		return name;
	}

}
