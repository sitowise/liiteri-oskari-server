package fi.nls.oskari.domain.map;

public class UserWmsLayer extends OskariLayer {
    private long userId;
    private int groupId; //this used to be part of OskariLayer, TODO rename here also

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public static final String PREFIX = "userwms_";
}
