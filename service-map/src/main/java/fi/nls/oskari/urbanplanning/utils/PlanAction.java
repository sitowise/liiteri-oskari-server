package fi.nls.oskari.urbanplanning.utils;

public enum PlanAction {
	PLAN("plans"), PLANSUMMARY("planSUMMARY");

	private String name;

	private PlanAction(String name) {

		this.name = name;
	}

	public String getName() {
		return name;
	}

}
