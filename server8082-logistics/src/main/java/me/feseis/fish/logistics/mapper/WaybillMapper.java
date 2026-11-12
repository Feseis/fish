package me.feseis.fish.logistics.mapper;

import java.util.List;
import java.util.Optional;

import me.feseis.fish.logistics.mapper.model.WaybillRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface WaybillMapper {

    @Insert("INSERT INTO waybill (waybill_id, waybill_no, auth_account_id, "
            + "sender_name, sender_phone, sender_province, sender_city, sender_district, sender_detail_addr, "
            + "sender_adcode, sender_poi_id, sender_poi_name, sender_lng, sender_lat, "
            + "receiver_name, receiver_phone, receiver_province, receiver_city, receiver_district, receiver_detail_addr, "
            + "receiver_adcode, receiver_poi_id, receiver_poi_name, receiver_lng, receiver_lat, "
            + "item_name, item_category_id, item_weight, declared_value, "
            + "freight_amount, insured_fee, total_amount, price_rule_id, insurance_rule_id, pay_method, status, remark) VALUES ("
            + "#{waybillId}, #{waybillNo}, #{authAccountId}, "
            + "#{senderName}, #{senderPhone}, #{senderProvince}, #{senderCity}, #{senderDistrict}, #{senderDetailAddr}, "
            + "#{senderAdcode}, #{senderPoiId}, #{senderPoiName}, #{senderLng}, #{senderLat}, "
            + "#{receiverName}, #{receiverPhone}, #{receiverProvince}, #{receiverCity}, #{receiverDistrict}, #{receiverDetailAddr}, "
            + "#{receiverAdcode}, #{receiverPoiId}, #{receiverPoiName}, #{receiverLng}, #{receiverLat}, "
            + "#{itemName}, #{itemCategoryId}, #{itemWeight}, #{declaredValue}, "
            + "#{freightAmount}, #{insuredFee}, #{totalAmount}, #{priceRuleId}, #{insuranceRuleId}, #{payMethod}, #{status}, #{remark})")
    void insert(WaybillRow row);

    /** 单表查询、无 join，列与 Row 属性一一对应，直接用 * 保持可读。 */
    @Select("SELECT * FROM waybill WHERE waybill_id = #{waybillId} AND auth_account_id = #{accountId}")
    Optional<WaybillRow> findOwned(@Param("waybillId") String waybillId,
                                   @Param("accountId") String accountId);

    @Select("SELECT * FROM waybill WHERE waybill_id = #{waybillId}")
    Optional<WaybillRow> findById(@Param("waybillId") String waybillId);

    @Select("SELECT * FROM waybill WHERE waybill_id = #{waybillId} FOR UPDATE")
    Optional<WaybillRow> findByIdForUpdate(@Param("waybillId") String waybillId);

    @Update("UPDATE waybill SET status = #{target} WHERE waybill_id = #{waybillId} AND status = #{expected}")
    int updateStatus(@Param("waybillId") String waybillId,
                     @Param("expected") int expected,
                     @Param("target") int target);

    @Update("UPDATE waybill SET status = 2, picked_at = #{pickedAt}, current_site_id = #{siteId} "
            + "WHERE waybill_id = #{waybillId} AND status = 1 AND current_site_id IS NULL")
    int markPicked(@Param("waybillId") String waybillId,
                   @Param("siteId") String siteId,
                   @Param("pickedAt") java.time.LocalDateTime pickedAt);

    @Update("UPDATE waybill SET current_site_id = #{siteId} WHERE waybill_id = #{waybillId}")
    int updateCurrentSite(@Param("waybillId") String waybillId,
                          @Param("siteId") String siteId);

    @Update("UPDATE waybill SET status = 5, delivered_at = #{deliveredAt}, "
            + "sign_type = #{signType}, sign_name = #{signName}, sign_phone = #{signPhone} "
            + "WHERE waybill_id = #{waybillId} AND status = #{expected}")
    int markDelivered(@Param("waybillId") String waybillId,
                      @Param("expected") int expected,
                      @Param("signType") int signType,
                      @Param("signName") String signName,
                      @Param("signPhone") String signPhone,
                      @Param("deliveredAt") java.time.LocalDateTime deliveredAt);

    @Select("SELECT * FROM waybill WHERE auth_account_id = #{accountId} "
            + "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<WaybillRow> listByAccount(@Param("accountId") String accountId,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);
}
