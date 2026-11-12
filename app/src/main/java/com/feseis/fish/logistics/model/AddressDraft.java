package com.feseis.fish.logistics.model;

/**
 * 保存地址簿条目的请求体，字段与后端 UserAddressSaveReqVO 对应。
 */
public final class AddressDraft {

    private final String poiId;
    private final String poiName;
    private final String adcode;
    private final String province;
    private final String city;
    private final String district;
    private final double lng;
    private final double lat;
    private final String contactName;
    private final String contactPhone;
    private final String detailAddr;
    private final String tag;

    public AddressDraft(String poiId, String poiName, String adcode, String province, String city,
                        String district, double lng, double lat, String contactName,
                        String contactPhone, String detailAddr, String tag) {
        this.poiId = poiId;
        this.poiName = poiName;
        this.adcode = adcode;
        this.province = province;
        this.city = city;
        this.district = district;
        this.lng = lng;
        this.lat = lat;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.detailAddr = detailAddr;
        this.tag = tag;
    }

    public String getPoiId() {
        return this.poiId;
    }

    public String getPoiName() {
        return this.poiName;
    }

    public String getAdcode() {
        return this.adcode;
    }

    public String getProvince() {
        return this.province;
    }

    public String getCity() {
        return this.city;
    }

    public String getDistrict() {
        return this.district;
    }

    public double getLng() {
        return this.lng;
    }

    public double getLat() {
        return this.lat;
    }

    public String getContactName() {
        return this.contactName;
    }

    public String getContactPhone() {
        return this.contactPhone;
    }

    public String getDetailAddr() {
        return this.detailAddr;
    }

    public String getTag() {
        return this.tag;
    }
}
