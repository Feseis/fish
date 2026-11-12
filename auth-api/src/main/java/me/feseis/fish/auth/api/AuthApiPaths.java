package me.feseis.fish.auth.api;

/**
 * 认证服务内部接口路径。服务间调用一律走 /internal 前缀。
 */
public final class AuthApiPaths {

    public static final String VERIFY_INTERNAL = "/auth/internal/verify";
    public static final String AUDIT_WAYBILL_CANCEL = "/auth/internal/audit/waybill-cancel";
    public static final String AUDIT_STAFF_APPLY = "/auth/internal/audit/staff-apply";

    /** 按手机号查账号，供发证、入职等内部流程取权威姓名与账号状态。 */
    public static final String ACCOUNT_BY_PHONE = "/auth/internal/accounts/by-phone";

    /** 内部建账号，仅用于演示数据初始化，不经过验证码。 */
    public static final String ACCOUNTS_INTERNAL = "/auth/internal/accounts";

    /** 内部授予角色，幂等；入职流程同步调用。 */
    public static final String GRANT_ROLE_INTERNAL = "/auth/internal/roles/grant";

    private AuthApiPaths() {
    }
}
