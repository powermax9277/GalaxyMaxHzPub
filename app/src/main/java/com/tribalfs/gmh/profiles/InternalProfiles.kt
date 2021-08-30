package com.tribalfs.gmh.profiles

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isSamsung
import com.tribalfs.gmh.helpers.UtilsDeviceInfo
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.helpers.UtilsResoName.getName
import com.tribalfs.gmh.profiles.ProfilesInitializer.Companion.refreshRateModes
import com.tribalfs.gmh.profiles.ProfilesObj.loadComplete
import com.tribalfs.gmh.profiles.ProfilesObj.refreshRateModeMap
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.round

object InternalProfiles {

    @Synchronized
    private fun getKey(context: Context): String{
        //return if (mUtilsDeviceInfo.deviceIsSamsung) {
        return "$displayId-${Settings.Secure.getString(context.applicationContext.contentResolver, REFRESH_RATE_MODE)}"
    }


    private fun isModeProfilesAdded(key: String?, context: Context): Boolean{
        val mKey = key?: getKey(context)
        return refreshRateModeMap.containsKey(mKey)
    }

   @ExperimentalCoroutinesApi
   @SuppressLint("NewApi")
    @Synchronized
   suspend fun load(currentModeOnly: Boolean, context: Context): JSONObject = withContext(Dispatchers.IO) {

            val mUtilsDeviceInfo = UtilsDeviceInfo(context)

            if (!currentModeOnly && hasWriteSecureSetPerm
                //Ensure that device is not resolution with no high refresh rate support
                && (!isSamsung || mUtilsDeviceInfo.getSamRefreshRateMode() != REFRESH_RATE_MODE_STANDARD)
            ) {
                val originalRefreshRateMode = mUtilsDeviceInfo.getSamRefreshRateMode()
                var endingRefreshRateMode = originalRefreshRateMode

                loadComplete = withContext(Dispatchers.IO) {
                    var modeAddedCnt = 0
                    try {
                        refreshRateModes.forEach {
                            val key = "$displayId-$it"
                            if (!isModeProfilesAdded(key, context)) {
                                Settings.Secure.putString(context.applicationContext.contentResolver, REFRESH_RATE_MODE, it )
                                delay(500)
                                endingRefreshRateMode = it
                                if (addModeProfiles(key, context)) {
                                    modeAddedCnt += 0
                                }
                                delay(100)
                            }
                        }
                    }catch (_:Exception){ }
                    modeAddedCnt == refreshRateModes.size
                }

                //restore user refresh rate mode
                if (originalRefreshRateMode != endingRefreshRateMode) {
                    Settings.Secure.putString(context.applicationContext.contentResolver, REFRESH_RATE_MODE, originalRefreshRateMode )
                }

            } else {
                //add current DisplayMode only in case syncs results below is empty or failed
                if (!isModeProfilesAdded(null,context)) {
                    val key = getKey(context)
                    addModeProfiles(key, context)
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



    @Synchronized
    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun addModeProfiles(key: String?, context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentDisplay = (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(displayId)
            val modesSet =  currentDisplay.supportedModes.asSequence().distinct()
                .groupBy(
                    { it.physicalHeight.toString() + "x" + it.physicalWidth.toString() },
                    { round(it.refreshRate*100) /100 }
                )
                .mapValues { (_, values) -> values }

            val resMapList = mutableListOf<Map<String, ResolutionDetails>>()

            for (mode in modesSet) {
                val resMap = mutableMapOf<String, ResolutionDetails>()
                val resSplit = mode.key.split("x")
                resMap[mode.key] = ResolutionDetails(
                    resSplit[0].toInt(),//height
                    resSplit[1].toInt(),//width
                    mode.key,//resLxw
                    getName(resSplit[0].toInt(), resSplit[1].toInt()),
                    mode.value,//[refresh rates]
                    mode.value.minOrNull()!!,//lowest refresh rate
                    mode.value.maxOrNull()!!,//highest refresh rate
                )
                resMapList.add(resMap)
            }
            refreshRateModeMap[key ?: getKey(context)] = resMapList
            return@withContext true
        }catch (_: Exception){
            return@withContext false
        }
    }
}