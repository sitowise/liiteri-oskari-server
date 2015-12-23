package fi.nls.oskari.urbanplanning.plandata;

import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.sito.liiteri.arcgis.domain.ArcgisLayer;
import pl.sito.liiteri.map.arcgislayer.service.ArcgisLayerService;
import pl.sito.liiteri.utils.ArcgisUtils;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceIbatisImpl;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.area.Area;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.urbanplanning.params.NumberParameter;
import fi.nls.oskari.urbanplanning.utils.JSONCopyHelper;
import fi.nls.oskari.urbanplanning.utils.PlanAction;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;

public class Plan extends PlanData {
    
    private static final Logger log = LogFactory.getLogger(Plan.class);

	@Override
	protected String getName() {
		return "plan";
	}

	@Override
	protected String buildUrl(List<CommonParameter> params) {
		StringWriter wr = new StringWriter();
		wr.write(_baseUrl);
		wr.write(getUrlExtension());

		if (params.get(0).Validate()) {
			wr.write("/");
			wr.write(params.get(0).getValue());
		} else
			throw new IllegalArgumentException("Invalid parameter value"
					+ params.get(0).getName());
		return wr.toString();
	}

	@Override
	public String getData(List<CommonParameter> params) throws ServiceException {
		if (params != null && !validateInputParameters(params)) {
			throw new ServiceException("Invalid parameters");
		}
		try {
			String data = convertData(getPlansData(buildUrl(params), null));
			JSONObject ob = JSONHelper.createJSONObject(data);
			addAreaArrayToJSONObject(Area.getInstance(AREA_RESERVATION)
					.getData((NumberParameter) params.get(0), PlanAction.PLAN),
					ob, AREA_RESERVATION);
			addAreaObjectToJSONObject(Area.getInstance(BUILDING_CONSERVATION)
					.getDataObject((NumberParameter) params.get(0), PlanAction.PLAN),
					ob, BUILDING_CONSERVATION);
			addAreaObjectToJSONObject(Area.getInstance(UNDERGROUND_AREA)
					.getDataObject((NumberParameter) params.get(0), PlanAction.PLAN),
					ob, UNDERGROUND_AREA);
			if(!ob.getString("tyviId").equalsIgnoreCase("null")) {
			    ob.put("hasGeometry", hasGeometry(ob.getString("tyviId")));
			}
			return ob.toString();

		} catch (IllegalArgumentException ex) {
			throw new ServiceException("Invalid parameter value", ex);
		}

		catch (JSONException ex) {
			throw new ServiceException("Error during preparing JSON Object", ex);
		}
	}

    private boolean hasGeometry(String tyviId) {

        String layerId = PropertyUtil.get("liiteri.urbanplanning.highlightlayerid");
        JSONArray features = findFeatures(tyviId, layerId);

        if (features.length() == 0) { // not found from normal layer, check also underground areas
            layerId = PropertyUtil.get("liiteri.urbanplanning.undergroundhighlightlayerid");
            features = findFeatures(tyviId, layerId);
        }

        return features.length() > 0;
    }

    private JSONArray findFeatures(String tyviId, String layerId) {
        final OskariLayerService service = new OskariLayerServiceIbatisImpl();
        final ArcgisLayerService arcgisLayerService = new ArcgisLayerService();
        final String FILTER_ATTRIBUTE = "Tyvi_Id";

        JSONArray features = new JSONArray();
        final OskariLayer layer = service.find(layerId);

        if (layer == null) {
            return features;
        }

        int arcgisId = ArcgisUtils.getArcgisId(layer);
        if (arcgisId < 0) {
            return features;
        }

        String arcgisMapServerUrl = ArcgisUtils.getArcgisMapServerUrl(layer);

        final ArcgisLayer arcgisLayer = new ArcgisLayer(arcgisMapServerUrl, arcgisId);
        try {
            JSONObject ret = new JSONObject();
            JSONArray filters = new JSONArray();
            JSONObject filter = new JSONObject();
            filter.put("attribute", FILTER_ATTRIBUTE);
            filter.put("operator", "=");
            filter.put("value", tyviId);

            filters.put(filter);

            ret.put("filters", filters);
            features = new JSONArray(arcgisLayerService.getFeatures(arcgisLayer, null, ret.toString()));

            return features;
        } catch (Exception e) {
            log.error(e, "Could not get features");
            return features;
        }
    }

    @Override
	protected String convertData(String inputData) throws JSONException {
		JSONObject ob = JSONHelper.createJSONObject(inputData);
		JSONObject outPutOb = new JSONObject();

		JSONCopyHelper.CopyValue(ob, "Id", outPutOb, "id");
		JSONCopyHelper.CopyValue(ob, "TyviId", outPutOb, "tyviId");
		JSONCopyHelper.CopyValue(ob, "Name", outPutOb, "name");
		
		JSONHelper.putValue(outPutOb, "municipalityName", ob.getString("MunicipalityName"));
		String formattedMunicipalityId = new DecimalFormat("000").format(ob.getDouble("MunicipalityId"));
		JSONHelper.putValue(outPutOb, "municipalityId", formattedMunicipalityId);
		JSONHelper.putValue(outPutOb, "municipality", formattedMunicipalityId + " " + ob.getString("MunicipalityName"));
		
		JSONCopyHelper.CopyValue(ob, "DecisionMaker", outPutOb, "decisionMaker");
		JSONCopyHelper.CopyValue(ob, "DecisionNumber", outPutOb, "decisionNumber");
				
		JSONCopyHelper.CopyDoubleValue(ob, "PlanArea", outPutOb, "planArea");
		JSONCopyHelper.CopyDoubleValue(ob, "Duration", outPutOb, "duration");
		JSONCopyHelper.CopyDoubleValue(ob, "UndergroundArea", outPutOb, "undergroundArea");
		JSONCopyHelper.CopyDoubleValue(ob, "PlanAreaNew", outPutOb, "planAreaNew");
		JSONCopyHelper.CopyDoubleValue(ob, "PlanAreaChange", outPutOb, "planAreaChange");
		
		JSONCopyHelper.CopyDoubleValue(ob, "CoastlineLength", outPutOb, "coastlineLength");
		JSONCopyHelper.CopyLongValue(ob, "BuildingCountOwn", outPutOb, "buildingCountOwn");
		JSONCopyHelper.CopyLongValue(ob, "BuildingCountOther", outPutOb, "buildingCountOther");
		JSONCopyHelper.CopyLongValue(ob, "BuildingCountOwnHoliday", outPutOb, "buildingCountOwnHoliday");
		JSONCopyHelper.CopyLongValue(ob, "BuildingCountOtherHoliday", outPutOb, "buildingCountOtherHoliday");	
		
		JSONCopyHelper.CopyValue(ob, "MunicipalityPlanId", outPutOb, "municipalityPlanId");
		JSONCopyHelper.CopyValue(ob, "GeneratedPlanId", outPutOb, "generatedPlanId");
		
		try {
			JSONCopyHelper.CopyDateValue(ob, "ApprovalDate", outPutOb, "approvalDate");
			JSONCopyHelper.CopyDateValue(ob, "ProposalDate", outPutOb, "proposalDate");
			JSONCopyHelper.CopyDateValue(ob, "InitialDate", outPutOb, "initialDate");
			JSONCopyHelper.CopyDateValue(ob, "FillDate", outPutOb, "fillDate");
		} catch (Exception ex) {
			throw new JSONException("Error during parsing JSON dates value");
		}
		return outPutOb.toString();

	}
}
