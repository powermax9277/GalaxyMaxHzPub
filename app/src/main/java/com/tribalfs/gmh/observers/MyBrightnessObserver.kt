package com.tribalfs.gmh.observers

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.Settings
import com.tribalfs.gmh.MyApplication
import com.tribalfs.gmh.helpers.CacheSettings.currentBrightness
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.SCREEN_BRIGHTNESS_FLOAT
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
internal class MyBrightnessObserver(h: Handler?, private val appCtx: Context) : ContentObserver(h) {

    private val brightnessFloatUri = Settings.System.getUriFor(SCREEN_BRIGHTNESS_FLOAT)
    private val brightnessUri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS)

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        when (uri) {
            brightnessFloatUri, brightnessUri -> {
                MyApplication.appScopeIO.launch {
                    if (isScreenOn.get()) {
                        currentBrightness.set(UtilsDeviceInfoSt.instance(appCtx).getScreenBrightnessPercent())
                    }
                }
            }
        }
    }


    fun start(){
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            appCtx.contentResolver.registerContentObserver(
                brightnessFloatUri, false, this
            )
        }else {
            appCtx.contentResolver.registerContentObserver(
                brightnessUri, false, this
            )
        }

    }

    fun stop(){
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            appCtx.contentResolver.registerContentObserver(
                brightnessFloatUri, false, this
            )
        }else{
            appCtx.contentResolver.registerContentObserver(
                brightnessUri, false, this)

        }
    }

}
