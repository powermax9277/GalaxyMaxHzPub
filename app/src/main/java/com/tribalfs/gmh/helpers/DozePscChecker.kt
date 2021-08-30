package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager.*
import android.provider.Settings
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.BATTERY_SAVER_CONSTANTS

object DozePSCChecker {
    @SuppressLint("InlinedApi")
    @Synchronized
    internal fun check(context:Context, enable: Boolean, updateIfDisable: Boolean) {
        if (enable) {
            var msc: String? = Settings.Global.getString(context.contentResolver, BATTERY_SAVER_CONSTANTS)
            var update = false

            if (msc?.contains("quick_doze_enabled=true") != true) {
                msc = if (msc?.contains("quick_doze_enabled=false") == true) {
                    msc.replace("quick_doze_enabled=false", "quick_doze_enabled=true")
                } else {
                    if (msc != "null" && !msc.isNullOrEmpty()) "$msc,quick_doze_enabled=true" else "quick_doze_enabled=true"
                }
                update = true
            }

            if (!msc.contains("fullbackup_deferred=true")) {
                msc = if (msc.contains("fullbackup_deferred=false")) {
                    msc.replace("fullbackup_deferred=false", "fullbackup_deferred=true")
                } else {
                    "$msc,fullbackup_deferred=true"
                }
                update = true
            }

            if (!msc.contains("gps_mode=$LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF")) {
               if (msc.contains("gps_mode=")) {
                    listOf(LOCATION_MODE_NO_CHANGE,
                        LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF,
                        LOCATION_MODE_FOREGROUND_ONLY,
                        LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF).forEach {
                        msc = msc!!.replace("gps_mode=$it", "gps_mode=$LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF")
                    }
                } else {
                   msc =  "$msc,gps_mode=$LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF"
                }
                update = true
            }

            if (update) {
                try {
                    Settings.Global.putString(context.contentResolver, BATTERY_SAVER_CONSTANTS, msc!!)
                }catch(_:Exception){}
            }

        }else{
            if (updateIfDisable){
                var msc: String? = Settings.Global.getString(context.contentResolver, BATTERY_SAVER_CONSTANTS)
                msc = if (msc.isNullOrEmpty() || msc == "null"){
                    "null"
                }else{
                    var tempMsc:String? = null
                    msc!!.split(",").forEach {
                        if (it != "quick_doze_enabled=true"
                            && it != "fullbackup_deferred=true"
                            && it != "gps_mode=$LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF"
                        ){
                            tempMsc += (if (tempMsc.isNullOrEmpty()) tempMsc else ",$tempMsc" )
                        }
                    }
                    tempMsc?:"null"
                }
                try {
                    Settings.Global.putString(context.contentResolver, BATTERY_SAVER_CONSTANTS, msc!!)
                }catch(_:Exception){}

            }
        }
    }
}