package com.tribalfs.gmh

import android.content.Context
import android.provider.Settings
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.SingletonHolder


internal class SensorsOffSt(context: Context) {

    companion object : SingletonHolder<SensorsOffSt, Context>(::SensorsOffSt) {
        private const val sensorsOffVal = "custom(com.android.settings/.development.qstile.DevelopmentTiles\$SensorsOff)"
        internal const val SYSUI_QS_TILES = "sysui_qs_tiles"
    }

    private val appCtx = context.applicationContext

    fun checkQsTileInPlace(): Boolean {
        val sysuiValues = Settings.Secure.getString(appCtx.contentResolver, SYSUI_QS_TILES)
        val isInPlace = sysuiValues.split(",").indexOf(sensorsOffVal).let{it != -1 && it < 4}

        return if (isInPlace){
            true
        }else{
            placeQSTile(sysuiValues)
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

        *//*val classes = Class.forName("com.android.settings.development.qstile.DevelopmentTiles").declaredClasses

                var sensorTile: Class<*>? = null
                for (cls in classes){
                    if (cls.simpleName == "SensorsOff"){
                        sensorTile = cls
                        break
                    }
                }
                sensorTile?.getDeclaredMethod("setIsEnabled", Boolean::class.java)?.invoke(null, true)*//*

    }*/


    private fun placeQSTile(sysuiValues: String): Boolean{
        return if (hasWriteSecureSetPerm) {
            if (sysuiValues.contains(sensorsOffVal)) {
                //var newSysuiValues = sensorsOffVal
                val newSysuiValues: ArrayList<String> = sysuiValues.split(",").filter { it != sensorsOffVal } as ArrayList<String>
                newSysuiValues.add(3, sensorsOffVal)
                Settings.Secure.putString(appCtx.contentResolver, SYSUI_QS_TILES, newSysuiValues.joinToString(","))

            } else {
                false
            }
        }else {
            false
        }
    }
}
