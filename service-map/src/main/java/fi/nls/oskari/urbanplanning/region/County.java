package fi.nls.oskari.urbanplanning.region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class County extends TypedRegion{
	@Override
	protected String getUrlExtension() {
		return "/county";
	}
	@Override
	public String getName() {
		return "county";
	}
	@Override
	public String getTitle()
	{
		return "Maakunta";
	}
	
	@Override
	protected List<String> getValidInputParameters() {
		return new ArrayList<String> (Arrays.asList("greaterArea", "administrativeCourt", "ely"));
	}
}
