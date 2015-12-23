package fi.nls.oskari.urbanplanning.region;



public class AdministrativeCourt extends TypedRegion{

	@Override
	protected String getUrlExtension() {
		return "/administrativeCourt";
	}
	@Override
	public String getName() {
		return "administrativeCourt";
	}
	@Override
	public String getTitle()
	{
		return "Hallinto-oikeus";
	}

}
