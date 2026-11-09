package me.feseis.fish.logistics.mapper;

import java.util.Optional;

import me.feseis.fish.logistics.mapper.model.GeoAddressRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface GeoAddressMapper {

    @Select("SELECT geo_address_id, poi_id, poi_name, adcode, province, city, district, lng, lat "
            + "FROM geo_address WHERE poi_id = #{poiId}")
    Optional<GeoAddressRow> findByPoiId(@Param("poiId") String poiId);

    @Insert("INSERT INTO geo_address (geo_address_id, poi_id, poi_name, adcode, province, city, district, lng, lat) "
            + "VALUES (#{geoAddressId}, #{poiId}, #{poiName}, #{adcode}, #{province}, #{city}, #{district}, #{lng}, #{lat})")
    void insert(@Param("geoAddressId") String geoAddressId,
                @Param("poiId") String poiId,
                @Param("poiName") String poiName,
                @Param("adcode") String adcode,
                @Param("province") String province,
                @Param("city") String city,
                @Param("district") String district,
                @Param("lng") java.math.BigDecimal lng,
                @Param("lat") java.math.BigDecimal lat);

    @Update("UPDATE geo_address SET poi_name = #{poiName}, adcode = #{adcode}, province = #{province}, "
            + "city = #{city}, district = #{district}, lng = #{lng}, lat = #{lat} "
            + "WHERE geo_address_id = #{geoAddressId}")
    void update(@Param("geoAddressId") String geoAddressId,
                @Param("poiName") String poiName,
                @Param("adcode") String adcode,
                @Param("province") String province,
                @Param("city") String city,
                @Param("district") String district,
                @Param("lng") java.math.BigDecimal lng,
                @Param("lat") java.math.BigDecimal lat);
}
