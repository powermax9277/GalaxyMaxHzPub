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

                startPipActivity()

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

    fun startPipActivity(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isPowerSaveMode.get() == true) {
            val pipIntent = Intent(appCtx, PipActivity::class.java)
            pipIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appCtx.startActivity(pipIntent)
        }
    }

    /* fun checkSemPowerModeRefreshRate(){
          try {
             Settings.Global.putString(mContentResolver, SEM_POWER_MODE_REFRESH_RATE, mode)
                     && (
                     if (ModelNumbers.fordableWithHrrExternal.indexOf(mUtilsDeviceInfo.deviceModel) != -1) {
                         Settings.Secure.putString(mContentResolver,
                             UtilsDeviceInfo.REFRESH_RATE_MODE_COVER, mode)
                     } else {
                         true
                     })
                     && (
                     if (CacheSettings.isOnePlus) {
                         val onePlusModeEq = if (mode == UtilsDeviceInfo.REFRESH_RATE_MODE_ALWAYS) UtilsDeviceInfo.ONEPLUS_RATE_MODE_ALWAYS else UtilsDeviceInfo.ONEPLUS_RATE_MODE_SEAMLESS
                         Settings.Global.putString(mContentResolver,
                             UtilsDeviceInfo.ONEPLUS_SCREEN_REFRESH_RATE, onePlusModeEq)
                     } else {
                         true
                     }
                     )
         }catch(_:java.lang.Exception){false}
         MainActivity.SEM_POWER_MODE_REFRESH_RATE
     }*/
}