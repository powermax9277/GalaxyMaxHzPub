package com.tribalfs.gmh.helpers

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Process
import android.provider.Settings
import com.tribalfs.gmh.helpers.UtilsSettings.GLOBAL
import com.tribalfs.gmh.helpers.UtilsSettings.SECURE
import com.tribalfs.gmh.helpers.UtilsSettings.SYSTEM


class UtilsPermSt(context: Context){

    companion object: SingletonHolder<UtilsPermSt, Context>(::UtilsPermSt){
        internal const val CHANGE_SETTINGS = -11
        internal const val REQUEST_CODE_APPEAR_ON_TOP_PERM = 5
    }

    private val appCtx: Context = context.applicationContext


    fun hasWriteSecurePerm(): Boolean{
        return getPerm(SECURE) == PERMISSION_GRANTED
    }

    fun hasWriteSystemPerm(): Boolean{
        return (getPerm(SYSTEM) == PERMISSION_GRANTED || hasWriteSecurePerm())
    }

    @SuppressLint("NewApi")
    fun getPerm(settingsGroup: String): Int {
        return when (settingsGroup) {
            SYSTEM -> {
                if (Settings.System.canWrite(appCtx)) {
                    PERMISSION_GRANTED
                } else {
                    CHANGE_SETTINGS
                }
            }
            SECURE, GLOBAL -> {
                appCtx.checkPermission(
                    WRITE_SECURE_SETTINGS,
                    Process.myPid(),
                    Process.myUid()
                )
            }
            else -> PERMISSION_DENIED
        }
    }


    @SuppressLint("NewApi")
    fun hasOverlayPerm(): Boolean {
        return Settings.canDrawOverlays(appCtx)
    }
}


