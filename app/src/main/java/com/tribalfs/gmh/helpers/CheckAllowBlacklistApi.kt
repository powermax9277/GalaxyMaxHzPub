package com.tribalfs.gmh.helpers

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm

internal class CheckBlacklistApiSt private constructor(context: Context) {

    companion object : SingletonMaker<CheckBlacklistApiSt, Context>(::CheckBlacklistApiSt) {
        private const val HAP = "hidden_api_policy"
        private const val HAP_PRE_P = "hidden_api_policy_pre_p_apps"
        private const val HAP_P = "hidden_api_policy_p_apps"
    }

    private val appCtx = context.applicationContext
    private val mResolver = appCtx.contentResolver

    fun isAllowed(): Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Global.getString(mResolver, HAP) == "1"
        }else {
            Settings.Global.getString(mResolver, HAP_PRE_P) == "1" &&
                    Settings.Global.getString(mResolver, HAP_P) == "1"

        }
    }

    fun setAllowed(): Boolean{
        return if (hasWriteSecureSetPerm) {
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    Settings.Global.putString(mResolver, HAP, "1")
                } else {
                    Settings.Global.putString(mResolver, HAP_PRE_P, "1") &&
                            Settings.Global.putString(mResolver, HAP_P, "1")
                }
            }catch (_: Exception){
                false
            }
        }else{
            false
        }
    }

}