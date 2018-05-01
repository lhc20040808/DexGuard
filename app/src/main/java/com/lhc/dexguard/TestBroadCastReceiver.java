package com.lhc.dexguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 作者：lhc
 * 时间：2018/5/1.
 */

public class TestBroadCastReceiver extends BroadcastReceiver{

    public static final String TAG = "BroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(TAG, "reciver:" + context);
        Log.w(TAG, "reciver:" + context.getApplicationContext());
        Log.w(TAG, "reciver:" + context.getApplicationInfo().className);

    }
}
