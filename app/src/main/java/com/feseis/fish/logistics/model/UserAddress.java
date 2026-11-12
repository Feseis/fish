package com.feseis.fish.logistics.model;

import java.io.Serializable;

/**
 * 地址簿条目，字段与后端 UserAddressRespVO 对应。
 */
public final class UserAddress implements Serializable {

    private final String userAddressId;
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
    private final String createdAt;

    public UserAddress(String userAddressId, String poiId, String poiName, String adcode,
                       String province, String city, String district, double lng, double lat,
                       String contactName, String contactPhone, String detailAddr, String tag,
                       String createdAt) {
        this.userAddressId = userAddressId;
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
        this.createdAt = createdAt;
    }

    public String getUserAddressId() {
        return this.userAddressId;
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

    public String getCreatedAt() {
        return this.createdAt;
    }

    /** 完整地址文本：省市区 + POI + 门牌。 */
    public String fullAddress() {
        StringBuilder builder = new StringBuilder();
        append(builder, this.province);
        append(builder, this.city);
        append(builder, this.district);
        append(builder, this.poiName);
        append(builder, this.detailAddr);
        return builder.toString();
    }

    private static void append(StringBuilder builder, String part) {
        if (part == null || part.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(part);
    }
}
