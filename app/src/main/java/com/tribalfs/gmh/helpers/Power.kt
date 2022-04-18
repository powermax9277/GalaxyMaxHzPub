package com.tribalfs.gmh.helpers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object Power {
    fun isConnected(context: Context): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
    }
}