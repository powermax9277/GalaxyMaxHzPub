package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.Display
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.UtilsResoName.getName
import com.tribalfs.gmh.profiles.ResolutionBasic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Field
import java.util.*
import kotlin.math.min
import kotlin.math.round


class UtilsDeviceInfo(val context: Context) {

    companion object{
        internal const val STANDARD_REFRESH_RATE_HZ = 60
        internal const val REFRESH_RATE_MODE = "refresh_rate_mode"
        internal const val REFRESH_RATE_MODE_COVER = "refresh_rate_mode_cover"
        internal const val ONEPLUS_SCREEN_REFRESH_RATE = "oneplus_screen_refresh_rate"
        internal const val PEAK_REFRESH_RATE = "peak_refresh_rate"
        internal const val USER_REFRESH_RATE = "user_refresh_rate"
        internal const val PSM_5G_MODE = "psm_5G_mode"
        internal const val PREFERRED_NETWORK_MODE = "preferred_network_mode"
        internal const val DEVICE_IDLE_CONSTANTS = "device_idle_constants"
        internal const val BATTERY_SAVER_CONSTANTS = "battery_saver_constants"
        internal const val MIN_REFRESH_RATE = "min_refresh_rate"
        internal const val REFRESH_RATE_MODE_ALWAYS = "2"
        internal const val REFRESH_RATE_MODE_SEAMLESS ="1"
        internal const val REFRESH_RATE_MODE_STANDARD ="0"
        internal const val ONEPLUS_RATE_MODE_ALWAYS = "0"
        internal const val ONEPLUS_RATE_MODE_SEAMLESS = "1"
        internal const val BRIGHTNESS_RESOLUTION = 100
        internal const val SCREEN_BRIGHTNESS_FLOAT = "screen_brightness_float"
    }
    private val appCtx: Context = context.applicationContext
    private val mContentResolver = appCtx.contentResolver
    internal val deviceModelVariant: String = Build.MODEL
    internal val androidVersion: String = Build.VERSION.RELEASE
    internal val manufacturer: String = Build.MANUFACTURER.uppercase(Locale.ROOT)
    internal val deviceModel: String = if (manufacturer == "SAMSUNG") {
        deviceModelVariant.substring(0, min(deviceModelVariant.length, 7))
    } else {
        deviceModelVariant
    }

    private val currentDisplay: Display = (appCtx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(displayId)

    fun getRefreshRateInt(): Int {
        return currentDisplay.refreshRate.toInt()
    }

    fun isDisplayOn(): Boolean{
        return currentDisplay.state == Display.STATE_ON
    }

    @SuppressLint("NewApi")
    fun getDisplayResolution(): ResolutionBasic {
        synchronized(this) {
            val resStr = Settings.Global.getString(mContentResolver, "display_size_forced"/*custom resolution*/)
            return if (currentDisplay.displayId == Display.DEFAULT_DISPLAY && !resStr.isNullOrEmpty()) {
                val resArr = resStr.split(",")
                ResolutionBasic(resArr[1].toInt()/*height*/, resArr[0].toInt()/*width*/)
            } else {
                getDisplayResolutionFromMode()
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getDisplayResolutionFromMode(): ResolutionBasic{
        synchronized(this) {
            val dispMode = currentDisplay.mode
            return ResolutionBasic(dispMode.physicalHeight, dispMode.physicalWidth)
        }
    }


    private fun getDisplayDensity(): Int {
        val denStr = Settings.Secure.getString(mContentResolver, "display_density_forced"/*custom density*/)
        return if (denStr != null && denStr.isNotEmpty()) {
            denStr.toInt()
        } else {
            val metrics = Resources.getSystem().displayMetrics
            currentDisplay.getRealMetrics(metrics)
            metrics.densityDpi
        }
    }



    internal fun getDisplayResStr(separator: String?): String {
        val res = getDisplayResolution()
        val sep = separator?:","
        return "${res.resHeight}$sep${res.resWidth}"
    }


    internal fun getDisplayResFromModeStr(separator: String?): String {
        val res = getDisplayResolutionFromMode()
        val sep = separator?: ","
        return "${res.resHeight}$sep${res.resWidth}"
    }

    fun getDensity(): String {
        return getDisplayDensity().toString()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun getDisplayModesSet(): Map<String, List<Float>> {
        val supportedModes = currentDisplay.supportedModes
        return supportedModes.asSequence()
            .distinct()
            .groupBy(
                { it.physicalHeight.toString() + "x" + it.physicalWidth.toString() },
                { round(it.refreshRate*100)/100 })
            .mapValues { (_, values) -> values }
    }



    @RequiresApi(Build.VERSION_CODES.M)
    internal fun getPeakRefreshRateFromSettings(): Int? {
        val prr = Settings.System.getString(mContentResolver, PEAK_REFRESH_RATE)
        return try {
            prr?.toInt()
        } catch (_: Exception) {
            null
        }
    }


    suspend fun getScreenBrightnessPercent(): Int = withContext(Dispatchers.IO){
        val br = Settings.System.getFloat(mContentResolver, SCREEN_BRIGHTNESS_FLOAT, 0.5f)
        return@withContext (br * BRIGHTNESS_RESOLUTION).toInt()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    internal fun getMaxHzForCurrentReso(resStrLcw: String?): Float {
        with (getDisplayModesSet()){
            (this[resStrLcw?:getDisplayResStr("x")]?:this[getDisplayResFromModeStr("x")]).let{
                return it?.maxOrNull()!!
            }
        }
    }


    internal fun getSamRefreshRateMode(): String {
        synchronized(this) {
            return (Settings.Secure.getString(mContentResolver, REFRESH_RATE_MODE)
                ?: 0).toString()
        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    internal fun getResoAndRefRateModeArr(currentRefreshRateMode: String?): Array<String> {
        val reso = getDisplayResolution()
        val resoCat = getName(
            reso.resHeight,
            reso.resWidth
        )

        val mode = when (currentRefreshRateMode?:getSamRefreshRateMode()) {
            REFRESH_RATE_MODE_SEAMLESS -> appCtx.getString(R.string.adp_mode)
            REFRESH_RATE_MODE_STANDARD -> appCtx.getString(R.string.std_mode)
            REFRESH_RATE_MODE_ALWAYS ->  appCtx.getString(R.string.high_mode)
            else -> "?"
        }
        return arrayOf(resoCat, mode)
    }

    internal fun isPowerSavingsModeOn(): Boolean{
        return (appCtx.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode //UtilsSettingsSt.get(appCtx).getConfig(GLOBAL, "low_power") == "1"//
    }

/*    private fun getMaxBrightness(): Int {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val fields = powerManager.javaClass.declaredFields
        for (field in fields) {
            if (field.name == "BRIGHTNESS_ON") {
                field.isAccessible = true
                return try {
                    field[powerManager] as Int
                } catch (e: IllegalAccessException) {
                    255
                }
            }
        }
        return 255*//*Default Value*//*
    }*/

    val oneUiVersion: Double?
        @SuppressLint("PrivateApi")
        get() {
            if (!isSemAvailable()) {
                return null
            }
            val semPlatformIntField: Field = Build.VERSION::class.java.getDeclaredField("SEM_PLATFORM_INT")
            val version: Int = semPlatformIntField.getInt(null) - 90000
            return if (version < 0) {
                1.0
            } else {
                ((version / 10000).toString() + "." + version % 10000 / 100).toDouble()
            }
        }

    private fun isSemAvailable(): Boolean {
        return appCtx.packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile") ||
                appCtx.packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile_lite")
    }

}