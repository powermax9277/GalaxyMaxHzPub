package com.tribalfs.gmh.receivers

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.Intent.*
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
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
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptiveValid
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveMode
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.offScreenRefreshRate
import com.tribalfs.gmh.helpers.CacheSettings.restoreSync
import com.tribalfs.gmh.helpers.CacheSettings.screenOffRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.sensorOnKey
import com.tribalfs.gmh.helpers.CacheSettings.turnOff5GOnPsm
import com.tribalfs.gmh.netspeed.NetSpeedService.Companion.netSpeedService
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch



private const val PREF_NET_TYPE_LTE_GSM_WCDMA    = 9 /* LTE, GSM/WCDMA */
private const val PREF_NET_TYPE_5G_LTE_GSM_WCDMA = 26

@ExperimentalCoroutinesApi
open class GmhBroadcastReceivers(private val appCtx: Context,
                                 private val gmhBroadcastCallback: GmhBroadcastCallback,
                                 private val scope: CoroutineScope,
                                 private val handler: Handler): BroadcastReceiver() {

   // private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val connectivityManager by  lazy { appCtx.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager}

    init{
        UtilsDeviceInfoSt.instance(appCtx).isDisplayOn().let { screenOn ->
            isScreenOn.set(screenOn)

            //Check if app is destroyed on screen off
            UtilsPrefsGmhSt.instance(appCtx).gmhPrefRefreshRateModePref?.let{ prefRrm ->
                if (prefRrm != currentRefreshRateMode.get()) {
                    scope.launch {
                        if (screenOn) {
                            UtilRefreshRateSt.instance(appCtx).setRefreshRateMode(UtilsPrefsGmhSt.instance(appCtx).gmhPrefRefreshRateModePref!!)
                        }else {//= Screen-off
                            //delay(250)
                            ignoreRrmChange.set(true)
                            screenOffRefreshRateMode?.let { soRefreshrate ->
                                UtilRefreshRateSt.instance(appCtx).setRefreshRateMode(soRefreshrate)
                            }
                        }
                    }
                }
            }


            if (screenOn) {
                scope.launch {
                    if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefRestoreSyncIsOn) {
                        ContentResolver.setMasterSyncAutomatically(true)
                    }
                }
                scope.launch {
                    if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefPsmIsOffCache) {
                        //Not ignored
                            if (hasWriteSecureSetPerm) {
                                Settings.Global.putString(
                                    appCtx.contentResolver,
                                    POWER_SAVING_MODE,
                                    POWER_SAVING_OFF
                                )
                            }
                    }
                }
            } else {
                //Screen is off
                restoreSync.set(UtilsPrefsGmhSt.instance(appCtx).gmhPrefRestoreSyncIsOn)
                disablePsm.set(UtilsPrefsGmhSt.instance(appCtx).gmhPrefPsmIsOffCache)
            }
        }
    }


    private val autosyncDisablerRunnable: Runnable by lazy {
        Runnable {
            ContentResolver.getMasterSyncAutomatically().let {
                restoreSync.set(it)
                UtilsPrefsGmhSt.instance(appCtx).gmhPrefRestoreSyncIsOn = it
                if (it) {
                    ContentResolver.setMasterSyncAutomatically(false)
                }
            }
        }
    }


    private val psmEnablerRunnable: Runnable by lazy {
        Runnable {
            if (isPowerSaveMode.get() == false) {
                disablePsm.set(true)
                setPowerSaving(true)
            }
        }
    }


    private val captureRrRunnable: Runnable by lazy {
        Runnable {
            offScreenRefreshRate = "${UtilsDeviceInfoSt.instance(appCtx).getCurrentDisplay().refreshRate.toInt()} hz"
        }
    }


    override fun onReceive(p0: Context, p1: Intent) {

        p1.action?.let{gmhBroadcastCallback.onIntentReceived(it)}

        when (p1.action) {
            ACTION_POWER_SAVE_MODE_CHANGED -> {
                isPowerSaveMode.set(UtilsDeviceInfoSt.instance(appCtx).isPowerSavingsMode())
                if (ignorePowerModeChange.getAndSet(false) || !hasWriteSecureSetPerm) return
                UtilsPrefsGmhSt.instance(appCtx).gmhPrefPsmIsOffCache = isPowerSaveMode.get() != true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PsmChangeHandler.instance(appCtx).handle()
                }
            }

            ACTION_SCREEN_OFF -> {

                handler.postDelayed(captureRrRunnable, 5000)

                if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefPsmOnSo) { handler.postDelayed(psmEnablerRunnable,8000) }

                if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefDisableSyncIsOn) { handler.postDelayed(autosyncDisablerRunnable,12000) }

            }


            ACTION_SCREEN_ON -> {

               // handler.removeCallbacksAndMessages(null)

                scope.launch {
                    if (restoreSync.get()) ContentResolver.setMasterSyncAutomatically(true)
                    if (disablePsm.get()) setPowerSaving(false)

                    if (netSpeedService == null && UtilsPrefsGmhSt.instance(appCtx).gmhPrefNetSpeedIsOn) {
                        if (isInternetConnected()) {
                            NetSpeedServiceHelperStn.instance(appCtx).startNetSpeed()
                        }
                    }
                }

                if (isFakeAdaptiveValid.get() == true) {
                    scope.launch {
                        currentBrightness.set(UtilsDeviceInfoSt.instance(appCtx).getScreenBrightnessPercent())
                    }

                }
            }

            ACTION_LOCALE_CHANGED -> {
                UtilsPrefsGmhSt.instance(appCtx).gmhPrefSensorOnKey = ""
                sensorOnKey = null
            }
        }
    }

    private fun isInternetConnected(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (connectivityManager.activeNetwork != null && connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) != null) {
                    return  true
                }
            } else {
                if (connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnectedOrConnecting) {
                    return true
                }
            }
        return false
    }

    @Synchronized
    private fun setPowerSaving(psmOn: Boolean){

        ignorePowerModeChange.set(true)

        if (hasWriteSecureSetPerm) {
            Settings.Global.putString(
                appCtx.contentResolver,
                POWER_SAVING_MODE,
                if (psmOn) POWER_SAVING_ON else POWER_SAVING_OFF
            )
        }

        if (turnOff5GOnPsm == true) {
            val pnm = (Settings.Global.getString(appCtx.contentResolver, PREFERRED_NETWORK_MODE)
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
                        appCtx.contentResolver,
                        "$PREFERRED_NETWORK_MODE${idxOf5G + 1}",
                        (if (psmOn) alt else PREF_NET_TYPE_5G_LTE_GSM_WCDMA).toString()
                    )
                }
            }

        }
    }


}