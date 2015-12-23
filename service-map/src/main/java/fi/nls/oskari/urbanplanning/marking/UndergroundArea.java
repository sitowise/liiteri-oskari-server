package fi.nls.oskari.urbanplanning.marking;

import java.util.List;

import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.urbanplanning.params.IdParameter;
import org.json.JSONArray;

public class UndergroundArea extends Marking{
	@Override
	protected String getName() {
		return "undergroundAreas";
	}
	@Override
	protected String getUrlExtension() {
		return "undergroundAreas";
	}

	@Override
	public String getData(
			List<CommonParameter> params,
			CommonParameter type,
			IdParameter municipalityId,
			CommonParameter mainMarkName) throws ServiceException {

		if (mainMarkName != null && !mainMarkName.getValue().equals("ma")) {
			// requesting undergroundAreas with mainMarkId is
			// invalid, so let's return an empty array here
			return new JSONArray().toString();
		}

		// for undergroundAreas, avoid passing mainMarkName as
		// anything other than null
		return super.getData(params, type, municipalityId, null);
	}

	@Override
	protected boolean validateInputParameters(List<CommonParameter> params,
			CommonParameter type, IdParameter municipalityId,
			CommonParameter mainMarkName) {
		if (mainMarkName != null)
			return false;

		return super.validateInputParameters(params, type, municipalityId,
				mainMarkName);
	}
}
