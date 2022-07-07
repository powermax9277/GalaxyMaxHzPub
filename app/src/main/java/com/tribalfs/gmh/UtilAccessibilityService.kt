package com.tribalfs.gmh

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.MyApplication.Companion.ignoreAccessibilityChange
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.*


@OptIn(ExperimentalCoroutinesApi::class)
object UtilAccessibilityService {

    @RequiresApi(Build.VERSION_CODES.M)
    private val serviceName = GalaxyMaxHzAccess::class.java.name

    private val mLock = Any()
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun isAccessibilityEnabled(appCtx: Context): Boolean {
        synchronized(mLock) {
            val mContentResolver = appCtx.contentResolver
            val gmhAccessibilityStr = "${appCtx.packageName}/$serviceName"
            return (Settings.Secure.getString(mContentResolver, ENABLED_ACCESSIBILITY_SERVICES)
                ?: "").contains(gmhAccessibilityStr)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    internal fun allowAccessibility(appCtx: Context, add: Boolean) {
        synchronized(mLock) {
            CoroutineScope(Dispatchers.IO).launch {
                val mContentResolver = appCtx.contentResolver
                val gmhAccessibilityStr = "${appCtx.packageName}/$serviceName"
                (Settings.Secure.getString(mContentResolver, ENABLED_ACCESSIBILITY_SERVICES) ?: "").let {
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
                            delay(2000)
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

    @RequiresApi(Build.VERSION_CODES.M)
    internal fun checkAccessibility(required:Boolean?, appCtx: Context): Boolean{
        synchronized(mLock) {
            val accessRequired =
                required ?: UtilsPrefsGmhSt.instance(appCtx).getEnabledAccessibilityFeatures()
                    .isNotEmpty()
            return if (accessRequired) {
                if (
                   hasWriteSecureSetPerm
                  /*  (isSpayInstalled == false || UtilsPrefsGmhSt.instance(appCtx).hzPrefSPayUsage == NOT_USING) && hasWriteSecureSetPerm*/
                ) {
                    allowAccessibility(appCtx, true)
                    true
                } else {
                    false
                }
            } else {
                gmhAccessInstance != null
            }
        }
    }

}