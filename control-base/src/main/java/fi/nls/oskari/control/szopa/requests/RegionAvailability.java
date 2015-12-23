package fi.nls.oskari.control.szopa.requests;

public class RegionAvailability extends SzopaRequest {

    @Override
    public String getName() {
        return "availability";
    }

    @Override
    public String getRequestSpecificParams() {
        return "/areaTypes/" + getAreaType() + "/availability";
    }

    @Override
    public boolean getUseCache() {
        return true;
    }

}
