package com.kronos.olympusfront;

import android.app.Application;

import com.kronos.olympusfront.network.RetrofitClient;

public class OlympusApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
}