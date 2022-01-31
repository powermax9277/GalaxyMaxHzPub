package com.tribalfs.gmh.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.profiles.ProfilesObj.refreshRateModeMap
import com.tribalfs.gmh.resochanger.ResolutionChangeUtil
import kotlinx.coroutines.*


@ExperimentalCoroutinesApi
class BootCompleteReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                CoroutineScope(Dispatchers.Main).launch {
                    while (refreshRateModeMap.isEmpty()) {
                        delay(250)
                    }
                    val appCtx = context.applicationContext
                    val resoChangeUtil = ResolutionChangeUtil(appCtx)
                    val reso = resoChangeUtil.mUtilsRefreshRate.mUtilsDeviceInfo.getDisplayResolution()
                    val resName = resoChangeUtil.getResName(null)
                    if (resName == "CQHD+") resoChangeUtil.changeRes(reso)
                    HzServiceHelperStn.instance(appCtx).switchHz()
                    NetSpeedServiceHelperStn.instance(appCtx).updateNetSpeed()
                    resoChangeUtil.mUtilsRefreshRate.requestListeningAllTiles()
                }
            }
        }
    }

}