package fi.nls.oskari.control.twowaystats.requests;

public class TwowayRequestPart extends TwowayRequest {
    private String _requestSpecificParams;
    private String _name;

    public TwowayRequestPart(String name) {
        this(name, "");
    }

    public TwowayRequestPart(String name, String requestSpecificParams) {
        _name = name;
        _requestSpecificParams = requestSpecificParams;
    }

    public void setRequestSpecificParams(String requestSpecificParams) {
        _requestSpecificParams = requestSpecificParams;
    }

    @Override
    public String getRequestSpecificParams() {
        return _requestSpecificParams;
    }

    @Override
    public String getName() {
        return _name;
    }
}
