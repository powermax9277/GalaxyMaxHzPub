package com.tribalfs.gmh.profiles

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.ANDROID_ID
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.tribalfs.gmh.BuildConfig
import com.tribalfs.gmh.GMH_WEB_APP
import com.tribalfs.gmh.helpers.PackageInfo
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val REQUEST_DISPLAY_MODES_FETCH = 0x3
private const val REQUEST_DISPLAY_MODES_POST = 0x4
internal const val KEY_JSON_REFRESH_RATES_PROFILE = "0x13"
internal const val KEY_JSON_ADAPTIVES = "0x14"
private const val KEY_JSON_ONEUI_VERSION = "0x15"
private const val KEY_JSON_APP_VERSION_CODE = "0x17"
internal const val REQUEST_BUY_LINK = 0x0
internal const val REQUEST_VERIFY_LICENSE = 0x2
internal const val JSON_RESPONSE_OK = 0x10
internal const val KEY_JSON_RESULT = "0x0"
internal const val KEY_JSON_DEVICE_ID = "0x2"
internal const val KEY_JSON_MODEL_NUMBER = "0x3"
internal const val KEY_JSON_ACTIVATION_CODE = "0x4"
internal const val KEY_JSON_SIGNATURE = "0x5"
internal const val KEY_JSON_TRIAL = "0x16"
internal const val KEY_JSON_TRIAL_START_DATE = "0x9"
internal const val KEY_JSON_TRIAL_DAYS = "0x10"
/*private const val REQUEST_SETTINGS_LIST_POST = 0x12
private const val KEY_JSON_SECURE_LIST = "0x6"
private const val KEY_JSON_SYSTEM_LIST = "0x15"
private const val KEY_JSON_GLOBAL_LIST = "0x17"*/
private const val REQUEST_HELP_URL = 0x15

@SuppressLint("HardwareIds")
internal class Syncer(private val context: Context) {

   // private val context.applicationContext = context.applicationContext
    //private val UtilDeviceInfoSt.instance(context.applicationContext) by lazy { UtilDeviceInfoSt.instance(context.applicationContext) }
   // private val UtilsPrefsGmhSt.instance(context.applicationContext) by lazy { UtilsPrefsGmhSt.instance(context.applicationContext) }

    private val deviceId by lazy {
        Settings.Secure.getString(context.applicationContext.contentResolver, ANDROID_ID)
    }

    suspend fun fetchProfileFromBackEnd(): JSONObject? = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put(KEY_JSON_MODEL_NUMBER, UtilsDeviceInfoSt.instance(context.applicationContext).deviceModelVariant)
            put(KEY_JSON_ONEUI_VERSION, UtilsDeviceInfoSt.instance(context.applicationContext).oneUiVersion)
            put(KEY_JSON_SIGNATURE, PackageInfo.getSignatureString(context.applicationContext))
            put(KEY_JSON_APP_VERSION_CODE, BuildConfig.VERSION_CODE)
        }

        val result = postDataVolley(REQUEST_DISPLAY_MODES_FETCH, GMH_WEB_APP, jsonBody)
        return@withContext if (result != null && result[KEY_JSON_RESULT] == JSON_RESPONSE_OK && result[KEY_JSON_REFRESH_RATES_PROFILE] != "") {
            result
        } else {
            null
        }
    }


    suspend fun postProfileToBackEnd(): JSONObject? = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put(KEY_JSON_MODEL_NUMBER, UtilsDeviceInfoSt.instance(context.applicationContext).deviceModelVariant)
            put(KEY_JSON_ONEUI_VERSION, UtilsDeviceInfoSt.instance(context.applicationContext).oneUiVersion)
            put(KEY_JSON_SIGNATURE, PackageInfo.getSignatureString(context.applicationContext))
            put(KEY_JSON_REFRESH_RATES_PROFILE, UtilsPrefsGmhSt.instance(context.applicationContext).gmhPrefDisplayModesObjectInJson)
        }

        val result = postDataVolley(REQUEST_DISPLAY_MODES_POST, GMH_WEB_APP, jsonBody)
        return@withContext if (result != null && result[KEY_JSON_RESULT] == JSON_RESPONSE_OK) {
            result
        } else {
            null
        }

    }

    
    suspend fun syncLicense(activationCode: String, trial: Boolean): JSONObject? = withContext(Dispatchers.IO) {
        val jsonObject = JSONObject().apply {
            put(KEY_JSON_DEVICE_ID, deviceId)
            put(KEY_JSON_MODEL_NUMBER, UtilsDeviceInfoSt.instance(context.applicationContext).deviceModelVariant)
            put(KEY_JSON_ACTIVATION_CODE, activationCode)//don't remove
            put(KEY_JSON_SIGNATURE, PackageInfo.getSignatureString(context.applicationContext))
            put(KEY_JSON_TRIAL, trial)
        }
        val result = postDataVolley(REQUEST_VERIFY_LICENSE, GMH_WEB_APP, jsonObject)
        return@withContext if (result != null && result[KEY_JSON_RESULT] == JSON_RESPONSE_OK) {
            result
        } else {
            null
        }
    }

    
    suspend fun getBuyAdFreeLink(): JSONObject? = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put(KEY_JSON_DEVICE_ID, deviceId)
            put(KEY_JSON_MODEL_NUMBER, UtilsDeviceInfoSt.instance(context.applicationContext).deviceModelVariant)
        }

        val result = postDataVolley(REQUEST_BUY_LINK, GMH_WEB_APP, jsonBody)
        return@withContext if (result != null && result[KEY_JSON_RESULT] == JSON_RESPONSE_OK){
            result
        } else {
            null
        }
    }

    
    suspend fun getHelpUrl(): JSONObject? = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put(KEY_JSON_MODEL_NUMBER, UtilsDeviceInfoSt.instance(context.applicationContext).deviceModelVariant)
        }

        val result = postDataVolley(REQUEST_HELP_URL, GMH_WEB_APP, jsonBody)
        return@withContext if (result != null && result[KEY_JSON_RESULT] == JSON_RESPONSE_OK){
            result
        } else {
            null
        }
    }


/*
    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun postSettingsList() = withContext(Dispatchers.IO){
        val settingsListAsync = listOf(
            // get simultaneously
            async{ UtilsDeviceInfoSt.instance(context.applicationContext).getSettingsList(SECURE)},
            async{ UtilsDeviceInfoSt.instance(context.applicationContext).getSettingsList(SYSTEM)},
            async{ UtilsDeviceInfoSt.instance(context.applicationContext).getSettingsList(GLOBAL)}
        )
        settingsListAsync.awaitAll()

        val jsonBody = JSONObject().apply {
            put(KEY_JSON_MODEL_NUMBER, UtilsDeviceInfoSt.instance(context.applicationContext).deviceModelVariant)
            put(KEY_JSON_SECURE_LIST, settingsListAsync[0].getCompleted().joinToString(";"))
            put(KEY_JSON_SYSTEM_LIST, settingsListAsync[1].getCompleted().joinToString(";"))
            put(KEY_JSON_GLOBAL_LIST, settingsListAsync[2].getCompleted().joinToString(";"))
        }

       postDataVolley(
            REQUEST_SETTINGS_LIST_POST,
            GMH_WEB_APP,
            jsonBody
        )
    }
*/


    private suspend fun postDataVolley(requestType: Int, url: String, sendObj: JSONObject?)  = suspendCoroutine<JSONObject?> {
        try {
            val queue = Volley.newRequestQueue(context.applicationContext)
            val jsonObjReq = JsonObjectRequest(
                Request.Method.POST,
                "$url?Rq=$requestType", sendObj,
                { response ->
                    it.resume(response)
                })
            { _ ->
                it.resume(null)
            }
            queue.add(jsonObjReq)
        } catch (_: Exception) {
            it.resume(null)
        }
    }

/*  private suspend fun getDataVolley(requestType: Int, url: String, sendObj: JSONObject?,mContext: Context)  = suspendCoroutine<JSONObject?> {
        try {
            val queue = Volley.newRequestQueue(mContext)
            val jsonObj = JsonObjectRequest(
                Request.Method.GET,
                "$url?Rq=$requestType", sendObj,
                { response ->
                     it.resume(response)
                },
                { _ ->
               it.resume(null)
                })
            queue.add(jsonObj)
        } catch (_: Exception) {
              it.resume(null)
        }
    }*/

}