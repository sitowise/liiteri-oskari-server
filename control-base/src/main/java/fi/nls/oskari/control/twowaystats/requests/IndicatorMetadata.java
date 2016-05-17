package fi.nls.oskari.control.twowaystats.requests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.szopa.JSONSzopaHelper;
import fi.nls.oskari.control.szopa.RegionDefinition;
import fi.nls.oskari.control.szopa.RegionService;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONCopyHelper;
import fi.nls.oskari.util.JSONHelper;

public class IndicatorMetadata extends TwowayRequest {

    private RegionService _regionService = RegionService.getInstance();

    private static Logger log = LogFactory.getLogger(IndicatorMetadata.class);

    // public boolean isValid () {
    // return getIndicator() != null && getIndicator().isEmpty();
    // }

    @Override
    public String getName() {
        return "indicator_metadata";
    }

    @Override
    public String getRequestSpecificParams() {
        return "/commuteStatistics";
    }

    @Override
    protected String ConvertData(String data) throws ActionException {
        try {
            JSONObject result = new JSONObject();

            JSONArray inputArray = JSONHelper.createJSONArray(data);

            log.info("Got", inputArray.length(), "statistics. Requested id",
                    getIndicator());

            for (int i = 0; i < inputArray.length(); i++) {

                JSONObject source = inputArray.getJSONObject(i);

                if (source.get("Id").toString().equals(getIndicator())) {

                    JSONCopyHelper.Copy(source, "Id", result, "id");
                    JSONCopyHelper.LanguageAwareCopy(source, "Name", result,
                            "title");
                    JSONCopyHelper.LanguageAwareCopy(source, "Description",
                            result, "description");
                    JSONCopyHelper.LanguageAwareCopy(source, "AdditionalInformation",
                            result, "additionalInfo");
                    JSONHelper.putValue(result, "unit", " ");
                    JSONHelper.putValue(result, "stage", JSONSzopaHelper
                            .createLanguageJSONObject("ProcessingStage"));

                    JSONHelper.putValue(result, "lifeCycleState",
                            JSONSzopaHelper.createLanguageJSONObject(" "));

                    JSONObject organization = new JSONObject();
                    JSONHelper.putValue(organization, "id", 69);
                    JSONHelper
                            .putValue(
                                    organization,
                                    "title",
                                    JSONSzopaHelper
                                            .createLanguageJSONObject("Suomen ympäristökeskus"));

                    // TODO: by default all region categories are taken
                    JSONObject classifications = new JSONObject();
                    JSONObject regionClassifications = new JSONObject();
                    JSONArray regionCategoriesArray = JSONSzopaHelper
                            .createJSONArrayFromArray(GetRegionCategories());
                    JSONHelper.putValue(regionClassifications, "values",
                            regionCategoriesArray);
                    JSONHelper.putValue(classifications, "region",
                            regionClassifications);

                    JSONArray typeClassifications = new JSONArray();

                    JSONArray inputTypes = source
                            .getJSONArray("CommuteStatisticsTypes");

                    for (int j = 0; j < inputTypes.length(); j++) {
                        JSONObject inputType = inputTypes.getJSONObject(j);
                        JSONObject type = new JSONObject();
                        JSONCopyHelper.Copy(inputType, "Id", type, "id");
                        JSONHelper.putValue(type, "orderNumber", j);
                        JSONCopyHelper.Copy(inputType, "Description", type,
                                "name");
                        typeClassifications.put(type);
                    }

                    JSONHelper.putValue(classifications, "type",
                            typeClassifications);

                    JSONObject sexClassifications = new JSONObject();
                    JSONArray sexCategoriesArray = new JSONArray(new String[] {
                            "male", "female", "total" });
                    JSONHelper.putValue(sexClassifications, "values",
                            sexCategoriesArray);
                    JSONHelper.putValue(classifications, "sex",
                            sexClassifications);

                    JSONObject directionClassifications = new JSONObject();
                    JSONArray directionCategoriesArray = new JSONArray(
                            new String[] { "work", "home" });
                    JSONHelper.putValue(directionClassifications, "values",
                            directionCategoriesArray);
                    JSONHelper.putValue(classifications, "direction",
                            directionClassifications);

                    JSONArray years = Indicators.convertTimePeriods(source
                            .getJSONArray("CommuteStatisticsYears"));
                    JSONHelper.putValue(result, "years", years);
                    JSONHelper.putValue(result, "gridYears", years);

                    Map<String, HashSet<Integer>> dataSourcesMap = Indicators
                            .gatherDataSources(source);
                    JSONArray dataSources = new JSONArray();
                    for (String dataSourceName : dataSourcesMap.keySet()) {
                        JSONObject dataSourceItem = new JSONObject();
                        JSONHelper.putValue(dataSourceItem, "name",
                                dataSourceName);
                        JSONArray yearsArray = new JSONArray();
                        for (Integer yearItem : dataSourcesMap
                                .get(dataSourceName)) {
                            yearsArray.put(yearItem);
                        }
                        JSONHelper
                                .putValue(dataSourceItem, "years", yearsArray);
                        dataSources.put(dataSourceItem);
                    }
                    JSONHelper.putValue(result, "dataSources", dataSources);

                    JSONHelper.putValue(result, "organization", organization);
                    JSONHelper.putValue(result, "classifications",
                            classifications);

                    JSONObject privacyLimit = new JSONObject();
                    JSONCopyHelper.Copy(source, "PrivacyDescription",
                            privacyLimit, "Description");
                    JSONHelper.putValue(result, "privacyLimit", privacyLimit);

                    JSONCopyHelper.Copy(source, "Unit", result, "unit");
                    JSONCopyHelper.LanguageAwareCopy(source, "TimeSpan",
                            result, "stage");
                    JSONCopyHelper.LanguageAwareCopy(source,
                            "TimeSpanDescription", result, "lifeCycleState");
                }
            }
            return result.toString(1);
        } catch (JSONException e) {
            log.error("Cannot convert data", e);
            throw new ActionException("Cannot convert data", e);
        }
    }

    //
    // @Override
    // public String getData() throws ActionException {
    // return
    // "{\"additionalInfo\":{\"en\":\"additionalInfo\",\"fi\":\"additionalInfo\",\"sv\":\"additionalInfo\"},\"classifications\":{\"region\":{\"values\":[\"KUNTA\",\"SEUTUKUNTA\",\"MAAKUNTA\",\"ELY_E\",\"ELY_L\",\"ELY_Y\",\"FINLAND\",\"LOCALITY\",\"NEIGHBORHOODTYPE\",\"URBANAREA\",\"PLANNEDAREA\",\"ADMINISTRATIVELAWAREA\",\"SHOPAREA\",\"CITYRURALAREATYPE\",\"URBANAREATYPE\",\"CITYCENTRALAREA\",\"CITYCENTRALTYPE\",\"LOCALDENSITYTYPE\"]}},\"description\":{\"en\":\"TOL\",\"fi\":\"TOL\",\"sv\":\"TOL\"},\"id\":200800001,\"lifeCycleState\":{\"en\":\"fake life cycle stage\",\"fi\":\"fake life cycle stage\",\"sv\":\"fake life cycle stage\"},\"organization\":{\"id\":69,\"title\":{\"en\":\"Suomen ympäristökeskus\",\"fi\":\"Suomen ympäristökeskus\",\"sv\":\"Suomen ympäristökeskus\"}},\"stage\":{\"en\":null,\"fi\":null,\"sv\":null},\"themes\":[],\"timePeriods\":[{\"AreaTypes\":[{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Taajama\",\"Id\":\"locality\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Kaupunkiseutu\",\"Id\":\"urban_area\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Kunta\",\"Id\":\"municipality\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Seutukunta\",\"Id\":\"sub_region\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Maakunta\",\"Id\":\"region\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"ELY E-alue\",\"Id\":\"ely_e\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"ELY L-alue\",\"Id\":\"ely_l\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"ELY Y-alue\",\"Id\":\"ely_y\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Suomi\",\"Id\":\"finland\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Asemakaavoitettu alue\",\"Id\":\"planned_area\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Asutusalueen tyyppi\",\"Id\":\"neighborhood_type\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Hallinto-oikeus\",\"Id\":\"administrative_law_area\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Kaupan alue\",\"Id\":\"shop_area\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Kaupunkimaaseutuluokitustyyppi\",\"Id\":\"city_rural_area_type\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Kaupunkiseutu-tyyppi\",\"Id\":\"urban_area_type\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Keskusta-alue\",\"Id\":\"city_central_area\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Keskustatyyppi\",\"Id\":\"city_central_type\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Taajama harva/tiheä -tyyppi\",\"Id\":\"locality_density_type\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 250m\",\"Id\":\"grid250m\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 500m\",\"Id\":\"grid500m\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 1km\",\"Id\":\"grid1km\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 2km\",\"Id\":\"grid2km\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 5km\",\"Id\":\"grid5km\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 10km\",\"Id\":\"grid10km\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 20km\",\"Id\":\"grid20km\"},{\"DataSource\":\"\",\"Description\":\"Kunta\",\"Id\":\"municipality\"},{\"DataSource\":\"\",\"Description\":\"Seutukunta\",\"Id\":\"sub_region\"},{\"DataSource\":\"\",\"Description\":\"Maakunta\",\"Id\":\"region\"},{\"DataSource\":\"\",\"Description\":\"ELY E-alue\",\"Id\":\"ely_e\"},{\"DataSource\":\"\",\"Description\":\"ELY L-alue\",\"Id\":\"ely_l\"},{\"DataSource\":\"\",\"Description\":\"ELY Y-alue\",\"Id\":\"ely_y\"},{\"DataSource\":\"\",\"Description\":\"Suomi\",\"Id\":\"finland\"}],\"Id\":2010},{\"AreaTypes\":[{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Taajama\",\"Id\":\"locality\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Kaupunkiseutu\",\"Id\":\"urban_area\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Kunta\",\"Id\":\"municipality\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Seutukunta\",\"Id\":\"sub_region\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Maakunta\",\"Id\":\"region\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"ELY E-alue\",\"Id\":\"ely_e\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"ELY L-alue\",\"Id\":\"ely_l\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"ELY Y-alue\",\"Id\":\"ely_y\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Suomi\",\"Id\":\"finland\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Asemakaavoitettu alue\",\"Id\":\"planned_area\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Asutusalueen tyyppi\",\"Id\":\"neighborhood_type\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Hallinto-oikeus\",\"Id\":\"administrative_law_area\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Kaupan alue\",\"Id\":\"shop_area\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Kaupunkimaaseutuluokitustyyppi\",\"Id\":\"city_rural_area_type\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Kaupunkiseutu-tyyppi\",\"Id\":\"urban_area_type\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Keskusta-alue\",\"Id\":\"city_central_area\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Keskustatyyppi\",\"Id\":\"city_central_type\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Taajama harva/tiheä -tyyppi\",\"Id\":\"locality_density_type\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 250m\",\"Id\":\"grid250m\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 500m\",\"Id\":\"grid500m\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 1km\",\"Id\":\"grid1km\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 2km\",\"Id\":\"grid2km\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 5km\",\"Id\":\"grid5km\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 10km\",\"Id\":\"grid10km\"},{\"DataSource\":\"Tilastokeskus\",\"Description\":\"Ruutu 20km\",\"Id\":\"grid20km\"},{\"DataSource\":\"\",\"Description\":\"Kunta\",\"Id\":\"municipality\"},{\"DataSource\":\"\",\"Description\":\"Seutukunta\",\"Id\":\"sub_region\"},{\"DataSource\":\"\",\"Description\":\"Maakunta\",\"Id\":\"region\"},{\"DataSource\":\"\",\"Description\":\"ELY E-alue\",\"Id\":\"ely_e\"},{\"DataSource\":\"\",\"Description\":\"ELY L-alue\",\"Id\":\"ely_l\"},{\"DataSource\":\"\",\"Description\":\"ELY Y-alue\",\"Id\":\"ely_y\"},{\"DataSource\":\"\",\"Description\":\"Suomi\",\"Id\":\"finland\"}],\"Id\":2011}],\"title\":{\"en\":\"Väkiluku\",\"fi\":\"Väkiluku\",\"sv\":\"Väkiluku\"},\"unit\":\"lkm\",\"years\":[\"2010\",\"2011\"]}";
    // }
    //
    private List<String> GetRegionCategories() {
        ArrayList<String> result = new ArrayList<String>();
        for (RegionDefinition definiton : _regionService.GetAllRegions()) {
            result.add(definiton.getId());
        }
        return result;
    }

}
