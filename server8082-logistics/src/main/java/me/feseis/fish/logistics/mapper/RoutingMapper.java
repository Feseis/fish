package me.feseis.fish.logistics.mapper;

import java.util.List;
import java.util.Optional;

import me.feseis.fish.logistics.mapper.model.RouteRuleRow;
import me.feseis.fish.logistics.mapper.model.SiteAreaRow;
import me.feseis.fish.logistics.mapper.model.SiteRow;
import me.feseis.fish.logistics.mapper.model.TransportLineRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RoutingMapper {

    @Select("SELECT logistics_site_id, site_code, site_name, site_type, parent_logistics_site_id "
            + "FROM logistics_site WHERE enabled = 1 AND site_type IN (1, 2)")
    List<SiteRow> listEnabledRoutingSites();

    @Select("SELECT logistics_site_id, adcode FROM logistics_site_area")
    List<SiteAreaRow> listSiteAreas();

    /** 按区县反查覆盖它的营业网点；用于下单时的超区校验。 */
    @Select("SELECT logistics_site_id FROM logistics_site_area WHERE adcode = #{adcode}")
    Optional<String> findCoveringSiteId(@Param("adcode") String adcode);

    /** 按 ID 查启用线路；停用或不存在都视为不可用。 */
    @Select("SELECT * FROM transport_line WHERE transport_line_id = #{transportLineId} AND enabled = 1")
    Optional<TransportLineRow> findEnabledLineById(@Param("transportLineId") String transportLineId);

    @Select("SELECT transport_line_id, line_code, origin_site_id, dest_site_id, line_type, transit_hours "
            + "FROM transport_line WHERE enabled = 1")
    List<TransportLineRow> listEnabledLines();

    @Select("SELECT route_rule_id, current_site_id, dest_adcode_prefix, transport_line_id "
            + "FROM route_rule WHERE enabled = 1")
    List<RouteRuleRow> listEnabledRules();
}
