package fi.nls.oskari.urbanplanning.params;

public class IdParameter extends NumberParameter {
	@Override
	public boolean Validate() {
		try {
			long l = Long.parseLong(value);
			return l>0;

		} catch (NumberFormatException ex)

		{
			return false;
		}

	}

	public IdParameter(String name, String value) {
		super(name,value);
	}
}
