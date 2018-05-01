package com.lhc.dexguard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
    public static final String TAG = "Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.w(TAG, "Activity:" + getApplication());
        Log.w(TAG, "Activity:" + getApplicationContext());
        Log.w(TAG, "Activity:" + getApplicationInfo().className);

        startService(new Intent(this, TestService.class));

        Intent intent = new Intent("com.lhc.dexguard.broadcast.test");
        intent.setComponent(new ComponentName(getPackageName(), TestBroadCastReceiver.class.getName()));
        sendBroadcast(intent);
    }
}
