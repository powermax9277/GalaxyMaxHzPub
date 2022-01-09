package com.tribalfs.gmh

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
import com.tribalfs.gmh.MyApplication.Companion.ignoreAccessibilityChange
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import kotlinx.coroutines.*


object AccessibilityPermission {

    fun isAccessibilityEnabled(
        context: Context,
        service: Class<out AccessibilityService?>
    ): Boolean {
        val mContentResolver = context.applicationContext.contentResolver
        val gmhAccessibilityStr =  "${context.applicationContext.packageName}/${service.name}"
        return (Settings.Secure.getString(mContentResolver, ENABLED_ACCESSIBILITY_SERVICES) ?: "").contains(gmhAccessibilityStr)
    }


    @ExperimentalCoroutinesApi
    @Synchronized
    fun allowAccessibility(context: Context, service: Class<out AccessibilityService?>, add: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val mContentResolver = context.applicationContext.contentResolver
            val gmhAccessibilityStr =  "${context.applicationContext.packageName}/${service.name}"
            (Settings.Secure.getString(mContentResolver, ENABLED_ACCESSIBILITY_SERVICES) ?: "").let {
                if (hasWriteSecureSetPerm) {
                    ignoreAccessibilityChange = true
                    var str = it
                    while (str.contains(":$gmhAccessibilityStr")) {
                        str = str.replace(":$gmhAccessibilityStr", "")
                    }
                    while (str.contains(gmhAccessibilityStr)) {
                        str = str.replace(gmhAccessibilityStr, "")
                    }
                    Settings.Secure.putString(mContentResolver, ENABLED_ACCESSIBILITY_SERVICES, str)

                    if (add) {
                        delay(900)
                        Settings.Secure.putString(mContentResolver, ENABLED_ACCESSIBILITY_SERVICES, if (str.isNotEmpty()) "$str:$gmhAccessibilityStr" else gmhAccessibilityStr)
                    }
                }
            }
        }
    }

}