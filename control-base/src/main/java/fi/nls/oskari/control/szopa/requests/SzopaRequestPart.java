package fi.nls.oskari.control.szopa.requests;

public class SzopaRequestPart extends SzopaRequest {
    private String _requestSpecificParams;
    private String _name;

    public SzopaRequestPart(String name) {
        this(name, "");
    }

    public SzopaRequestPart(String name, String requestSpecificParams) {
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
