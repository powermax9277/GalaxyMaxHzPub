package com.tribalfs.gmh

import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
import com.tribalfs.gmh.MyApplication.Companion.ignoreAccessibilityChange
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import kotlinx.coroutines.*


object UtilAccessibilityService {

    @ExperimentalCoroutinesApi
    private val serviceName = GalaxyMaxHzAccess::class.java.name

    internal fun isAccessibilityEnabled(
        context: Context
    ): Boolean {
        synchronized(this) {
            val mContentResolver = context.applicationContext.contentResolver
            val gmhAccessibilityStr = "${context.applicationContext.packageName}/$serviceName"
            return (Settings.Secure.getString(mContentResolver, ENABLED_ACCESSIBILITY_SERVICES)
                ?: "").contains(gmhAccessibilityStr)
        }
    }

    internal fun allowAccessibility(context: Context, add: Boolean) {
        synchronized(this) {
            CoroutineScope(Dispatchers.IO).launch {
                val mContentResolver = context.applicationContext.contentResolver
                val gmhAccessibilityStr =
                    "${context.applicationContext.packageName}/$serviceName"
                (Settings.Secure.getString(mContentResolver, ENABLED_ACCESSIBILITY_SERVICES)
                    ?: "").let {
                    if (hasWriteSecureSetPerm) {
                        var str = it
                        while (str.contains(":$gmhAccessibilityStr")) {
                            str = str.replace(":$gmhAccessibilityStr", "")
                        }
                        while (str.contains(gmhAccessibilityStr)) {
                            str = str.replace(gmhAccessibilityStr, "")
                        }
                        ignoreAccessibilityChange = true
                        Settings.Secure.putString(
                            mContentResolver,
                            ENABLED_ACCESSIBILITY_SERVICES,
                            str
                        )

                        if (add) {
                            delay(1000)
                            //needed as first call above will set this to false
                            ignoreAccessibilityChange = true
                            Settings.Secure.putString(
                                mContentResolver,
                                ENABLED_ACCESSIBILITY_SERVICES,
                                if (str.isNotEmpty()) "$str:$gmhAccessibilityStr" else gmhAccessibilityStr
                            )
                        }
                    }
                }
            }
        }
    }

    /*internal fun isAccessibilityEnabled(
        context: Context,
        service: Class<out AccessibilityService?>
    ): Boolean {
        synchronized(this) {
            val mContentResolver = context.applicationContext.contentResolver
            val gmhAccessibilityStr = "${context.applicationContext.packageName}/${service.name}"
            return (Settings.Secure.getString(mContentResolver, ENABLED_ACCESSIBILITY_SERVICES)
                ?: "").contains(gmhAccessibilityStr)
        }
    }

    internal fun allowAccessibility(context: Context, service: Class<out AccessibilityService?>, add: Boolean) {
        synchronized(this) {
            CoroutineScope(Dispatchers.IO).launch {
                val mContentResolver = context.applicationContext.contentResolver
                val gmhAccessibilityStr =
                    "${context.applicationContext.packageName}/${service.name}"
                (Settings.Secure.getString(mContentResolver, ENABLED_ACCESSIBILITY_SERVICES)
                    ?: "").let {
                    if (hasWriteSecureSetPerm) {
                        var str = it
                        while (str.contains(":$gmhAccessibilityStr")) {
                            str = str.replace(":$gmhAccessibilityStr", "")
                        }
                        while (str.contains(gmhAccessibilityStr)) {
                            str = str.replace(gmhAccessibilityStr, "")
                        }
                        ignoreAccessibilityChange = true
                        Settings.Secure.putString(
                            mContentResolver,
                            ENABLED_ACCESSIBILITY_SERVICES,
                            str
                        )

                        if (add) {
                            delay(1000)
                            //needed as first call above will set this to false
                            ignoreAccessibilityChange = true
                            Settings.Secure.putString(
                                mContentResolver,
                                ENABLED_ACCESSIBILITY_SERVICES,
                                if (str.isNotEmpty()) "$str:$gmhAccessibilityStr" else gmhAccessibilityStr
                            )
                        }
                    }
                }
            }
        }
    }
*/
}