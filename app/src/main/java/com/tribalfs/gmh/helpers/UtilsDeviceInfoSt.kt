package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.System.SCREEN_BRIGHTNESS
import android.util.DisplayMetrics
import android.util.Size
import android.view.Display
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Field
import java.util.*
import kotlin.math.min
import kotlin.math.round


class UtilsDeviceInfoSt private constructor(val context: Context) {

    companion object : SingletonMaker<UtilsDeviceInfoSt, Context>(::UtilsDeviceInfoSt){
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
        internal const val DISPLAY_SIZE_FORCED = "display_size_forced"
        internal const val POWER_SAVING_MODE = "low_power"
        internal const val SYSTEM = "system"
        internal const val SECURE = "secure"
        internal const val GLOBAL = "global"
    }

    internal val appCtx: Context = context.applicationContext
    private val mContentResolver = appCtx.contentResolver
    internal val deviceModelVariant: String = Build.MODEL //"SM-TEST" //ODO(before release: Replace with Build.MODEL)
    internal val androidVersion: String = Build.VERSION.RELEASE
    internal val manufacturer: String = Build.MANUFACTURER.uppercase(Locale.ROOT)
    internal val deviceModel: String = if (manufacturer == "SAMSUNG") {
        deviceModelVariant.substring(0, min(deviceModelVariant.length, 7))
    } else {
        deviceModelVariant
    }


    internal val currentDisplay: Display
        get() = (appCtx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(
            displayId
        )

    internal fun isDisplayOn(): Boolean{
        return currentDisplay.state == Display.STATE_ON
    }

    @Suppress("DEPRECATION")
    fun getDisplayResolution(): Size {
        val resStr = Settings.Global.getString(mContentResolver, DISPLAY_SIZE_FORCED/*custom resolution*/)
        return if (currentDisplay.displayId == Display.DEFAULT_DISPLAY && !resStr.isNullOrEmpty()) {
            val resArr = resStr.split(",")
            Size(resArr[0].toInt()/*width*/,resArr[1].toInt()/*height*/)
        } else {
            val metrics = DisplayMetrics()
            currentDisplay.getRealMetrics(metrics)
            if (appCtx.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Size(metrics.widthPixels, metrics.heightPixels)
            }else{
                Size(metrics.heightPixels, metrics.widthPixels)
            }

        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun getDisplayResolutionFromMode(): Size{
        val dispMode = currentDisplay.mode
        return Size(dispMode.physicalWidth, dispMode.physicalHeight)
    }


    internal fun getDisplayDensity(): Int {
        val denStr = try {
            Settings.Secure.getString(
                mContentResolver,
                "display_density_forced"/*custom density*/
            )
        } catch (_: Exception) {
            null
        }
        return if (!denStr.isNullOrEmpty()) {
            denStr.toInt()
        } else {
            val metrics: DisplayMetrics = appCtx.resources.displayMetrics
            (metrics.density * 160f).toInt()
            /*resources.configuration.densityDpi*/
        }
    }



    internal fun getDisplayResoStr(separator: String?): String {
        val res = getDisplayResolution()
        val sep = separator ?: ","
        return "${res.height}$sep${res.width}"
    }


    @RequiresApi(Build.VERSION_CODES.M)
    internal fun getDisplayResFromModeStr(separator: String?): String {
        val res = getDisplayResolutionFromMode()
        val sep = separator ?: ","
        return "${res.height}$sep${res.width}"
    }


    fun getDisplayModesSet(): Map<String, List<Float>> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            currentDisplay.supportedModes.let {modes ->
                modes.asSequence()
                    .distinct()
                    .groupBy(
                        {y -> y.physicalHeight.toString() + "x" + y.physicalWidth.toString() },
                        {x -> round(x.refreshRate * 100) / 100 })
                    .mapValues { (_, values) -> values }
            }
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            currentDisplay.getMetrics(metrics)
            val resoKey = "${metrics.heightPixels}x${metrics.widthPixels}"
            val map = mutableMapOf<String, List<Float>>()
            @Suppress("DEPRECATION")
            map[resoKey] = currentDisplay.supportedRefreshRates.toList()
            map
        }
    }


    suspend fun getScreenBrightnessPercent(): Int = withContext(Dispatchers.IO){
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            val br = Settings.System.getFloat(mContentResolver, SCREEN_BRIGHTNESS_FLOAT, 0.5f)
            return@withContext (br * BRIGHTNESS_RESOLUTION).toInt()
        }else {
            return@withContext Settings.System.getInt(mContentResolver, SCREEN_BRIGHTNESS, 50)
        }
    }


    // @RequiresApi(Build.VERSION_CODES.M)
    internal fun getMaxHzForCurrentReso(resStrLcw: String?): Float {
        var refreshRates: List<Float>?

        refreshRates = if (resStrLcw != null) {
            getDisplayModesSet()[resStrLcw]
        } else {
            getDisplayModesSet()[getDisplayResoStr("x")]
        }

        if (refreshRates != null) {
            return refreshRates.maxOrNull()!!
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            refreshRates = getDisplayModesSet()[getDisplayResFromModeStr("x")]
        }

        return if (refreshRates != null) {
            refreshRates.maxOrNull()!!
        } else {
            @Suppress("DEPRECATION")
            currentDisplay.supportedRefreshRates.maxOrNull()!!
        }
    }




    internal var isPowerSavingsModeOn: Boolean
        get() {
            return try {
                Settings.Global.getInt(mContentResolver, POWER_SAVING_MODE) == 1
            }catch(_:Exception){
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isPowerSaveMode
            }
        }
        set(on) { try{Settings.Global.putInt(mContentResolver, POWER_SAVING_MODE, if (on) 1 else 0) }catch(_:Exception){}}



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

    fun getSettingsList(settings: String):List<String>{
        val list = mutableListOf<String>()
        val columns = arrayOf("_id", "name", "value")
        val cursor = appCtx.contentResolver.query(Uri.parse("content://settings/$settings"), columns, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                list.add("${it.getString(1)}=${it.getString(2)}")
            }
        }

        return list
    }

}