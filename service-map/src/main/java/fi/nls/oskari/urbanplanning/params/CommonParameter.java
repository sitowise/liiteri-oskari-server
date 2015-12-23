package fi.nls.oskari.urbanplanning.params;

public class CommonParameter {
	protected String value;
	protected String name;

	public boolean Validate() {
		return value != null && !value.isEmpty();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public CommonParameter(String name, String value) {
		this.name = name;
		this.value = value;
	}

}
