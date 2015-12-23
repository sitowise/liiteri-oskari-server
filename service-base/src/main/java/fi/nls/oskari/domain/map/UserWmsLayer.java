package fi.nls.oskari.domain.map;

public class UserWmsLayer extends OskariLayer {
    private long userId;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public static final String PREFIX = "userwms_";
}
