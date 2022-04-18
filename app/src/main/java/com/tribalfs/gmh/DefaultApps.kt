package com.tribalfs.gmh

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.provider.Settings.Secure.DEFAULT_INPUT_METHOD

object DefaultApps {
    internal fun getLauncher(context: Context): String {
        val intent = Intent("android.intent.action.MAIN")
        intent.addCategory("android.intent.category.HOME")
        return context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )!!.activityInfo.packageName
    }

    internal fun getKeyboard(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, DEFAULT_INPUT_METHOD)!!.split("/")[0]
    }


}
