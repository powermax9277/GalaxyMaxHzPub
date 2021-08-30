package com.tribalfs.gmh

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object DefaultApps {
    fun getLauncher(context: Context): String {
        val intent = Intent("android.intent.action.MAIN")
        intent.addCategory("android.intent.category.HOME")
        return context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )!!.activityInfo.packageName
    }

    /*fun getKeyboard(context: Context): String {
        return UtilsSettingsSt.get(context.applicationContext).getConfig(SECURE, DEFAULT_INPUT_METHOD)!!.split("/")[0]
    }*/

}