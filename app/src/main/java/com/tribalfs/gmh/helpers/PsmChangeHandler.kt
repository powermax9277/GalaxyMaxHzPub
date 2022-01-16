package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tribalfs.gmh.PipActivity
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE_STANDARD
import kotlinx.coroutines.ExperimentalCoroutinesApi

internal class PsmChangeHandler(context: Context) {

    companion object: SingletonMaker<PsmChangeHandler, Context>(::PsmChangeHandler){
        // private const val TAG = "PSMChangeHandler"
        // private const val SEM_POWER_MODE_REFRESH_RATE = "sem_power_mode_refresh_rate"
    }

    private val appCtx: Context = context.applicationContext
    //private val mUtilsPrefsGmh by lazy {UtilsPrefsGmhSt(appCtx)}
    private val mUtilsRefreshRate by lazy {UtilsRefreshRateSt.instance(appCtx)}
    //  private val mContentResolver = appCtx.contentResolver


    @ExperimentalCoroutinesApi
    @SuppressLint("NewApi")
    fun handle() {
        // Log.d(TAG, "execute called $isPowerSaveModeOn")
        if (isPowerSaveModeOn.get() == true) {
            if (keepModeOnPowerSaving && isPremium.get()!!) {
                //Use Psm Max Hz
                prrActive.set( mUtilsRefreshRate.mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm)
                mUtilsRefreshRate.setPrefOrAdaptOrHighRefreshRateMode(null)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                   val pipIntent = Intent(appCtx, PipActivity::class.java)
                   pipIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    appCtx.startActivity(pipIntent)
                }
            } else {
                mUtilsRefreshRate.setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
            }
        }else{
            if (isPremium.get()!!) {
                //Change Max Hz back to Std Prr
                if (!UtilsPermSt.instance(appCtx).hasWriteSystemPerm()) {
                    UtilsPermSt.instance(appCtx).requestWriteSettings()
                    return
                }
                mUtilsRefreshRate.mUtilsPrefsGmh.hzPrefMaxRefreshRate.let {
                    prrActive.set(it)
                    mUtilsRefreshRate.setRefreshRate(it, mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefMinHzAdapt)
                }
            }
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