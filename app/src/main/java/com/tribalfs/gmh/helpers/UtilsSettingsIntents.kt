package com.tribalfs.gmh.helpers

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
import android.provider.Settings.ACTION_SYNC_SETTINGS
import androidx.annotation.RequiresApi

internal object UtilsSettingsIntents{

    val changeSystemSettingsIntent: Intent
        @RequiresApi(Build.VERSION_CODES.M)
        get() {
            /* i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            i.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP*/
            return Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        }

    val motionSmoothnessSettingsIntent: Intent
        get() {
            val i = Intent()
            i.setClassName(
                "com.android.settings",
                "com.android.settings.Settings\$HighRefreshRatesSettingsActivity"
            )
            return i
        }

    val powerSavingModeSettingsIntent: Intent
        get() {
            /*val i = Intent()
            i.setClassName(
                "com.android.settings",
                "com.android.settings.Settings\$BatterySaverSettingsActivity"
            )*/
            return Intent(ACTION_BATTERY_SAVER_SETTINGS)
        }

    val autoSyncSettingsIntent: Intent
        get() {
            return Intent(ACTION_SYNC_SETTINGS)
        }


    val displaySettingsIntent: Intent
        get() {
            val i = Intent()
            i.action = Settings.ACTION_DISPLAY_SETTINGS
            return i

        }

    val dataUsageSettingsIntent: Intent
        get() {
            val i = Intent()
            i.setClassName(
                "com.android.settings",
                "com.android.settings.Settings\$DataUsageSummaryActivity"
            )
            return i
        }

    val deviceInfoActivity: Intent
        get() {
            val i = Intent()
            i.setClassName(
                "com.android.settings",
                "com.android.settings.Settings\$MyDeviceInfoActivity"
            )
            return i
        }

}