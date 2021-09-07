package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.profiles.ProfilesInitializer
import com.tribalfs.gmh.profiles.ProfilesObj.refreshRateModeMap
import com.tribalfs.gmh.resochanger.ResolutionChangeUtilSt
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
class BootCompleteReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val appCtx = context.applicationContext
                bootCompleteChecker(appCtx)
                HzServiceHelperStn.instance(appCtx).startHertz(null, null, null)
                NetSpeedServiceHelperStn.instance(appCtx).runNetSpeed(null)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun bootCompleteChecker(appCtx: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            while(refreshRateModeMap.isEmpty()) {
                delay(250)
            }
            val res = ProfilesInitializer.instance(appCtx).getCurrentResLxw()
            val resName = ResolutionChangeUtilSt.instance(appCtx).getResName(null)
            if (resName == "CQHD+"/* && currentRefreshRateMode.get() == UtilsDeviceInfo.REFRESH_RATE_MODE_STANDARD*/) {
                ResolutionChangeUtilSt.instance(appCtx).changeRes(res)
            }
        }
    }
}