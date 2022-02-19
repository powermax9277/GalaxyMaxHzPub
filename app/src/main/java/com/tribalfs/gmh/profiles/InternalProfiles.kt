package com.tribalfs.gmh.profiles

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import com.google.gson.Gson
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.ignoreRrmChange
import com.tribalfs.gmh.helpers.UtilsReso.getName
import com.tribalfs.gmh.profiles.ProfilesObj.refreshRateModeMap
import kotlinx.coroutines.*
import org.json.JSONObject

object InternalProfiles {
    private val mLock = Any()


    private fun getKey(contentResolver: ContentResolver): String{
        //return if (mUtilsDeviceInfo.deviceIsSamsung) {
        synchronized(mLock) {
            return "$displayId-${
                Settings.Secure.getString(
                    contentResolver,
                    REFRESH_RATE_MODE
                ) ?: 0
            }"
        }
    }


    private fun isModeProfilesAdded(key: String?, contentResolver: ContentResolver): Boolean{
        synchronized(mLock) {
            val mKey = key ?: getKey(contentResolver)
            return refreshRateModeMap.containsKey(mKey)
        }
    }



    suspend fun loadToProfilesObj(currentModeOnly: Boolean, overwriteExisting: Boolean, appCtx: Context): JSONObject = withContext(Dispatchers.IO) {
        if (!currentModeOnly && hasWriteSecureSetPerm){

            val originalRefreshRateMode = UtilRefreshRateSt.instance(appCtx).getRefreshRateMode()
            var endingRefreshRateMode = originalRefreshRateMode
            var modeAddedCnt = 0
            try {
                refreshRateModes.forEach { rrm ->
                    val key = "$displayId-$rrm"
                    if (overwriteExisting || !isModeProfilesAdded(key,appCtx.contentResolver)) {
                        ignoreRrmChange.set(true)
                        UtilRefreshRateSt.instance(appCtx).setRefreshRateMode(rrm)
                        endingRefreshRateMode = rrm
                        delay(1000)
                        if (addModeToProfileObj(appCtx, key)) {
                            modeAddedCnt += 0
                        }
                    }
                }

            } catch (_: Exception) { }


            //restore user refresh rate mode
            if (originalRefreshRateMode != endingRefreshRateMode) {
                //assert(hasWriteSecureSetPerm)
                UtilRefreshRateSt.instance(appCtx).setRefreshRateMode(originalRefreshRateMode)
            }

        } else {
            //add current DisplayMode only in case syncs results below is empty or failed
            if (overwriteExisting ||  !isModeProfilesAdded(null, appCtx.contentResolver)) {
                val key = getKey(appCtx.contentResolver)
                addModeToProfileObj(appCtx, key)
            }
        }

        return@withContext getLoadedDisplayModesInJson()

    }


    private fun getLoadedDisplayModesInJson(): JSONObject {
        val gson = Gson()
        val keyJson = JSONObject()
        refreshRateModeMap.keys.forEach { main_key ->
            val resDetailsList = refreshRateModeMap[main_key]
            val resStrJson = JSONObject()
            for (i in resDetailsList!!.indices) {//list of resStr-Details pair
                resDetailsList[i].keys.forEach { resStrLxw ->//only one key
                    val detailsJson = gson.toJson(resDetailsList[i][resStrLxw])
                    resStrJson.put(resStrLxw, detailsJson)
                }
            }
            keyJson.put(main_key, resStrJson)
        }
        return keyJson
    }

    private fun addModeToProfileObj(appCtx: Context, key: String?): Boolean {
        synchronized(mLock) {
            try {
                val resMapList = mutableListOf<Map<String, ResolutionDetails>>()

                for (mode in UtilsDeviceInfoSt.instance(appCtx).getDisplayModesSet()) {
                    val resMap = mutableMapOf<String, ResolutionDetails>()
                    val resSplit = mode.key.split("x")
                    val h = resSplit[0].toInt()
                    val w = resSplit[1].toInt()
                    resMap[mode.key] = ResolutionDetails(
                        h,//height
                        w,//width
                        mode.key,//resLxw
                        getName(h, w),
                        mode.value,//[refresh rates]
                        mode.value.minOrNull()!!,//lowest refresh rate
                        mode.value.maxOrNull()!!//highest refresh rate
                    )

                    resMapList.add(resMap)
                }
                refreshRateModeMap[key ?: getKey(appCtx.contentResolver)] = resMapList

                return true
            } catch (_: Exception) {
                return false
            }
        }
    }
}