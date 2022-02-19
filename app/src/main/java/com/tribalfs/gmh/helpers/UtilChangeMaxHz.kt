package com.tribalfs.gmh.helpers

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveMode
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntCurMod
import com.tribalfs.gmh.helpers.UtilPermSt.Companion.CHANGE_SETTINGS
import com.tribalfs.gmh.profiles.InternalProfiles
import com.tribalfs.gmh.profiles.ProfilesObj.loadComplete
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.*
import kotlin.math.max


class UtilChangeMaxHz (private val appCtx: Context) {

    companion object{
        const val CHANGE_RES = -15
        const val CHANGE_MODE = -16
        const val POWER_SAVINGS = -17
        const val NO_CONFIG_LOADED = -18
    }

    private var isModeUpdated = false
    
    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun changeMaxHz(maxHzToApply: Int?): Int = withContext(Dispatchers.IO){

        val per = if (UtilPermSt.instance(appCtx).hasWriteSystemPerm()) PERMISSION_GRANTED else CHANGE_SETTINGS

        if (per == CHANGE_SETTINGS) {
            return@withContext CHANGE_SETTINGS
        }

        if (currentRefreshRateMode.get() == REFRESH_RATE_MODE_STANDARD) {
            if (hasWriteSecureSetPerm) {
                if (UtilRefreshRateSt.instance(appCtx).getResoHighestHzForAllMode(null)
                    > SIXTY_HZ
                ) {
                    isModeUpdated = true
                    withContext(Dispatchers.IO) {
                        UtilRefreshRateSt.instance(appCtx).setPrefOrAdaptOrHighRefreshRateMode(null)
                        delay(400)
                    }
                } else {
                    if (loadComplete) {
                        //we are sure that resolution has no high refresh rate support
                        return@withContext CHANGE_RES

                    } else {
                        //hasWriteSecure perm so test if current reso supports adaptive or high
                        withContext(Dispatchers.IO) {
                            UtilRefreshRateSt.instance(appCtx).setPrefOrAdaptOrHighRefreshRateMode(null)
                            delay(400)//don't remove
                        }

                        if (UtilsDeviceInfoSt.instance(appCtx).getMaxHzForCurrentReso(null).toInt() <= SIXTY_HZ) {
                            //restore to standard if not high refresh rate
                            UtilRefreshRateSt.instance(appCtx).setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
                            return@withContext CHANGE_RES

                        } else {
                            //has high refresh rate per test so retain mode
                            isModeUpdated = true

                            if (!loadComplete) {
                                InternalProfiles.loadToProfilesObj(
                                    currentModeOnly = true,
                                    overwriteExisting = false,
                                    appCtx
                                )
                            }
                        }
                    }
                }
            } else {
                //standard mode and no writeSecure perm
                if (UtilsDeviceInfoSt.instance(appCtx).isPowerSavingsMode()){
                    return@withContext POWER_SAVINGS
                }

                if (!loadComplete) {
                    //not configComplete so not sure if res supports high, so prompt change mode
                    return@withContext CHANGE_MODE
                } else {
                    //  Log.d(TAG, "getHighestHzForAllMode called from UtilsChangeMaxHz")
                    if (UtilRefreshRateSt.instance(appCtx).getResoHighestHzForAllMode(null).toInt() > SIXTY_HZ) {
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
                    appCtx
                )
                delay(250)
            }

            try {
                if (UtilRefreshRateSt.instance(appCtx).getThisRrmAndResoHighestHz(null, null)
                        .toInt()
                    <= SIXTY_HZ
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
                    val idx = supportedHzIntCurMod!!.indexOfFirst { it == UtilRefreshRateSt.instance(appCtx).getPeakRefreshRate() }
                    max(if (idx >= supportedHzIntCurMod!!.size - 1) { supportedHzIntCurMod!![0] } else { supportedHzIntCurMod!![idx + 1]},
                        UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzForToggle)
                } else {
                    if (supportedHzIntCurMod!!.indexOfFirst { it == maxHzToApply } != -1) {
                        maxHzToApply
                    }else{
                        UtilRefreshRateSt.instance(appCtx).getPeakRefreshRate()
                    }
                }

            if (UtilsDeviceInfoSt.instance(appCtx).isDisplayOn()) {
                UtilRefreshRateSt.instance(appCtx).setRefreshRate(maxHzToApplyFinal.coerceAtLeast(lowestHzCurMode), UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt)
            }

            prrActive.set(maxHzToApplyFinal.coerceAtLeast(lowestHzCurMode))
            if (isPremium.get()!! && isPowerSaveMode.get() == true){// && keepModeOnPowerSaving) {
                UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRatePsm = maxHzToApplyFinal
            }else{
                UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRate = maxHzToApplyFinal
            }
            return@withContext per

        }else{
            return@withContext NO_CONFIG_LOADED
        }//Log.i(TAG, "UtilsChangeMax: hzSet is empty")

    }

}

