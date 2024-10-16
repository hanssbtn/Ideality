package com.example.ideality.activities;

import android.app.Application;
import android.content.Context;

public class IdealityApplication extends Application {

    private static IdealityApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static IdealityApplication getInstance() {
        return instance;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}