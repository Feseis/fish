package me.feseis.fish.logistics.mapper;

import java.util.List;
import java.util.Optional;

import me.feseis.fish.logistics.mapper.model.SiteOptionRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SiteMapper {

    /** 全部网点，不过滤启用状态与类型，供内部工具做选择列表。 */
    @Select("SELECT logistics_site_id AS site_id, site_code, site_name, site_type, enabled "
            + "FROM logistics_site ORDER BY site_code")
    List<SiteOptionRow> listAll();

    @Select("SELECT logistics_site_id AS site_id, site_code, site_name, site_type, enabled "
            + "FROM logistics_site WHERE site_code = #{siteCode}")
    Optional<SiteOptionRow> findByCode(@Param("siteCode") String siteCode);

    @Select("SELECT logistics_site_id AS site_id, site_code, site_name, site_type, enabled "
            + "FROM logistics_site WHERE logistics_site_id = #{siteId}")
    Optional<SiteOptionRow> findById(@Param("siteId") String siteId);
}
