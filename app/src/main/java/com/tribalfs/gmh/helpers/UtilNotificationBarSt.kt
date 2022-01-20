package com.tribalfs.gmh.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("WrongConstant", "PrivateApi")
internal class UtilNotificationBarSt private constructor (context: Context) {

    companion object : SingletonMaker<UtilNotificationBarSt, Context>(::UtilNotificationBarSt){
        private const val sensorsOffVal = "custom(com.android.settings/.development.qstile.DevelopmentTiles\$SensorsOff)"
        internal const val SYSUI_QS_TILES = "sysui_qs_tiles"
    }

    private val appCtx = context.applicationContext

    private val sensorPrivacyService by lazy {appCtx.getSystemService("sensor_privacy")}
    private val sensorPrivacyManager by lazy {Class.forName("android.hardware.SensorPrivacyManager")}

    private val statusbarService by lazy{ appCtx.getSystemService( "statusbar")}
    private val statusBarManager by lazy{ Class.forName("android.app.StatusBarManager")}

    private val isPermitted: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        (appCtx.checkSelfPermission(Manifest.permission.EXPAND_STATUS_BAR) == PERMISSION_GRANTED)
    } else {
        true
    }

    @SuppressLint("WrongConstant")
    fun expandNotificationBar() {
        if (!isPermitted) return
        try {
            statusbarService?.let{
                statusBarManager.getMethod("expandSettingsPanel").invoke(it)
            }
        } catch (e: Exception) {
            try {
            Toast.makeText(appCtx, e.localizedMessage, Toast.LENGTH_SHORT)
                .show()
            }catch(_: Exception){}
        }
    }

    @SuppressLint("WrongConstant")
    fun collapseNotificationBar(): Boolean {
        if (!isPermitted) return false
        return try {
            statusBarManager.getMethod("collapsePanels").invoke(statusbarService)
            true
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(appCtx, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
            false
        }
    }


    fun checkQsTileInPlace(): Boolean? {
        val sysUiValues = Settings.Secure.getString(appCtx.contentResolver,
            SYSUI_QS_TILES
        )
        val idx = sysUiValues.split(",").indexOf(sensorsOffVal)

        return when {
            (idx in 0..3) -> {
                true
            }
            (idx != -1) -> {
                placeQSTile(sysUiValues)
            }
            else ->{
                false
            }

        }
    }

    private val sdkAtleastS =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.S


    fun isSensorsOff(): Boolean? {
        if (sdkAtleastS) return null
        return try {
            sensorPrivacyManager.getDeclaredMethod("isSensorPrivacyEnabled").invoke(sensorPrivacyService) as Boolean
        }catch(_: Exception){
            (CheckBlacklistApiSt.instance(appCtx).setAllowed())
            /*launch(Dispatchers.Main) {
                Toast.makeText(
                    applicationContext,
                    "Error reading device sensors state. Reboot this device and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }*/
            null
        }
    }


    private fun placeQSTile(sysuiValues: String): Boolean?{
        return if (hasWriteSecureSetPerm) {
            if (sysuiValues.contains(sensorsOffVal)) {
                val newSysUiValues: ArrayList<String> = sysuiValues.split(",").filter { it != sensorsOffVal } as ArrayList<String>
                newSysUiValues.add(3, sensorsOffVal)
                Settings.Secure.putString(appCtx.contentResolver,
                    SYSUI_QS_TILES, newSysUiValues.joinToString(","))
            } else {
                false
            }
        }else {
            null
        }
    }

    /*
    @SuppressLint("PrivateApi", "BlockedPrivateApi")
    private fun isSensorOff(): Boolean {
        @SuppressLint("WrongConstant")
        val sensorPrivacyService: Any? = appCtx.getSystemService("sensor_privacy")
        return Class.forName("android.hardware.SensorPrivacyManager")
            .getDeclaredMethod("isSensorPrivacyEnabled")
            .invoke(sensorPrivacyService) as Boolean
    }

    private fun isDevModeEnabled():Boolean{
        return UtilsSettingsSt.get(appCtx).getConfig(
            GLOBAL,
            DEVELOPMENT_SETTINGS_ENABLED
        ) == "1"

        val classes = Class.forName("com.android.settings.development.qstile.DevelopmentTiles").declaredClasses

                var sensorTile: Class<*>? = null
                for (cls in classes){
                    if (cls.simpleName == "SensorsOff"){
                        sensorTile = cls
                        break
                    }
                }
                sensorTile?.getDeclaredMethod("setIsEnabled", Boolean::class.java)?.invoke(null, true)

    }*/


}

