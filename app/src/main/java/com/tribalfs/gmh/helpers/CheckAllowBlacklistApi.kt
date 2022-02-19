package com.tribalfs.gmh.helpers

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm

private const val HAP = "hidden_api_policy"
private const val HAP_PRE_P = "hidden_api_policy_pre_p_apps"
private const val HAP_P = "hidden_api_policy_p_apps"

internal class CheckBlacklistApiSt private constructor(val appCtx: Context) {

    companion object : SingletonMaker<CheckBlacklistApiSt, Context>(::CheckBlacklistApiSt)

    fun isAllowed(): Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Global.getString(appCtx.contentResolver, HAP) == "1"
        }else {
            Settings.Global.getString(appCtx.contentResolver, HAP_PRE_P) == "1" &&
                    Settings.Global.getString(appCtx.contentResolver, HAP_P) == "1"

        }
    }

    fun setAllowed(): Boolean{
        return if (hasWriteSecureSetPerm) {
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    Settings.Global.putString(appCtx.contentResolver, HAP, "1")
                } else {
                    Settings.Global.putString(appCtx.contentResolver, HAP_PRE_P, "1") &&
                            Settings.Global.putString(appCtx.contentResolver, HAP_P, "1")
                }
            }catch (_: Exception){
                false
            }
        }else{
            false
        }
    }

}