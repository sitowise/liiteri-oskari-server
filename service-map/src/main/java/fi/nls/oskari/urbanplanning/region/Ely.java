package fi.nls.oskari.urbanplanning.region;

public class Ely extends TypedRegion{
	
	@Override
	protected String getUrlExtension() {
		return "/ely";
	}
	@Override
	public String getName() {
		return "ely";
	}
	@Override
	public String getTitle()
	{
		return "Ympäristö-ELY";
	}
	
	
}
