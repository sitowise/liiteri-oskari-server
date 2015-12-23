package fi.nls.oskari.urbanplanning.marking;

import java.util.List;

import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.urbanplanning.params.IdParameter;
import fi.nls.oskari.urbanplanning.utils.UrbanPlanningMarkingType;

public class AreaReservation extends Marking {

	@Override
	protected String getName() {
		return "areaReservations";
	}

	@Override
	protected String getUrlExtension() {
		return "areaReservations";
	}

	@Override
	protected boolean validateInputParameters(List<CommonParameter> params,
			CommonParameter type, IdParameter municipalityId,
			CommonParameter mainMarkName) {
		if (type.getValue().equals(UrbanPlanningMarkingType.STANDARD.getName())
				&& mainMarkName != null)
			return false;
		if (mainMarkName != null && !mainMarkName.Validate())
			return false;

		return super.validateInputParameters(params, type, municipalityId,
				mainMarkName);
	}
}
