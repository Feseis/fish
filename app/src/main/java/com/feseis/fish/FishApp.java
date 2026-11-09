package com.feseis.fish;

import android.app.Application;

import com.feseis.fish.auth.api.AuthApiProvider;

public class FishApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AuthApiProvider.init(this);
    }
}
