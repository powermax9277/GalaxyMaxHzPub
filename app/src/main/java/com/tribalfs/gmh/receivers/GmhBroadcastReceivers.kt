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
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.callbacks.AccessibilityCallback
import com.tribalfs.gmh.helpers.CacheSettings.currentBrightness
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.disablePsm
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.hzStatus
import com.tribalfs.gmh.helpers.CacheSettings.ignorePowerModeChange
import com.tribalfs.gmh.helpers.CacheSettings.ignoreRrmChange
import com.tribalfs.gmh.helpers.CacheSettings.isNsNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.offScreenRefreshRate
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.restoreSync
import com.tribalfs.gmh.helpers.CacheSettings.screenOffRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.turnOff5GOnPsm
import com.tribalfs.gmh.helpers.CacheSettings.turnOffAutoSensorsOff
import com.tribalfs.gmh.helpers.PsmChangeHandler
import com.tribalfs.gmh.helpers.UtilsDeviceInfo
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.PREFERRED_NETWORK_MODE
import com.tribalfs.gmh.helpers.UtilsRefreshRate
import com.tribalfs.gmh.hertz.HzService.Companion.PLAYING
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.*


@ExperimentalCoroutinesApi
@RequiresApi(Build.VERSION_CODES.M)
class GmhBroadcastReceivers(context: Context, private val accessibilityCallback: AccessibilityCallback, private val scope: CoroutineScope): BroadcastReceiver() {
    private val appCtx = context.applicationContext
    private val mContentResolver = appCtx.contentResolver

    private val mUtilsPrefsGmh by lazy { UtilsPrefsGmh(appCtx) }
    private val mUtilsDeviceInfo by lazy { UtilsDeviceInfo(appCtx) }
    private val mUtilsRefreshRate by lazy { UtilsRefreshRate (appCtx)}
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    //private val keyguardManager by lazy {appCtx.getSystemService(KEYGUARD_SERVICE) as KeyguardManager }
    companion object{
        private const val PREF_NET_TYPE_LTE_GSM_WCDMA    = 9 /* LTE, GSM/WCDMA */
        private const val PREF_NET_TYPE_5G_LTE_GSM_WCDMA = 26
        private const val LOW_POWER = "low_power"
        private const val LOW_POWER_ON = "1"
        private const val LOW_POWER_OFF = "0"
        //  private const val TAG = "GmhBroadcastReceivers"
    }

    init{
        mUtilsDeviceInfo.isDisplayOn().let {
            isScreenOn = it

            //Check if app is destroyed on screen off
            if (mUtilsPrefsGmh.gmhPrefRefreshRateModePref != null
                && mUtilsPrefsGmh.gmhPrefRefreshRateModePref != currentRefreshRateMode.get()
            ) {
                scope.launch {
                    mUtilsRefreshRate.setRefreshRateMode(mUtilsPrefsGmh.gmhPrefRefreshRateModePref!!)
                    if (!it) {
                        delay(250)
                        ignoreRrmChange = true
                        screenOffRefreshRateMode?.let { it1 ->
                            mUtilsRefreshRate.setRefreshRateMode(
                                it1
                            )
                        }
                    }
                }
            }

            if (it) {
                scope.launch {
                    if (mUtilsPrefsGmh.gmhPrefRestoreSyncIsOn) {
                        ContentResolver.setMasterSyncAutomatically(true)
                    }
                }
                scope.launch {
                    if (mUtilsPrefsGmh.gmhPrefPsmIsOffCache) {
                        //Not ignored
                            try {
                                Settings.Global.putString(
                                    context.applicationContext.contentResolver,
                                    LOW_POWER,
                                    LOW_POWER_OFF
                                )
                            }catch(_: Exception){}
                    }
                }
            } else {
                //Screen is off
                restoreSync = (mUtilsPrefsGmh.gmhPrefRestoreSyncIsOn)
                disablePsm = (mUtilsPrefsGmh.gmhPrefPsmIsOffCache)
            }
        }
    }


    private val autosyncDisablerRunnable: Runnable by lazy {
        Runnable {
            ContentResolver.getMasterSyncAutomatically().let {
                restoreSync = it
                mUtilsPrefsGmh.gmhPrefRestoreSyncIsOn = it
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
                    mUtilsRefreshRate.setRefreshRate(lowestHzForAllMode)
                }else{
                    mUtilsRefreshRate.setRefreshRate(lowestHzCurMode)
                    ignoreRrmChange = false
                }
            }else {
                mUtilsRefreshRate.setRefreshRate(lowestHzCurMode)
            }
        }
    }


    private val captureRrRunnable: Runnable by lazy {
        Runnable {
            offScreenRefreshRate = "${mUtilsDeviceInfo.getRefreshRateInt()} hz"
        }
    }

    private val hzMonNetspeedSensorsDisablerRunnable: Runnable by lazy {
        Runnable {
            if (isNsNotifOn.get()!!) {
                NetSpeedServiceHelperStn.instance(appCtx).stopService(null)
            }
            if (hzStatus.get() == PLAYING) {
                HzServiceHelperStn.instance(appCtx).stopHertz()
            }
            if (mUtilsPrefsGmh.gmhPrefSensorsOff) {
                accessibilityCallback.onChange(userPresent = false, turnOffSensors = true)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(p0: Context, p1: Intent) {
        when (p1.action) {

            ACTION_POWER_SAVE_MODE_CHANGED -> {
                isPowerSaveModeOn.set(mUtilsDeviceInfo.isPowerSavingsModeOn())
                if (ignorePowerModeChange.getAndSet(false) || !hasWriteSecureSetPerm) return
                mUtilsPrefsGmh.gmhPrefPsmIsOffCache = isPowerSaveModeOn.get() != true
                PsmChangeHandler.instance(appCtx).handle()
            }

            Intent.ACTION_SCREEN_OFF -> {
                isScreenOn = false
                restoreSync = false
                disablePsm = false

                // Workaround for AOD Bug on some device????
                mUtilsRefreshRate.clearRefreshRate()

                if (mUtilsPrefsGmh.gmhPrefForceLowestSoIsOn) { handler.postDelayed(forceLowestRunnable,3000) }

                handler.postDelayed(captureRrRunnable, 4000)

                if (mUtilsPrefsGmh.gmhPrefPsmOnSo) { handler.postDelayed(psmEnablerRunnable,8000) }

                if (mUtilsPrefsGmh.gmhPrefDisableSyncIsOn) { handler.postDelayed(autosyncDisablerRunnable,12000) }

                handler.postDelayed(hzMonNetspeedSensorsDisablerRunnable,20000)
            }


            Intent.ACTION_SCREEN_ON -> {
                isScreenOn = true

                scope.launch {
                    mUtilsRefreshRate.setRefreshRate(prrActive.get()!!)
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

                    if (mUtilsPrefsGmh.gmhPrefNetSpeedIsOn && !isNsNotifOn.get()!!) {
                        NetSpeedServiceHelperStn.instance(appCtx).runNetSpeed(null)
                    }
                    if (mUtilsPrefsGmh.gmhPrefHzIsOn && hzStatus.get() != PLAYING) {
                        HzServiceHelperStn.instance(appCtx).startHertz(null, null, null)
                    }
                }

                scope.launch {
                    currentBrightness.set(mUtilsDeviceInfo.getScreenBrightnessPercent())
                }

            }

            Intent.ACTION_USER_PRESENT -> {
                accessibilityCallback.onChange(true, mUtilsPrefsGmh.gmhPrefSensorsOff || turnOffAutoSensorsOff)
                if (turnOffAutoSensorsOff){
                    mUtilsPrefsGmh.gmhPrefSensorsOff = false
                    turnOffAutoSensorsOff = false
                }
            }

            /* ACTION_PHONE_STATE ->{
                        when (val stateStr = p1.extras?.getString(TelephonyManager.EXTRA_STATE)){
                            TelephonyManager.EXTRA_STATE_RINGING ->{
                                if (!keyguardManager.isDeviceLocked) {
                                    // launch {
                                    accessibilityCallback.onChange(
                                        true,
                                        mUtilsPrefsGmh.gmhPrefSensorsOff
                                    )
                                    // }
                                }
                            }
                            TelephonyManager.EXTRA_STATE_OFFHOOK ->{
                                if (keyguardManager.isDeviceLocked) {
                                    Toast.makeText(appCtx, "You microphone might be disabled during this call. Long-press the SensorsOff  tile to unlock and turn it off", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                    }*/
        }
    }



    @Synchronized
    private fun setPowerSaving(psmOn: Boolean){
        //Log.d(TAG, "turnOnPowerSaving: $on")
        ignorePowerModeChange.set(true)

        if (hasWriteSecureSetPerm) {
            Settings.Global.putString(
                mContentResolver,
                LOW_POWER,
                if (psmOn) LOW_POWER_ON else LOW_POWER_OFF
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