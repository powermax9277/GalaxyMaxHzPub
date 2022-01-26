package com.tribalfs.gmh.helpers

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntCurMod
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.STANDARD_REFRESH_RATE_HZ
import com.tribalfs.gmh.helpers.UtilsPermSt.Companion.CHANGE_SETTINGS
import com.tribalfs.gmh.profiles.InternalProfiles
import com.tribalfs.gmh.profiles.ProfilesObj.loadComplete
import kotlinx.coroutines.*
import kotlin.math.max


class UtilsChangeMaxHz (private val appCtx: Context) {

    companion object{
        const val CHANGE_RES = -15
        const val CHANGE_MODE = -16
        const val POWER_SAVINGS = -17
        const val NO_CONFIG_LOADED = -18
        // private const val TAG = "UtilsChangeMaxHz"
    }

    //  private val appCtx = context.applicationContext
    private var isModeUpdated = false
    internal val mUtilsRefreshRate by lazy { UtilsRefreshRateSt.instance(appCtx) }

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
                if (mUtilsRefreshRate.getResoHighestHzForAllMode(null)
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

                        if (mUtilsRefreshRate.mUtilsDeviceInfo.getMaxHzForCurrentReso(null).toInt() <= STANDARD_REFRESH_RATE_HZ) {
                            //restore to standard if not high refresh rate
                            mUtilsRefreshRate.setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
                            return@withContext CHANGE_RES

                        } else {
                            //has high refresh rate per test so retain mode
                            isModeUpdated = true

                            if (!loadComplete) {
                                InternalProfiles.loadToProfilesObj(
                                    currentModeOnly = true,
                                    overwriteExisting = false,
                                    mUtilsRefreshRateSt = mUtilsRefreshRate
                                )
                            }
                        }
                    }
                }
            } else {
                //standard mode and no writeSecure perm
                if (mUtilsRefreshRate.mUtilsDeviceInfo.isPowerSavingsModeOn){
                    return@withContext POWER_SAVINGS
                }

                if (!loadComplete) {
                    //not configComplete so not sure if res supports high, so prompt change mode
                    return@withContext CHANGE_MODE
                } else {
                    //  Log.d(TAG, "getHighestHzForAllMode called from UtilsChangeMaxHz")
                    if (mUtilsRefreshRate.getResoHighestHzForAllMode(null).toInt() > STANDARD_REFRESH_RATE_HZ) {
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
                InternalProfiles.loadToProfilesObj(
                    currentModeOnly = true,
                    overwriteExisting = false,
                    mUtilsRefreshRateSt = mUtilsRefreshRate
                )
                delay(250)
            }

            try {
                if (mUtilsRefreshRate.getThisRrmAndResoHighestHz(null, null)
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
                    max(if (idx >= supportedHzIntCurMod!!.size - 1) { supportedHzIntCurMod!![0] } else { supportedHzIntCurMod!![idx + 1]},
                        mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefMinHzForToggle)
                } else {
                    if (supportedHzIntCurMod!!.indexOfFirst { it == maxHzToApply } != -1) {
                        maxHzToApply
                    }else{
                        mUtilsRefreshRate.getPeakRefreshRate()
                    }
                }

            if (mUtilsRefreshRate.mUtilsDeviceInfo.isDisplayOn()) {
                mUtilsRefreshRate.setRefreshRate(maxHzToApplyFinal.coerceAtLeast(lowestHzCurMode), mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefMinHzAdapt)
            }

            prrActive.set(maxHzToApplyFinal.coerceAtLeast(lowestHzCurMode))
            if (isPremium.get()!! && isPowerSaveModeOn.get() == true){// && keepModeOnPowerSaving) {
                mUtilsRefreshRate.mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm = maxHzToApplyFinal
            }else{
                mUtilsRefreshRate.mUtilsPrefsGmh.hzPrefMaxRefreshRate = maxHzToApplyFinal
            }
            return@withContext per

        }else{
            return@withContext NO_CONFIG_LOADED
        }//Log.i(TAG, "UtilsChangeMax: hzSet is empty")

    }

}

