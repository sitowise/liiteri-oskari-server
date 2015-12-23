package fi.nls.oskari.urbanplanning.params;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class RangeDateParameter extends CommonParameter {

	public RangeDateParameter(String name, String value) {
		super(name,value);
	}

	@Override
	public boolean Validate() {

		String[] splittedValues = value.split(",");
		if (splittedValues.length != 2)
			return false;
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			formatter.parse(splittedValues[0]);
			formatter.parse(splittedValues[1]);
			return true;
		} catch (ParseException ex) {
			return false;
		}

	}
}
