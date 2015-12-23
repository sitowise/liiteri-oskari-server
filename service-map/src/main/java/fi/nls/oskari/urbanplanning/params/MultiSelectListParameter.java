package fi.nls.oskari.urbanplanning.params;

import java.util.List;

public class MultiSelectListParameter extends CommonParameter {

	private List<String> values;

	public MultiSelectListParameter(String name, String value,
			List<String> values) {
		super(name,value);
		this.values = values;
	}

	@Override
	public boolean Validate() {

		String[] splittedValues = value.split(",");

		for (String v : splittedValues) {
			if (!values.contains(v))
				return false;
		}

		return true;

	}
}
