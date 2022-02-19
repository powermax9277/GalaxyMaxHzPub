package com.tribalfs.gmh.helpers

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.BuildConfig
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.UtilSettingsIntents.changeSystemSettingsIntent


class UtilPermSt private constructor(val appCtx: Context){

    companion object: SingletonMaker<UtilPermSt, Context>(::UtilPermSt){
        internal const val CHANGE_SETTINGS = -11
    }

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

    
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestWriteSettings() {
        Toast.makeText(appCtx, appCtx.getString(R.string.enable_write_settings), Toast.LENGTH_LONG).show()
        val intent = changeSystemSettingsIntent.apply {
            flags = FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
        }
        appCtx.startActivity(intent)
    }



}


