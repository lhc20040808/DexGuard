package com.lhc.dexguard;

import android.app.Application;
import android.util.Log;

/**
 * 作者：lhc
 * 时间：2018/5/1.
 */

public class OriginApplication extends Application {
    private static final String TAG = "Application";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "onCreate");
    }
}
