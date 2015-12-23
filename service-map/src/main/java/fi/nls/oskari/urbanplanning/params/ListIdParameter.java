package fi.nls.oskari.urbanplanning.params;

public class ListIdParameter extends NumberParameter {
	@Override
	public boolean Validate() {
		try {
			String[] splittedValues = value.split(",");
			for (int i = 0; i < splittedValues.length; i++)

			{
				long l = Long.parseLong(splittedValues[i]);
				if (l < 1)
					return false;
			}

			return true;

		} catch (NumberFormatException ex)

		{
			return false;
		}

	}

	public ListIdParameter(String name, String value) {
		super(name, value);
	}

}
