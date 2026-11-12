package com.feseis.fish.logistics.store;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 默认地址本地记录：按账号映射一个 user_address_id，不落库（D-022）。
 */
public final class DefaultAddressStore {

    private static final String PREFS_NAME = "fish_address_prefs";
    private static final String KEY_PREFIX = "default_address_";

    private DefaultAddressStore() {
    }

    public static String get(Context context, String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            return "";
        }
        return prefs(context).getString(KEY_PREFIX + accountId, "");
    }

    public static void set(Context context, String accountId, String userAddressId) {
        if (accountId == null || accountId.isEmpty() || userAddressId == null) {
            return;
        }
        prefs(context).edit().putString(KEY_PREFIX + accountId, userAddressId).apply();
    }

    /** 仅当被删除的条目正是当前默认地址时清空。 */
    public static void clearIfMatches(Context context, String accountId, String userAddressId) {
        if (userAddressId == null || !userAddressId.equals(get(context, accountId))) {
            return;
        }
        prefs(context).edit().remove(KEY_PREFIX + accountId).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
