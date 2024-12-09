package com.example.ideality.activities;

import android.app.Application;

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

}