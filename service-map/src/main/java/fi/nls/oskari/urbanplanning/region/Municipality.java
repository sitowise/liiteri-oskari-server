package fi.nls.oskari.urbanplanning.region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Municipality extends TypedRegion{

	@Override
	protected String getUrlExtension() {
		return "/municipality";
	}
	@Override
	public String getName() {
		return "municipality";
	}
	@Override
	public String getTitle()
	{
		return "Kunta";
	}
	@Override
	protected List<String> getValidInputParameters() {
		return new ArrayList<String> (Arrays.asList("greaterArea", "administrativeCourt", "ely","county", "subRegion"));
	}
}
