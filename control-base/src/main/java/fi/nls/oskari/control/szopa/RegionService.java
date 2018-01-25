package fi.nls.oskari.control.szopa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fi.nls.oskari.control.szopa.RegionDefinition.RegionType;

public class RegionService {
    private static class RegionServiceHolder {
        static final RegionService INSTANCE = new RegionService();
    }

    public static RegionService getInstance() {
        return RegionServiceHolder.INSTANCE;
    }

    private HashMap<RegionType, List<RegionDefinition>> _lookup;

    protected RegionService() {
        _lookup = LoadDefinitions();
    }

    private HashMap<RegionType, List<RegionDefinition>> LoadDefinitions() {
        HashMap<RegionType, List<RegionDefinition>> lookup = new HashMap<RegionDefinition.RegionType, List<RegionDefinition>>();
        lookup.put(RegionType.ADMINISTRATIVE, new ArrayList<RegionDefinition>());
        lookup.put(RegionType.FUNCTIONAL, new ArrayList<RegionDefinition>());
        lookup.put(RegionType.AREA, new ArrayList<RegionDefinition>());

        List<RegionDefinition> list = lookup.get(RegionType.ADMINISTRATIVE);

        list.add(new RegionDefinition("KUNTA", "municipality",
                RegionType.ADMINISTRATIVE));
        list.add(new RegionDefinition("SEUTUKUNTA", "sub_region",
                RegionType.ADMINISTRATIVE));
        list.add(new RegionDefinition("MAAKUNTA", "region",
                RegionType.ADMINISTRATIVE));
        list.add(new RegionDefinition("ELY_E", "ely_e",
                RegionType.ADMINISTRATIVE));
        list.add(new RegionDefinition("ELY_Y", "ely_y",
                RegionType.ADMINISTRATIVE));
        list.add(new RegionDefinition("ELY_L", "ely_l",
                RegionType.ADMINISTRATIVE));
        list.add(new RegionDefinition("ADMINISTRATIVELAWAREA",
                "administrative_law_area", RegionType.ADMINISTRATIVE));
        list.add(new RegionDefinition("PALISKUNTA", "reindeer_herding_cooperative",
                RegionType.ADMINISTRATIVE));
        list.add(new RegionDefinition("FINLAND", "finland",
                RegionType.ADMINISTRATIVE));

        list = lookup.get(RegionType.FUNCTIONAL);

        list.add(new RegionDefinition("NEIGHBORHOODTYPE", "neighborhood_type",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("NEIGHBORHOODCLASS",
                "neighborhood_class", RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("PLANNEDAREATYPE", "planned_area_type",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("PLANNEDAREACLASS", "planned_area_class",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("CITYRURALAREATYPE",
                "city_rural_area_type", RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("CITYRURALAREACLASS",
                "city_rural_area_class", RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("URBANAREATYPE", "urban_area_type",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("URBANAREACLASS", "urban_area_class",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("CITYCENTRALTYPE", "city_central_type",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("CITYCENTRALCLASS", "city_central_class",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("SHOPAREATYPE", "shop_area_type",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("SHOPAREACLASS", "shop_area_class",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("LOCALDENSITYTYPE",
                "locality_density_type", RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("LOCALDENSITYCLASS",
                "locality_density_class", RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("LOCALITYRURALTYPE",
                "locality_rural_type", RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("LOCALITYRURALCLASS",
                "locality_rural_class", RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("URBANZONETYPE", "urban_zone_type",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("URBANZONECLASS", "urban_zone_class",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("LOCALSIZETYPE", "locality_size_type",
                RegionType.FUNCTIONAL));
        list.add(new RegionDefinition("LOCALSIZECLASS", "locality_size_class",
                RegionType.FUNCTIONAL));

        list = lookup.get(RegionType.AREA);

        list.add(new RegionDefinition("grid250m", "grid250m", RegionType.AREA));
        list.add(new RegionDefinition("grid500m", "grid500m", RegionType.AREA));
        list.add(new RegionDefinition("grid1km", "grid1km", RegionType.AREA));
        list.add(new RegionDefinition("grid2km", "grid2km", RegionType.AREA));
        list.add(new RegionDefinition("grid5km", "grid5km", RegionType.AREA));
        list.add(new RegionDefinition("grid10km", "grid10km", RegionType.AREA));
        list.add(new RegionDefinition("grid20km", "grid20km", RegionType.AREA));

        return lookup;
    }

    public String getAPIId(String id) {
        String result = null;

        for (RegionDefinition definition : this.GetAllRegions()) {
            if (definition.getId().equals(id)) {
                result = definition.getApiid();
                break;
            }
        }

        return result;
    }

    public RegionType getType(String id) {
        RegionType result = null;

        for (RegionDefinition definition : this.GetAllRegions()) {
            if (definition.getId().equals(id)) {
                result = definition.getType();
                break;
            }
        }

        return result;
    }

    public List<RegionDefinition> GetAllRegions() {
        return GetRegionsOfType(_lookup.keySet().toArray(new RegionType[0]));
    }

    public List<RegionDefinition> GetRegionsOfType(RegionType... types) {
        if (types.length == 1)
            return _lookup.get(types[0]);

        List<RegionDefinition> result = new ArrayList<RegionDefinition>();
        for (RegionType type : types) {
            result.addAll(_lookup.get(type));
        }

        return result;
    }
}
