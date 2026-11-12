package me.feseis.fish.logistics.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import me.feseis.fish.logistics.mapper.model.HandoverRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface HandoverMapper {

    @Insert("INSERT INTO handover (handover_id, handover_no, transport_line_id, direction, "
            + "from_site_id, to_site_id, vehicle_id, vehicle_plate, driver_staff_id, driver_name, "
            + "operator_name, departed_at, arrived_at, status) VALUES ("
            + "#{handoverId}, #{handoverNo}, #{transportLineId}, #{direction}, "
            + "#{fromSiteId}, #{toSiteId}, #{vehicleId}, #{vehiclePlate}, #{driverStaffId}, #{driverName}, "
            + "#{operatorName}, #{departedAt}, #{arrivedAt}, #{status})")
    void insert(HandoverRow row);

    @Select("SELECT * FROM handover WHERE handover_id = #{handoverId}")
    Optional<HandoverRow> findById(@Param("handoverId") String handoverId);

    @Select("SELECT * FROM handover WHERE handover_id = #{handoverId} FOR UPDATE")
    Optional<HandoverRow> findByIdForUpdate(@Param("handoverId") String handoverId);

    @Update("UPDATE handover SET status = 1, departed_at = #{departedAt} "
            + "WHERE handover_id = #{handoverId} AND status = 0")
    int markDeparted(@Param("handoverId") String handoverId,
                     @Param("departedAt") LocalDateTime departedAt);

    @Update("UPDATE handover SET status = 2, arrived_at = #{arrivedAt} "
            + "WHERE handover_id = #{handoverId} AND status = 1")
    int markArrived(@Param("handoverId") String handoverId,
                    @Param("arrivedAt") LocalDateTime arrivedAt);

    @Insert("INSERT INTO handover_item (handover_item_id, handover_id, waybill_id, loaded_at) "
            + "VALUES (#{handoverItemId}, #{handoverId}, #{waybillId}, #{loadedAt})")
    void insertItem(@Param("handoverItemId") String handoverItemId,
                    @Param("handoverId") String handoverId,
                    @Param("waybillId") String waybillId,
                    @Param("loadedAt") LocalDateTime loadedAt);

    @Select("SELECT waybill_id FROM handover_item WHERE handover_id = #{handoverId} ORDER BY loaded_at ASC")
    List<String> listItemWaybillIds(@Param("handoverId") String handoverId);

    @Select("SELECT COUNT(*) FROM handover_item WHERE handover_id = #{handoverId} AND waybill_id = #{waybillId}")
    int countItem(@Param("handoverId") String handoverId, @Param("waybillId") String waybillId);
}
