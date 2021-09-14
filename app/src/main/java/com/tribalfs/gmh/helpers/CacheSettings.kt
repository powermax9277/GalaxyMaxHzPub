package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.view.Display.DEFAULT_DISPLAY
import androidx.databinding.ObservableField
import com.tribalfs.gmh.hertz.HzService.Companion.STOP
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.Long.max
import java.util.concurrent.atomic.AtomicBoolean


object CacheSettings {

    internal var isSamsung: Boolean = true
    internal var isXiaomi: Boolean = false
    internal var isOnePlus: Boolean = false

    internal const val TIMEOUT_FACTOR = 0.90
    /***Updated by GmhBroadcastReceiver***/
    @Volatile internal var isScreenOn = true
    internal var offScreenRefreshRate:String? = null

    /***Updated by MyApplication, KeepMotion, GmhBroadcastReceiver***/
    internal val isPowerSaveModeOn = ObservableField<Boolean>() //Reflects SCREEN ON status only

    /***Updated by MainActivity***/
   // internal var isTileExpired = false
    internal var adaptiveDelayMillis = 1750L//ok
    @SuppressLint("NewApi")
    internal var adaptiveAccessTimeout: Long = max(1500L, adaptiveDelayMillis * TIMEOUT_FACTOR.toLong())

   // internal var prrKey = PEAK_REFRESH_RATE

    /***Updated by DisplayInfo, MainActivity***/
    internal var displayId: Int = DEFAULT_DISPLAY

    /***Updated by MainActivity,MyApplication***/
    internal var isPremium = ObservableField(false)
    internal val prrActive = ObservableField(60)
    internal val lrrPref = ObservableField(60)
    internal var isSpayInstalled: Boolean? = null
    internal var hasWriteSecureSetPerm: Boolean = false
    internal var hasWriteSystemSetPerm: Boolean = false
    internal var canApplyFakeAdaptive: Boolean = false
    internal val isFakeAdaptive = ObservableField(false)
    internal var turnOff5GOnPsm: Boolean? = null

    /***Updated by ProfilesHelperSt***/
    internal var isOfficialAdaptive: Boolean = false
    internal var isMultiResolution: Boolean = true
    internal var lowestHzCurMode: Int = 60 //default
    internal var supportedHzIntCurMod: List<Int>? = null
    internal var minHzListForAdp: List<Int>? = null
    internal var highestHzForAllMode: Int = 60 //default
    internal val currentRefreshRateMode = ObservableField<String>()//ok
    internal var lowestHzForAllMode: Int = 60 //default
    internal var supportedHzIntAllMod: List<Int>? = null
    internal var modesWithLowestHz: List<String>? = null

    /***Updated by MainActivity,MyApplication, TaskerPlugin***/
    internal var keepModeOnPowerSaving: Boolean = false

    /***Updated by MyApplication,HzService***/
    internal val isHzNotifOn = ObservableField(false)  //ok
    @ExperimentalCoroutinesApi
    @SuppressLint("NewApi")
    internal var hzStatus = ObservableField(STOP)

    /***Updated by MyApplication,NetSpeedService***/
    internal val isNsNotifOn = ObservableField(false) //ok
    //internal val isSwitchingSensorsOn = ObservableField(false) //ok


    /*internal val fixedHzOnSystemUi = object : ObservableField<Boolean>(isHzNotifOn, hzStatus, isNsNotifOn*//*, isSwitchingSensorsOn*//*) {
        @SuppressLint("NewApi")
        override fun get(): Boolean {
            return (hzStatus.get() == PLAYING && isHzNotifOn.get()!!) || isNsNotifOn.get()!!*//* || isSwitchingSensorsOn.get()!!*//*
        }
    }*/

    internal val isFakeAdaptiveValid = object : ObservableField<Boolean>(isFakeAdaptive,isPremium, prrActive,lrrPref) {
        override fun get(): Boolean {
            return isFakeAdaptive.get()!! && isPremium.get()!! && (prrActive.get()!! > lrrPref.get()!! || prrActive.get() == -1)
        }
    }

    internal val currentBrightness = ObservableField(120)
    internal val brightnessThreshold = ObservableField(0)
    internal val belowBrightnessThreshold = object: ObservableField<Boolean>(currentBrightness,brightnessThreshold){
        override fun get(): Boolean {
            return currentBrightness.get()!! < brightnessThreshold.get()!!
        }
    }

    internal val applyAdaptiveMod = object: ObservableField<Boolean>(isFakeAdaptiveValid,belowBrightnessThreshold){
        override fun get(): Boolean {
            return (isFakeAdaptiveValid.get()!! && !belowBrightnessThreshold.get()!!)
        }
    }

    internal var ignorePowerModeChange = AtomicBoolean(false)

    @Volatile internal var restoreSync  = false
    @Volatile internal var disablePsm  = false
    @Volatile internal var ignoreRrmChange  = false
    @Volatile internal var screenOffRefreshRateMode: String?  = null

    internal var turnOffAutoSensorsOff = false
    internal var preventHigh = false
   // internal var turnOnAutoSensorsOff = false

}