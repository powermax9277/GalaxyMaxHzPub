package com.tribalfs.gmh.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.helpers.UtilRefreshRateSt
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt
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
                    val reso = UtilsDeviceInfoSt.instance(appCtx).getDisplayResolution()
                    val resName = ResolutionChangeUtil(appCtx).getResName(null)
                    if (resName == "CQHD+") ResolutionChangeUtil(appCtx).changeRes(reso)
                    UtilRefreshRateSt.instance(appCtx).requestListeningAllTiles()
                }
            }
        }
    }

}