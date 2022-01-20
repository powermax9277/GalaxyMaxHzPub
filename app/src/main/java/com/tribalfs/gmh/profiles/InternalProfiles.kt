package com.tribalfs.gmh.profiles

import android.content.ContentResolver
import android.provider.Settings
import com.google.gson.Gson
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.ignoreRrmChange
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE
import com.tribalfs.gmh.helpers.UtilsRefreshRateSt
import com.tribalfs.gmh.helpers.UtilsRefreshRateSt.Companion.refreshRateModes
import com.tribalfs.gmh.helpers.UtilsResoName.getName
import com.tribalfs.gmh.profiles.ProfilesObj.refreshRateModeMap
import kotlinx.coroutines.*
import org.json.JSONObject

object InternalProfiles {
    private val mLock = Any()

    @Synchronized
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

    @Synchronized
    internal fun isModeProfilesAdded(key: String?, contentResolver: ContentResolver): Boolean{
        val mKey = key ?: getKey(contentResolver)
        return refreshRateModeMap.containsKey(mKey)
    }

    

    suspend fun loadToProfilesObj(currentModeOnly: Boolean, overwriteExisting: Boolean, mUtilsRefreshRateSt: UtilsRefreshRateSt): JSONObject = withContext(Dispatchers.IO) {

        if (!currentModeOnly && hasWriteSecureSetPerm
            //Ensure that device is not resolution with no high refresh rate support
            /*&& (!isSamsung || mUtilsDeviceInfo.samRefreshRateMode != REFRESH_RATE_MODE_STANDARD)*/){

            val originalRefreshRateMode = mUtilsRefreshRateSt.samRefreshRateMode
            delay(100)
            var endingRefreshRateMode = originalRefreshRateMode
            //loadComplete = withContext(Dispatchers.IO) {
            var modeAddedCnt = 0
            try {
                refreshRateModes.forEach { rrm ->
                    val key = "$displayId-$rrm"
                    if (overwriteExisting || !isModeProfilesAdded(key, mUtilsRefreshRateSt.mContentResolver)) {
                        ignoreRrmChange = true
                        delay(200)
                        mUtilsRefreshRateSt.samRefreshRateMode = rrm
                        endingRefreshRateMode = rrm
                        delay(400)
                        if (addModeToProfileObj(mUtilsRefreshRateSt, key)) {
                            modeAddedCnt += 0
                        }
                    }
                    delay(200)
                }
            } catch (_: Exception) { }


            //restore user refresh rate mode
            if (originalRefreshRateMode != endingRefreshRateMode) {
                //assert(hasWriteSecureSetPerm)
                mUtilsRefreshRateSt.samRefreshRateMode = originalRefreshRateMode
            }

        } else {
            //add current DisplayMode only in case syncs results below is empty or failed
            if (overwriteExisting ||  !isModeProfilesAdded(null, mUtilsRefreshRateSt.mContentResolver)) {

                val key = getKey(mUtilsRefreshRateSt.mContentResolver)
                addModeToProfileObj(mUtilsRefreshRateSt, key)
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


    private suspend fun addModeToProfileObj(mUtilsRefreshRateSt: UtilsRefreshRateSt, key: String?): Boolean = withContext(Dispatchers.IO) {
        try {

            //Workaround for inconsistent read
            var modesSet: Map<String, List<Float>>? = null
            var pass = false
            while (!pass){
                modesSet = mUtilsRefreshRateSt.mUtilsDeviceInfo.getDisplayModesSet()
                while (modesSet != mUtilsRefreshRateSt.mUtilsDeviceInfo.getDisplayModesSet()) {
                    delay(1000)
                    modesSet = mUtilsRefreshRateSt.mUtilsDeviceInfo.getDisplayModesSet()
                }
                delay(1000)
                pass = modesSet == mUtilsRefreshRateSt.mUtilsDeviceInfo.getDisplayModesSet()
            }

            val resMapList = mutableListOf<Map<String, ResolutionDetails>>()

            for (mode in modesSet!!) {
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
            refreshRateModeMap[key ?: getKey(mUtilsRefreshRateSt.mContentResolver)] = resMapList

            return@withContext true
        } catch (_: Exception) {
            return@withContext false
        }
    }
}