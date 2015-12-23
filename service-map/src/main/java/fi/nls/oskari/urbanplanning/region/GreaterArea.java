package fi.nls.oskari.urbanplanning.region;

public class GreaterArea extends TypedRegion{

	@Override
	protected String getUrlExtension() {
		return "/greaterArea";
	}
	@Override
	public String getName() {
		return "greaterArea";
	}
	@Override
	public String getTitle()
	{
		return "Suuralue";
	}
	
	
}
