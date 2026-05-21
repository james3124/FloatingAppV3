package com.floatingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.floatingapp.utils.AppPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        AppPreferences prefs = new AppPreferences(context);
        if (prefs.getBoolean(AppPreferences.KEY_AUTO_START, false)
                && Settings.canDrawOverlays(context)) {
            Intent service = new Intent(context, FloatingWindowService.class);
            service.setAction(FloatingWindowService.ACTION_START);
            ContextCompat.startForegroundService(context, service);
        }
    }
}

