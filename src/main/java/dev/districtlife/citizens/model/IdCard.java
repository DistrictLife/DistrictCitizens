package dev.districtlife.citizens.model;

public class IdCard {

    private final String serial;
    private final String ownerUuid;
    private final long issuedAt;
    private final int reissueCount;

    public IdCard(String serial, String ownerUuid, long issuedAt, int reissueCount) {
        this.serial = serial;
        this.ownerUuid = ownerUuid;
        this.issuedAt = issuedAt;
        this.reissueCount = reissueCount;
    }

    public String getSerial() { return serial; }
    public String getOwnerUuid() { return ownerUuid; }
    public long getIssuedAt() { return issuedAt; }
    public int getReissueCount() { return reissueCount; }
}
