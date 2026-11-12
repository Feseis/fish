package me.feseis.fish.auth.api.dto;

/**
 * 授予角色出参。granted=false 表示此前已授予，属幂等结果。
 */
public record RoleGrantRespDTO(
        String accountId,
        Integer roleId,
        boolean granted
) {
}
