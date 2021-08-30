package com.tribalfs.gmh.profiles

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.tribalfs.gmh.BuildConfig
import com.tribalfs.gmh.MainActivity.Companion.GMH_WEB_APP
import com.tribalfs.gmh.helpers.Certificate
import com.tribalfs.gmh.helpers.UtilsDeviceInfo
import com.tribalfs.gmh.helpers.UtilsSettings
import com.tribalfs.gmh.helpers.UtilsSettings.SECURE
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@SuppressLint("HardwareIds")
internal class Syncer(context: Context) {
    private val appCtx = context.applicationContext
    private val mUtilsDeviceInfo by lazy { UtilsDeviceInfo(appCtx) }
    private val mUtilsPrefsAct by lazy { UtilsPrefsAct(appCtx) }
    private val mUtilsPrefsGmh by lazy { UtilsPrefsGmh(appCtx) }

    companion object{
        private const val TAG = "ProfilesSync"
        private const val REQUEST_DISPLAY_MODES_FETCH = 0x3
        private const val REQUEST_DISPLAY_MODES_POST = 0x4
        internal const val KEY_JSON_REFRESH_RATES_PROFILE = "0x13"
        internal const val KEY_JSON_ADAPTIVES = "0x14"
        private const val KEY_JSON_ONEUI_VERSION = "0x15"
        private const val KEY_JSON_APP_VERSION_CODE = "0x17"

        internal const val REQUEST_BUY_LINK = 0x0
        //internal const val REQUEST_SYNC_INS_DATE = 0x1
        internal const val REQUEST_VERIFY_LICENSE = 0x2
        internal const val JSON_RESPONSE_OK = 0x10
        internal const val KEY_JSON_RESULT = "0x0"
        internal const val KEY_JSON_DEVICE_ID = "0x2"
        internal const val KEY_JSON_MODEL_NUMBER = "0x3"
        internal const val KEY_JSON_ACTIVATION_CODE = "0x4"
        internal const val KEY_JSON_SIGNATURE = "0x5"
        internal const val KEY_JSON_TRIAL = "0x16"
       // internal const val KEY_JSON_INS_DATE_SYNCMODE = "0x6"
        //internal const val KEY_JSON_INS_DATE = "0x7"
        //internal const val KEY_JSON_EXPIRY_DAYS = "0x8"
        internal const val KEY_JSON_TRIAL_START_DATE = "0x9"
        internal const val KEY_JSON_TRIAL_DAYS = "0x10"

        private const val REQUEST_SETTINGS_LIST_POST = 0x12
        private const val KEY_JSON_SECURE_LIST = "0x6"
        private const val KEY_JSON_SYSTEM_LIST = "0x15"
        private const val KEY_JSON_GLOBAL_LIST = "0x17"
    }

    private val deviceId by lazy {
        Settings.Secure.getString(appCtx.contentResolver,
            Settings.Secure.ANDROID_ID
        )}
    
    @ExperimentalCoroutinesApi
    @RequiresApi(VERSION_CODES.M)
    suspend fun fetchProfileFromBackEnd(): JSONObject? = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put(KEY_JSON_MODEL_NUMBER, mUtilsDeviceInfo.deviceModelVariant)
            put(KEY_JSON_ONEUI_VERSION, mUtilsDeviceInfo.oneUiVersion)
            put(KEY_JSON_SIGNATURE, Certificate.getEncSig(appCtx))
            put(KEY_JSON_APP_VERSION_CODE, BuildConfig.VERSION_CODE)
        }

        Log.d(TAG, "Starting sync FETCH...")
        val result = postDataVolley(REQUEST_DISPLAY_MODES_FETCH, GMH_WEB_APP, jsonBody)
        return@withContext if (result != null && result[KEY_JSON_RESULT] == JSON_RESPONSE_OK && result[KEY_JSON_REFRESH_RATES_PROFILE] != "") {
            result
        } else {
            null
        }
    }


    @ExperimentalCoroutinesApi
    @RequiresApi(VERSION_CODES.M)
    suspend fun postProfileToBackEnd(): JSONObject? = withContext(Dispatchers.IO) {
        // Log.d(TAG, "Starting sync POST...")
        val jsonBody = JSONObject().apply {
            put(KEY_JSON_MODEL_NUMBER, mUtilsDeviceInfo.deviceModelVariant)
            put(KEY_JSON_ONEUI_VERSION, mUtilsDeviceInfo.oneUiVersion)
            put(KEY_JSON_SIGNATURE, Certificate.getEncSig(appCtx))
            put(KEY_JSON_REFRESH_RATES_PROFILE, mUtilsPrefsGmh.gmhPrefDisplayModesObjectInJson)
        }

        val result = postDataVolley(REQUEST_DISPLAY_MODES_POST, GMH_WEB_APP, jsonBody)
        return@withContext if (result != null && result[KEY_JSON_RESULT] == JSON_RESPONSE_OK) {
            result
        } else {
            null
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun syncLicense(tryTrial: Boolean): JSONObject? = withContext(Dispatchers.IO) {
        val jsonObject = JSONObject().apply {
            put(KEY_JSON_DEVICE_ID, deviceId)
            put(KEY_JSON_MODEL_NUMBER, mUtilsDeviceInfo.deviceModelVariant)
            put(
                KEY_JSON_ACTIVATION_CODE,
                mUtilsPrefsAct.gmhPrefActivationCode ?: ""
            )//don't remove
            put(KEY_JSON_SIGNATURE, Certificate.getEncSig(appCtx))
            put(KEY_JSON_TRIAL, tryTrial)
        }
        val result = postDataVolley(REQUEST_VERIFY_LICENSE, GMH_WEB_APP, jsonObject)
        return@withContext if (result != null && result[KEY_JSON_RESULT] == JSON_RESPONSE_OK) {
            result
        } else {
            null
        }
    }

    suspend fun openBuyAdFreeLink(): JSONObject? = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put(KEY_JSON_DEVICE_ID, deviceId)
            put(KEY_JSON_MODEL_NUMBER, mUtilsDeviceInfo.deviceModelVariant)
        }

        val result = postDataVolley(REQUEST_BUY_LINK, GMH_WEB_APP, jsonBody)
        return@withContext if (result != null && result[KEY_JSON_RESULT] == JSON_RESPONSE_OK){
            result
        } else {
            null
        }
    }

/*    suspend fun syncInsDate(instDateSyncMode: String, expiryDays: Int?): JSONObject? = withContext(Dispatchers.IO) {
        Log.d(TAG, "syncInsDate() called")
        val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())

        val jsonBody = JSONObject().apply {
            put(KEY_JSON_DEVICE_ID, deviceId)
            put(KEY_JSON_MODEL_NUMBER, mUtilsDeviceInfo.deviceModelVariant)
            put(KEY_JSON_INS_DATE, (sdf.format(mUtilsPrefsAct.gmhPrefGetInstallDate ?: Calendar.getInstance().time)))
            put(KEY_JSON_EXPIRY_DAYS, expiryDays)
            put(KEY_JSON_INS_DATE_SYNCMODE, instDateSyncMode)
        }

        val result = postDataVolley(REQUEST_SYNC_INS_DATE, GMH_WEB_APP, jsonBody)
        return@withContext if (result != null && result[KEY_JSON_RESULT] == JSON_RESPONSE_OK){
            result
        } else {
            null
        }
    }*/

    @ExperimentalCoroutinesApi
    @RequiresApi(VERSION_CODES.M)
    suspend fun postSettingsList() = withContext(Dispatchers.IO){
        Log.d(TAG, "Starting sync POST...")

        val deferreds = listOf(
            // fetch  at the same time
            async{ UtilsSettings.getList(SECURE, appCtx)},
            async{ UtilsSettings.getList(UtilsSettings.SYSTEM, appCtx)},
            async{ UtilsSettings.getList(UtilsSettings.GLOBAL, appCtx)}
        )
        deferreds.awaitAll()

        val jsonBody = JSONObject().apply {
            put(KEY_JSON_MODEL_NUMBER, mUtilsDeviceInfo.deviceModelVariant)
            put(KEY_JSON_SECURE_LIST, deferreds[0].getCompleted().joinToString(";"))
            put(KEY_JSON_SYSTEM_LIST, deferreds[1].getCompleted().joinToString(";"))
            put(KEY_JSON_GLOBAL_LIST, deferreds[2].getCompleted().joinToString(";"))
        }

       postDataVolley(
            REQUEST_SETTINGS_LIST_POST,
            GMH_WEB_APP,
            jsonBody
        )
    }


    private suspend fun postDataVolley(requestType: Int, url: String, sendObj: JSONObject?)  = suspendCoroutine<JSONObject?> {
        try {
            val queue = Volley.newRequestQueue(appCtx)
            val jsonObjReq = JsonObjectRequest(
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