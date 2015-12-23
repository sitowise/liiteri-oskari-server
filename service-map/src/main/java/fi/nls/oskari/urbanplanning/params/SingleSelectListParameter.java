package fi.nls.oskari.urbanplanning.params;

import java.util.List;

public class SingleSelectListParameter extends CommonParameter {

	private List<String> values;

	public SingleSelectListParameter(String name, String value,
			List<String> values) {
		super(name,value);
		this.values = values;
	}

	@Override
	public boolean Validate() {

		if (values.contains(value))
			return true;

		return false;

	}
}
