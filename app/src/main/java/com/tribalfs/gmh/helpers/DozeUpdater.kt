package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import java.lang.Integer.max
import java.lang.Integer.min

object DozeUpdater {
    internal val mwInterval = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120)

    private const val DOZE_VAL_MAIN =
        "light_idle_maintenance_max_budget=${30*1000}," +
                "idle_pending_to=${5*1000}," +
                "max_idle_pending_to=${30*1000}," +
                "min_deep_maintenance_time=${5*1000}," +
                "quick_doze_delay_to=${25*1000}," +
                "light_after_inactive_to=${0*1000}," +
                "light_pre_idle_to=${15*1000}," +
                "motion_inactive_to=${4*24*60*60*1000}," +
                "light_idle_factor=2," +
                "idle_factor=2"

    @SuppressLint("NewApi")
    fun getDozeVal(dozeMw: Int): String{
        val dozValApx = "light_idle_to=${dozeMw*60*1000}," +
                "idle_after_inactive_to=${dozeMw*60*1000}," +
                "light_max_idle_to=${max(dozeMw*5,4*60)*60*1000}," +
                "light_idle_maintenance_min_budget=${1000*min(dozeMw/5,5)}," +
                "inactive_to=${dozeMw*60*1000}," +
                "idle_to=${dozeMw*60*1000}," +
                "max_idle_to=${max(dozeMw*5,4*60)*60*1000}"
        return "$DOZE_VAL_MAIN,$dozValApx"
    }

    @Synchronized
    fun Context.updateDozValues(enable:Boolean, dozeMw: Int?) {
        (if (!enable || dozeMw == null) "null" else getDozeVal(if (dozeMw == 0) 5*24*60 else dozeMw)).let{
            if (hasWriteSecureSetPerm){
                Settings.Global.putString(
                    applicationContext.contentResolver,
                    DEVICE_IDLE_CONSTANTS,
                    it
                )
            }
        }

    }
}