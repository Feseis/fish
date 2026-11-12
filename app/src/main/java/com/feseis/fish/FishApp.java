package com.feseis.fish;

import android.app.Application;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.MapsInitializer;
import com.feseis.fish.auth.api.AuthApiProvider;

public class FishApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 高德要求：地图与定位对象创建前完成隐私合规声明；演示项目按用户已同意处理
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);
        AuthApiProvider.init(this);
    }
}
