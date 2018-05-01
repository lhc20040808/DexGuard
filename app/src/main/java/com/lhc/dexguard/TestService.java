package com.lhc.dexguard;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * 作者：lhc
 * 时间：2018/5/1.
 */

public class TestService extends Service {

    public static final String TAG = "Service";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "service:" + getApplication());
        Log.w(TAG, "service:" + getApplicationContext());
        Log.w(TAG, "service:" + getApplicationInfo().className);

    }
}
