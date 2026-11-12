package com.feseis.fish.logistics.model;

import java.io.Serializable;

/**
 * 高德候选 POI。选中后作为地址簿条目的地理锚点。
 */
public final class Poi implements Serializable {

    private final String poiId;
    private final String poiName;
    private final String adcode;
    private final String province;
    private final String city;
    private final String district;
    private final String address;
    private final double lng;
    private final double lat;

    public Poi(String poiId, String poiName, String adcode, String province, String city,
               String district, String address, double lng, double lat) {
        this.poiId = poiId;
        this.poiName = poiName;
        this.adcode = adcode;
        this.province = province;
        this.city = city;
        this.district = district;
        this.address = address;
        this.lng = lng;
        this.lat = lat;
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

    public String getAddress() {
        return this.address;
    }

    public double getLng() {
        return this.lng;
    }

    public double getLat() {
        return this.lat;
    }

    /** 省市区 + POI 名称，供表单摘要展示。 */
    public String summary() {
        StringBuilder builder = new StringBuilder();
        append(builder, this.province);
        append(builder, this.city);
        append(builder, this.district);
        append(builder, this.poiName);
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
