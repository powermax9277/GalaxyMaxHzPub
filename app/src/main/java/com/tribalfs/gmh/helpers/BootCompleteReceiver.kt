package com.tribalfs.gmh.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.profiles.ProfilesObj.refreshRateModeMap
import com.tribalfs.gmh.resochanger.ResolutionChangeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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


    private fun bootCompleteChecker(appCtx: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            while(refreshRateModeMap.isEmpty()) {
                delay(250)
            }
            val resoChangeUtil = ResolutionChangeUtil(appCtx)
            val reso = resoChangeUtil.mUtilsRefreshRate.mUtilsDeviceInfo.getDisplayResolution()
            val resName = resoChangeUtil.getResName(null)
            if (resName == "CQHD+"/* && currentRefreshRateMode.get() == UtilsDeviceInfo.REFRESH_RATE_MODE_STANDARD*/) {
                resoChangeUtil.changeRes(reso)
                delay(500)
            }
            resoChangeUtil.mUtilsRefreshRate.requestListeningAllTiles()
        }
    }


}