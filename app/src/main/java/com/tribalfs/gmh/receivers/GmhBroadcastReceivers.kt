package com.tribalfs.gmh.receivers

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
import android.provider.Settings
import com.tribalfs.gmh.callbacks.GmhBroadcastCallback
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.currentBrightness
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.disablePsm
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.ignorePowerModeChange
import com.tribalfs.gmh.helpers.CacheSettings.ignoreRrmChange
import com.tribalfs.gmh.helpers.CacheSettings.isNetSpeedRunning
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.offScreenRefreshRate
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.restoreSync
import com.tribalfs.gmh.helpers.CacheSettings.screenOffRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.turnOff5GOnPsm
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import kotlinx.coroutines.*

private const val PREF_NET_TYPE_LTE_GSM_WCDMA    = 9 /* LTE, GSM/WCDMA */
private const val PREF_NET_TYPE_5G_LTE_GSM_WCDMA = 26

@ExperimentalCoroutinesApi
open class GmhBroadcastReceivers(context: Context, private val gmhBroadcastCallback: GmhBroadcastCallback, private val scope: CoroutineScope): BroadcastReceiver() {
    private val appCtx = context.applicationContext
    private val mContentResolver = appCtx.contentResolver

    private val mUtilsRefreshRate by lazy { UtilRefreshRateSt.instance(appCtx)}
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    init{
        mUtilsRefreshRate.mUtilsDeviceInfo.isDisplayOn().let {
            isScreenOn = it

            //Check if app is destroyed on screen off
            if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefRefreshRateModePref != null
                && mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefRefreshRateModePref != currentRefreshRateMode.get()
            ) {
                scope.launch {
                    mUtilsRefreshRate.setRefreshRateMode(mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefRefreshRateModePref!!)
                    if (!it) {
                        delay(250)
                        ignoreRrmChange = true
                        screenOffRefreshRateMode?.let { soRefreshrate ->
                            mUtilsRefreshRate.setRefreshRateMode(
                                soRefreshrate
                            )
                        }
                    }
                }
            }

            if (it) {
                scope.launch {
                    if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefRestoreSyncIsOn) {
                        ContentResolver.setMasterSyncAutomatically(true)
                    }
                }
                scope.launch {
                    if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefPsmIsOffCache) {
                        //Not ignored
                            if (hasWriteSecureSetPerm) {
                                Settings.Global.putString(
                                    context.applicationContext.contentResolver,
                                    POWER_SAVING_MODE,
                                    POWER_SAVING_OFF
                                )
                            }
                    }
                }
            } else {
                //Screen is off
                restoreSync = (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefRestoreSyncIsOn)
                disablePsm = (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefPsmIsOffCache)
            }
        }
    }


    private val autosyncDisablerRunnable: Runnable by lazy {
        Runnable {
            ContentResolver.getMasterSyncAutomatically().let {
                restoreSync = it
                mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefRestoreSyncIsOn = it
                if (it) {
                    ContentResolver.setMasterSyncAutomatically(false)
                }
            }
        }
    }


    private val psmEnablerRunnable: Runnable by lazy {
        Runnable {
            if (isPowerSaveModeOn.get() == false) {
                disablePsm = true
                setPowerSaving(true)
            }
        }
    }


    private val forceLowestRunnable: Runnable by lazy {
        Runnable {
            if (screenOffRefreshRateMode != currentRefreshRateMode.get()) {
                ignoreRrmChange = true
                if (mUtilsRefreshRate.setRefreshRateMode(screenOffRefreshRateMode!!)) {
                    mUtilsRefreshRate.setRefreshRate(lowestHzForAllMode, null)
                }else{
                    mUtilsRefreshRate.setRefreshRate(lowestHzCurMode, null)
                    ignoreRrmChange = false
                }
            }else {
                mUtilsRefreshRate.setRefreshRate(lowestHzCurMode, null)
            }
        }
    }


    private val captureRrRunnable: Runnable by lazy {
        Runnable {
            offScreenRefreshRate = "${mUtilsRefreshRate.mUtilsDeviceInfo.currentDisplay.refreshRate.toInt()} hz"
        }
    }


    override fun onReceive(p0: Context, p1: Intent) {

        p1.action?.let{gmhBroadcastCallback.onIntentReceived(it)}

        when (p1.action) {
            ACTION_POWER_SAVE_MODE_CHANGED -> {
                isPowerSaveModeOn.set(mUtilsRefreshRate.mUtilsDeviceInfo.isPowerSavingsModeOn)
                if (ignorePowerModeChange.getAndSet(false) || !hasWriteSecureSetPerm) return
                mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefPsmIsOffCache = isPowerSaveModeOn.get() != true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PsmChangeHandler.instance(appCtx).handle()
                }
            }

            Intent.ACTION_SCREEN_OFF -> {
                isScreenOn = false
                restoreSync = false
                disablePsm = false

                // Workaround for AOD Bug on some device????
                mUtilsRefreshRate.clearPeakAndMinRefreshRate()

                if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefForceLowestSoIsOn) { handler.postDelayed(forceLowestRunnable,3000) }

                handler.postDelayed(captureRrRunnable, 5000)

                if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefPsmOnSo) { handler.postDelayed(psmEnablerRunnable,8000) }

                if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefDisableSyncIsOn) { handler.postDelayed(autosyncDisablerRunnable,12000) }

            }


            Intent.ACTION_SCREEN_ON -> {
                isScreenOn = true

                scope.launch {
                    mUtilsRefreshRate.setRefreshRate(prrActive.get()!!, mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefMinHzAdapt)
                    currentRefreshRateMode.get()?.let {
                        if (screenOffRefreshRateMode != it) {
                            //ignoreRrmChange = true
                            mUtilsRefreshRate.setRefreshRateMode(it)
                        }
                    }
                }

                handler.removeCallbacksAndMessages(null)

                scope.launch {
                    if (restoreSync) ContentResolver.setMasterSyncAutomatically(true)
                    if (disablePsm) setPowerSaving(false)

                    if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefNetSpeedIsOn && !isNetSpeedRunning.get()!!) {
                        NetSpeedServiceHelperStn.instance(appCtx).runNetSpeed(null)
                    }
                }

                scope.launch {
                    currentBrightness.set(mUtilsRefreshRate.mUtilsDeviceInfo.getScreenBrightnessPercent())
                }

            }
        }
    }



    @Synchronized
    private fun setPowerSaving(psmOn: Boolean){
        //Log.d(TAG, "turnOnPowerSaving: $on")
        ignorePowerModeChange.set(true)

        if (hasWriteSecureSetPerm) {
            Settings.Global.putString(
                mContentResolver,
                POWER_SAVING_MODE,
                if (psmOn) POWER_SAVING_ON else POWER_SAVING_OFF
            )
        }

        if (turnOff5GOnPsm == true) {
            val pnm = (Settings.Global.getString(mContentResolver, PREFERRED_NETWORK_MODE)
                ?: "$PREF_NET_TYPE_LTE_GSM_WCDMA,$PREF_NET_TYPE_LTE_GSM_WCDMA").split(",")

            val idxOf5G = pnm.indexOf(PREF_NET_TYPE_5G_LTE_GSM_WCDMA.toString())

            if (idxOf5G != -1) {
                val alt = if (pnm.size == 2){
                    if (idxOf5G == 0) pnm[1] else pnm[0]
                }else{
                    PREF_NET_TYPE_LTE_GSM_WCDMA.toString()
                }
                if (hasWriteSecureSetPerm) {
                    Settings.Global.putString(
                        mContentResolver,
                        "$PREFERRED_NETWORK_MODE${idxOf5G + 1}",
                        (if (psmOn) alt else PREF_NET_TYPE_5G_LTE_GSM_WCDMA).toString()
                    )
                }
            }

        }
    }


}