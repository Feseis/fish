package com.feseis.fish.logistics.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 逆地理编码结果：行政区 + 附近候选 POI。
 */
public final class RegeoResult {

    private final String formattedAddress;
    private final String province;
    private final String city;
    private final String district;
    private final String adcode;
    private final List<Poi> pois;

    public RegeoResult(String formattedAddress, String province, String city, String district,
                       String adcode, List<Poi> pois) {
        this.formattedAddress = formattedAddress;
        this.province = province;
        this.city = city;
        this.district = district;
        this.adcode = adcode;
        this.pois = pois == null
                ? Collections.<Poi>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(pois));
    }

    public String getFormattedAddress() {
        return this.formattedAddress;
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

    public String getAdcode() {
        return this.adcode;
    }

    public List<Poi> getPois() {
        return this.pois;
    }
}
