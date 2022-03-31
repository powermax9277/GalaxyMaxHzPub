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
import kotlinx.coroutines.ExperimentalCoroutinesApi

internal class PsmChangeHandler private constructor(val appCtx: Context) {

    companion object: SingletonMaker<PsmChangeHandler, Context>(::PsmChangeHandler)

    @ExperimentalCoroutinesApi
    @Synchronized
    @RequiresApi(Build.VERSION_CODES.M)
    fun handle() {
        // Log.d(TAG, "execute called $isPowerSaveModeOn")
        if (isPowerSaveMode.get() == true) {
            if (keepModeOnPowerSaving && isPremium.get()!!) {

                //Use Psm Max Hz
                /*try {
                    UtilDisplayMod.updateVote(appCtx)
                }catch (e: Exception){
                    e.printStackTrace()
                }*/

                prrActive.set( UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRatePsm)
                UtilRefreshRateSt.instance(appCtx).setPrefOrAdaptOrHighRefreshRateMode(null)

                startPipActivityIfS()

            } else {
                UtilRefreshRateSt.instance(appCtx).setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
            }
        }else{
            if (isPremium.get()!!) {
                //Change Max Hz back to Std Prr
                if (!UtilPermSt.instance(appCtx).hasWriteSystemPerm()) {
                    UtilPermSt.instance(appCtx).requestWriteSettings()
                    return
                }
                UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRate.let {
                    prrActive.set(it)
                    UtilRefreshRateSt.instance(appCtx).setRefreshRate(it, UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt)
                }
            }
        }
    }

    fun startPipActivityIfS(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isPowerSaveMode.get() == true) {
                val pipIntent = Intent(appCtx, PipActivity::class.java)
                pipIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appCtx.startActivity(pipIntent)
        }
    }

}