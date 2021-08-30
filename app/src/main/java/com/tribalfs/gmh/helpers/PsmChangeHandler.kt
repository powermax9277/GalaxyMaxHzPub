package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.content.Context
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.ExperimentalCoroutinesApi

internal class PsmChangeHandler(context: Context) {

    companion object: SingletonHolder<PsmChangeHandler, Context>(::PsmChangeHandler){
       // private const val TAG = "PSMChangeHandler"
    }

    private val appCtx: Context = context.applicationContext
    private val mUtilsPrefsGmh = UtilsPrefsGmh(appCtx)
    private val mUtilsRefreshRate = UtilsRefreshRate(appCtx)

    @ExperimentalCoroutinesApi
    @SuppressLint("NewApi")
    fun handle() {
        // Log.d(TAG, "execute called $isPowerSaveModeOn")
        if (isPowerSaveModeOn.get() == true) {
            if (keepModeOnPowerSaving && isPremium.get()!!) {
                //Use Psm Max Hz
                prrActive.set( mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm)
                mUtilsRefreshRate.setPrefOrAdaptOrHighRefreshRateMode(null)
            } else {
                mUtilsRefreshRate.setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
            }
        }else{
            if (isPremium.get()!!) {
                //Change Max Hz back to Std Prr
                mUtilsPrefsGmh.hzPrefMaxRefreshRate.let {
                    prrActive.set(it)
                    mUtilsRefreshRate.setRefreshRate(it)
                }
            }
        }
    }
}