package com.tribalfs.gmh.helpers

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.profiles.InternalProfiles
import com.tribalfs.gmh.profiles.ProfilesInitializer
import com.tribalfs.gmh.profiles.ProfilesObj.loadComplete
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntCurMod
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.STANDARD_REFRESH_RATE_HZ
import com.tribalfs.gmh.helpers.UtilsPermSt.Companion.CHANGE_SETTINGS
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.*
import kotlin.math.max


class UtilsChangeMaxHzSt private constructor(private val appCtx: Context) {

    companion object : SingletonHolder<UtilsChangeMaxHzSt, Context>(::UtilsChangeMaxHzSt){
        const val CHANGE_RES = -15
        const val CHANGE_MODE = -16
        const val POWER_SAVINGS = -17
        const val NO_CONFIG_LOADED = -18
        // private const val TAG = "UtilsChangeMaxHz"
    }

    //  private val appCtx = context.applicationContext
    private var isModeUpdated = false
    private val mUtilsDisplayInfo by lazy {UtilsDeviceInfo(appCtx)}
    private val mUtilsPrefsGmh by lazy { UtilsPrefsGmh(appCtx) }
    private val mUtilsRefreshRate by lazy { UtilsRefreshRate(appCtx) }

    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun changeMaxHz(maxHzToApply: Int?): Int = withContext(Dispatchers.IO){

        val per = if (UtilsPermSt.instance(appCtx).hasWriteSystemPerm()) PERMISSION_GRANTED else CHANGE_SETTINGS

        if (per == CHANGE_SETTINGS) {
            return@withContext CHANGE_SETTINGS
        }

        //if (mUtilsDisplayInfo.deviceIsSamsung) {

        if (currentRefreshRateMode.get() == REFRESH_RATE_MODE_STANDARD) {
            if (hasWriteSecureSetPerm) {
                if (ProfilesInitializer.instance(appCtx).getResoHighestHzForAllMode(null)
                    > STANDARD_REFRESH_RATE_HZ
                ) {
                    isModeUpdated = true
                    withContext(Dispatchers.IO) {
                        mUtilsRefreshRate.setPrefOrAdaptOrHighRefreshRateMode(null)
                        delay(400)
                    }
                } else {
                    if (loadComplete) {
                        //we are sure that resolution has no high refresh rate support
                        return@withContext CHANGE_RES

                    } else {
                        //hasWriteSecure perm so test if current reso supports adaptive or high
                        withContext(Dispatchers.IO) {
                            mUtilsRefreshRate.setPrefOrAdaptOrHighRefreshRateMode(null)
                            delay(400)//don't remove
                        }

                        if (mUtilsDisplayInfo.getMaxHzForCurrentReso(null).toInt() <= STANDARD_REFRESH_RATE_HZ) {
                            //restore to standard if not high refresh rate
                            mUtilsRefreshRate.setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
                            return@withContext CHANGE_RES

                        } else {
                            //has high refresh rate per test so retain mode
                            isModeUpdated = true

                            if (!loadComplete) {
                                InternalProfiles.load(true, appCtx)
                            }
                        }
                    }
                }
            } else {
                //standard mode and no writeSecure perm
                if (mUtilsDisplayInfo.isPowerSavingsModeOn()){
                    return@withContext POWER_SAVINGS
                }

                if (!loadComplete) {
                    //not configComplete so not sure if res supports high, so prompt change mode
                    return@withContext CHANGE_MODE
                } else {
                    //  Log.d(TAG, "getHighestHzForAllMode called from UtilsChangeMaxHz")
                    if (ProfilesInitializer.instance(appCtx).getResoHighestHzForAllMode(null).toInt() > STANDARD_REFRESH_RATE_HZ) {
                        //no writeSecure perm but profile is complete, so we are sure that reso support high/adapt
                        return@withContext CHANGE_MODE
                    } else{
                        //configComplete so we are sure that reso not support high/adapt
                        return@withContext CHANGE_RES
                    }
                }
            }
        } else {
            //Adaptive or High
            // To make sure current config already added
            if (!loadComplete) {
                InternalProfiles.load(true, appCtx)
                delay(250)
            }

            try {
                if (ProfilesInitializer.instance(appCtx).getResoHighestHzForCurrentMode(null, null)
                        .toInt()
                    <= STANDARD_REFRESH_RATE_HZ
                ) {
                    return@withContext CHANGE_RES
                }
            }catch(_: Exception){
                return@withContext NO_CONFIG_LOADED
            }
        }
        // }

        if (supportedHzIntCurMod != null) {
            val maxHzToApplyFinal :Int =
                if (maxHzToApply == null) {
                    val idx = supportedHzIntCurMod!!.indexOfFirst { it == mUtilsRefreshRate.getPeakRefreshRate() }
                    max(if (idx >= supportedHzIntCurMod!!.size - 1) { supportedHzIntCurMod!![0] } else { supportedHzIntCurMod!![idx + 1]}, mUtilsPrefsGmh.gmhPrefMinHzForToggle)
                } else {
                    if (supportedHzIntCurMod!!.indexOfFirst { it == maxHzToApply } != -1) {
                        maxHzToApply
                    }else{
                        mUtilsRefreshRate.getPeakRefreshRate()
                    }
                }

            if (isScreenOn) {
                mUtilsRefreshRate.setRefreshRate(maxHzToApplyFinal.coerceAtLeast(lowestHzCurMode))
            }

            prrActive.set(maxHzToApplyFinal.coerceAtLeast(lowestHzCurMode))
            if (isPremium.get()!! && isPowerSaveModeOn.get() == true){// && keepModeOnPowerSaving) {
                mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm = maxHzToApplyFinal
            }else{
                mUtilsPrefsGmh.hzPrefMaxRefreshRate = maxHzToApplyFinal
            }

            delay(200)

            return@withContext per

        }else{
            return@withContext NO_CONFIG_LOADED
        }//Log.i(TAG, "UtilsChangeMax: hzSet is empty")

    }

}

