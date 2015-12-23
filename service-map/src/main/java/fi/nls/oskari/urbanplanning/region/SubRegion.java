package fi.nls.oskari.urbanplanning.region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubRegion extends TypedRegion{
	@Override
	protected String getUrlExtension() {
		return "/subRegion";
	}
	@Override
	public String getName() {
		return "subRegion";
	}
	@Override
	public String getTitle()
	{
		return "Seutukunta";
	}
	@Override
	protected List<String> getValidInputParameters() {
		return new ArrayList<String> (Arrays.asList("county"));
	}
}
