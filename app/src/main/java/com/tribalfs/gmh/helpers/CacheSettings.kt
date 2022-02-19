package com.tribalfs.gmh.helpers

import android.view.Display.DEFAULT_DISPLAY
import androidx.databinding.ObservableField
import com.tribalfs.gmh.STOPPED
import java.lang.Long.max
import java.util.concurrent.atomic.AtomicBoolean


object CacheSettings {

    internal var isSamsung: Boolean = true
    internal var isXiaomi: Boolean = false
    internal var isOnePlus: Boolean = false

    internal const val TIMEOUT_FACTOR = 0.90
    internal var isScreenOn = AtomicBoolean(true)
    internal var offScreenRefreshRate:String? = null

    internal val isPowerSaveMode = ObservableField<Boolean>() //Reflects SCREEN ON status only

    internal var adaptiveDelayMillis = 1750L
    internal var adaptiveAccessTimeout: Long = max(1500L, adaptiveDelayMillis * TIMEOUT_FACTOR.toLong())

    internal var displayId: Int = DEFAULT_DISPLAY

    internal var isPremium = ObservableField(false)
    internal val prrActive = ObservableField(60)
    internal val lrrPref = ObservableField(60)
    internal var isSpayInstalled: Boolean? = null
    internal var hasWriteSecureSetPerm: Boolean = false
    internal var hasWriteSystemSetPerm: Boolean = false
    internal var canApplyFakeAdaptive: Boolean = false
    internal val isFakeAdaptive = ObservableField(false)
    internal var turnOff5GOnPsm: Boolean? = null

    internal var isOfficialAdaptive: Boolean = false
    internal var isMultiResolution: Boolean = true
    internal var lowestHzCurMode: Int = 60 //default
    internal var supportedHzIntCurMod: List<Int>? = null
    @Volatile
    internal var minHzListForAdp: List<Int>? = null
    internal var highestHzForAllMode: Int = 60 //default
    internal val currentRefreshRateMode = ObservableField<String>()
    internal var lowestHzForAllMode: Int = 60 //default
    internal var supportedHzIntAllMod: List<Int>? = null
    internal var modesWithLowestHz: List<String>? = null

    internal var keepModeOnPowerSaving: Boolean = false

    internal val hzNotifOn = ObservableField(false)

    internal val hzStatus = ObservableField(STOPPED)

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

    internal var restoreSync  = AtomicBoolean(false)
    internal var disablePsm  = AtomicBoolean(false)
    internal var ignoreRrmChange  = AtomicBoolean( false)
    @Volatile internal var screenOffRefreshRateMode: String?  = null

    internal var turnOffAutoSensorsOff = false
    internal var preventHigh = false
    internal var sensorOnKey: CharSequence? = null


}