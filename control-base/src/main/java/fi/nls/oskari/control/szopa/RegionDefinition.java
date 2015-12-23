package fi.nls.oskari.control.szopa;

public class RegionDefinition {
    private String apiid;
    private String id;
    private RegionType type;

    public enum RegionType {
        ADMINISTRATIVE, FUNCTIONAL, AREA
    }

    public RegionDefinition() {

    }

    public RegionDefinition(String id, String apiid, RegionType type) {
        this.apiid = apiid;
        this.id = id;
        this.type = type;
    }

    public String getApiid() {
        return apiid;
    }

    public void setApiid(String apiid) {
        this.apiid = apiid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RegionType getType() {
        return type;
    }

    public void setType(RegionType type) {
        this.type = type;
    }
}
