package com.tribalfs.gmh.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveMode
import com.tribalfs.gmh.helpers.PsmChangeHandler
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
                    while (refreshRateModeMap.isEmpty() && isPowerSaveMode.get() != true && isPowerSaveMode.get() != false) {
                        delay(500)
                    }

                    val reso = UtilsDeviceInfoSt.instance(context.applicationContext).getDisplayResolution()
                    val resName = ResolutionChangeUtil(context.applicationContext).getResName(null)
                    if (resName == "CQHD+") ResolutionChangeUtil(context.applicationContext).changeRes(reso)

                    PsmChangeHandler.instance(context.applicationContext).handle()

                    UtilRefreshRateSt.instance(context.applicationContext).requestListeningAllTiles()

                }
            }
        }
    }

}

