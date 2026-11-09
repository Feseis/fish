package me.feseis.fish.logistics.mapper;

import java.util.List;
import java.util.Optional;

import me.feseis.fish.logistics.mapper.model.UserAddressRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserAddressMapper {

    String JOIN_COLUMNS = "ua.user_address_id, ua.auth_account_id, ua.geo_address_id, ua.contact_name, "
            + "ua.contact_phone, ua.detail_addr, ua.tag, ua.created_at, ua.deleted_at, "
            + "ga.poi_id, ga.poi_name, ga.adcode, ga.province, ga.city, ga.district, ga.lng, ga.lat ";

    @Select("SELECT " + JOIN_COLUMNS
            + "FROM user_address ua JOIN geo_address ga ON ga.geo_address_id = ua.geo_address_id "
            + "WHERE ua.auth_account_id = #{accountId} AND ua.deleted_at IS NULL "
            + "ORDER BY ua.created_at DESC")
    List<UserAddressRow> listByAccount(@Param("accountId") String accountId);

    @Select("SELECT " + JOIN_COLUMNS
            + "FROM user_address ua JOIN geo_address ga ON ga.geo_address_id = ua.geo_address_id "
            + "WHERE ua.user_address_id = #{userAddressId} AND ua.auth_account_id = #{accountId}")
    Optional<UserAddressRow> findOwned(@Param("userAddressId") String userAddressId,
                                       @Param("accountId") String accountId);

    /** 唯一键自然键命中，含已软删行；返回结果需由调用方判断 deletedAt。 */
    @Select("SELECT user_address_id, deleted_at FROM user_address "
            + "WHERE auth_account_id = #{accountId} AND geo_address_id = #{geoAddressId} "
            + "AND detail_addr = #{detailAddr} AND contact_name = #{contactName} "
            + "AND contact_phone = #{contactPhone}")
    Optional<UserAddressRow> findNatural(@Param("accountId") String accountId,
                                         @Param("geoAddressId") String geoAddressId,
                                         @Param("detailAddr") String detailAddr,
                                         @Param("contactName") String contactName,
                                         @Param("contactPhone") String contactPhone);

    @Insert("INSERT INTO user_address (user_address_id, auth_account_id, geo_address_id, contact_name, "
            + "contact_phone, detail_addr, tag) VALUES (#{userAddressId}, #{accountId}, #{geoAddressId}, "
            + "#{contactName}, #{contactPhone}, #{detailAddr}, #{tag})")
    void insert(@Param("userAddressId") String userAddressId,
                @Param("accountId") String accountId,
                @Param("geoAddressId") String geoAddressId,
                @Param("contactName") String contactName,
                @Param("contactPhone") String contactPhone,
                @Param("detailAddr") String detailAddr,
                @Param("tag") String tag);

    /** 软删行复活：清空 deleted_at 并覆盖标签，created_at 保留原值。 */
    @Update("UPDATE user_address SET deleted_at = NULL, tag = #{tag} WHERE user_address_id = #{userAddressId}")
    void revive(@Param("userAddressId") String userAddressId, @Param("tag") String tag);

    @Update("UPDATE user_address SET detail_addr = #{detailAddr}, contact_name = #{contactName}, "
            + "contact_phone = #{contactPhone}, tag = #{tag} "
            + "WHERE user_address_id = #{userAddressId} AND auth_account_id = #{accountId} AND deleted_at IS NULL")
    int update(@Param("userAddressId") String userAddressId,
               @Param("accountId") String accountId,
               @Param("detailAddr") String detailAddr,
               @Param("contactName") String contactName,
               @Param("contactPhone") String contactPhone,
               @Param("tag") String tag);

    /** 编辑时换了 POI：把条目挂到新的 geo_address 缓存行。 */
    @Update("UPDATE user_address SET geo_address_id = #{geoAddressId} "
            + "WHERE user_address_id = #{userAddressId} AND auth_account_id = #{accountId}")
    int moveGeoAddress(@Param("userAddressId") String userAddressId,
                       @Param("accountId") String accountId,
                       @Param("geoAddressId") String geoAddressId);

    @Update("UPDATE user_address SET deleted_at = NOW() "
            + "WHERE user_address_id = #{userAddressId} AND auth_account_id = #{accountId} AND deleted_at IS NULL")
    int softDelete(@Param("userAddressId") String userAddressId, @Param("accountId") String accountId);
}
