package com.feseis.fish.logistics.api;

import android.content.Context;

/**
 * 物流接口唯一出口。地址由认证服务主机派生，端口固定 8082。
 */
public final class LogisticsApiProvider {

    private static LogisticsApi instance;

    private LogisticsApiProvider() {
    }

    public static LogisticsApi get(Context context) {
        if (instance == null) {
            instance = new HttpLogisticsApi(context.getApplicationContext());
        }
        return instance;
    }
}
