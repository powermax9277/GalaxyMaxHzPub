package com.tribalfs.gmh.helpers

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.PipActivity
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveMode
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


internal class PsmChangeHandler private constructor(val appCtx: Context) {

    companion object: SingletonMaker<PsmChangeHandler, Context>(::PsmChangeHandler)

    @Synchronized
    @RequiresApi(Build.VERSION_CODES.M)
    fun handle() {
        if (isPowerSaveMode.get() == true) {
            if (keepModeOnPowerSaving && isPremium.get()!!) {
                prrActive.set( UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRatePsm)
                UtilRefreshRateSt.instance(appCtx).setPrefOrAdaptOrHighRefreshRateMode(null)
                startPipActivityIfS()
            } else {
                UtilRefreshRateSt.instance(appCtx).setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
            }
        }else{
            if (isPremium.get()!!) {
                if (!UtilPermSt.instance(appCtx).hasWriteSystemPerm()) {
                    UtilPermSt.instance(appCtx).requestWriteSettings()
                    return
                }
                //Change Max Hz back to Std Prr
                UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRate.let {
                    prrActive.set(it)
                    if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt
                        > UtilsDeviceInfoSt.instance(appCtx).regularMinHz
                    ) {
                        UtilRefreshRateSt.instance(appCtx)
                            .setRefreshRate(it, UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt)
                    }else{
                        UtilRefreshRateSt.instance(appCtx).setRefreshRate(it,0)
                    }
                }
            }
        }
    }


    fun startPipActivityIfS(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isPowerSaveMode.get() == true) {

            if (UtilsDeviceInfoSt.instance(appCtx).isGoogleMapsTrickDevice){
                CoroutineScope(Dispatchers.IO).launch {
                    val intent = appCtx.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        appCtx.startActivity(intent)
                    }
                    delay(1000)
                    val startMain = Intent(Intent.ACTION_MAIN)
                    startMain.addCategory(Intent.CATEGORY_HOME)
                    startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    appCtx.startActivity(startMain)
                }
            }else {
                if (UtilPermSt.instance(appCtx).hasPipPermission()) {
                    val pipIntent = Intent(appCtx, PipActivity::class.java)
                    pipIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    appCtx.startActivity(pipIntent)
                }
            }
        }
    }

}