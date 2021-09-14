package com.tribalfs.gmh.profiles

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Display
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.tribalfs.gmh.helpers.CacheSettings
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.highestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.isMultiResolution
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.minHzListForAdp
import com.tribalfs.gmh.helpers.CacheSettings.modesWithLowestHz
import com.tribalfs.gmh.helpers.CacheSettings.preventHigh
import com.tribalfs.gmh.helpers.CacheSettings.screenOffRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntAllMod
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntCurMod
import com.tribalfs.gmh.helpers.SingletonHolder
import com.tribalfs.gmh.helpers.UtilsDeviceInfo
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_ALWAYS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.STANDARD_REFRESH_RATE_HZ
import com.tribalfs.gmh.helpers.UtilsRefreshRate
import com.tribalfs.gmh.helpers.UtilsResoName.getName
import com.tribalfs.gmh.profiles.ModelNumbers.S20FE
import com.tribalfs.gmh.profiles.ModelNumbers.S20FE5G
import com.tribalfs.gmh.profiles.ModelNumbers.S21_U
import com.tribalfs.gmh.profiles.ModelNumbers.TS7L
import com.tribalfs.gmh.profiles.ModelNumbers.TS7LW
import com.tribalfs.gmh.profiles.ModelNumbers.TS7P
import com.tribalfs.gmh.profiles.ModelNumbers.TS7W
import com.tribalfs.gmh.profiles.ModelNumbers.ZF3
import com.tribalfs.gmh.profiles.ModelNumbers.ZFp3
import com.tribalfs.gmh.profiles.ModelNumbers.adaptiveModelsLocal
import com.tribalfs.gmh.profiles.ProfilesObj.adaptiveModelsObj
import com.tribalfs.gmh.profiles.ProfilesObj.loadComplete
import com.tribalfs.gmh.profiles.ProfilesObj.refreshRateModeMap
import com.tribalfs.gmh.profiles.Syncer.Companion.KEY_JSON_ADAPTIVES
import com.tribalfs.gmh.profiles.Syncer.Companion.KEY_JSON_REFRESH_RATES_PROFILE
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject


@RequiresApi(Build.VERSION_CODES.M)
internal class ProfilesInitializer private constructor(context: Context) {

    companion object : SingletonHolder<ProfilesInitializer, Context>(::ProfilesInitializer) {
        private const val TAG = "ProfilesHelperSt"
        val refreshRateModes = listOf(
            REFRESH_RATE_MODE_STANDARD,
            REFRESH_RATE_MODE_ALWAYS,
            REFRESH_RATE_MODE_SEAMLESS
        )
    }


    private val appCtx = context.applicationContext
    private val mUtilsDeviceInfo by lazy { UtilsDeviceInfo(appCtx) }
    private val mUtilsPrefsGmh by lazy { UtilsPrefsGmh(appCtx) }
    private val mSyncer by lazy { Syncer(appCtx) }
    private val _listedHighestHz: Float?
        get() {
            return when (mUtilsDeviceInfo.deviceModel) {
                S20FE5G, S20FE, S21_U, TS7P, TS7W, TS7L, TS7LW, ZF3, ZFp3 -> 120f
                else -> null
            }
        }
    private val mUtilsRefreshRate by lazy{ UtilsRefreshRate(appCtx) }

    @ExperimentalCoroutinesApi
    @Synchronized
    suspend fun initProfiles(): Boolean {
        //Source 1: backend
        val isLoadedFromBackEnd = withContext(Dispatchers.IO) { loadProfilesFromBackEnd() }
        if (isLoadedFromBackEnd) {
            return true
        } else {
            //Restore the previously fetched profiled rom backend, if any
            if (mUtilsPrefsGmh.prefProfileFetched && loadProfilesFromPref()) {
                updateCacheSettings()
                loadComplete = true
                return true
            }
        }

        //Source 2: predefined
        val isLoadedFromPredefined = withContext(Dispatchers.IO) { loadFromPredefinedProfiles() }
        if (isLoadedFromPredefined) {
            return true
        }

        //Source 3: scan from device
        return withContext(Dispatchers.IO) { readAndLoadProfileFromPhone() }
    }

    private fun updateCacheSettings(){
        synchronized(this){
            supportedHzIntAllMod = getSupportedHzIntAllModUpd()
            highestHzForAllMode =supportedHzIntAllMod?.maxOrNull()?: STANDARD_REFRESH_RATE_HZ//?:getHighestHzForAllModeUpd().toInt()
            lowestHzForAllMode = supportedHzIntAllMod?.minOrNull()?: STANDARD_REFRESH_RATE_HZ//?:getLowestHzForAllModeUpd().toInt()
            modesWithLowestHz = getModesWithHz(lowestHzForAllMode)
            isOfficialAdaptive = isAdaptiveSupportedUpd()
            isMultiResolution = isMultiResolutionUpd()
            minHzListForAdp = getMinHzListForAdpUpd()
            updateModeBasedVariables()
        }
    }


    fun updateModeBasedVariables(){
        synchronized(this) {
            supportedHzIntCurMod = getSupportedHzIntCurModUpd()
            mUtilsDeviceInfo.getSamRefreshRateMode().let {
                if (!isOfficialAdaptive && preventHigh && it == REFRESH_RATE_MODE_ALWAYS){
                    mUtilsRefreshRate.setRefreshRateMode(REFRESH_RATE_MODE_SEAMLESS)
                    return@synchronized
                }
                lowestHzCurMode = getForceLowestHzUpd(it)
                currentRefreshRateMode.set(it) //should be after updating the variables above
                screenOffRefreshRateMode =
                    if (mUtilsPrefsGmh.gmhPrefForceLowestSoIsOn
                        && modesWithLowestHz?.size?:0 > 0
                        && !modesWithLowestHz!!.contains(it)
                    ) {
                        modesWithLowestHz!![0]
                    } else {
                        it
                    }
            }
        }
    }



    @ExperimentalCoroutinesApi
    private suspend fun loadProfilesFromBackEnd(): Boolean{

        if (mUtilsPrefsGmh.gmhRefetchProfile) {//Forced or Scheduled Refetch
            mSyncer.fetchProfileFromBackEnd()?.let { jo ->
                try {
                    jo.optJSONArray(KEY_JSON_ADAPTIVES)?.let{
                        updateOfficialAdaptives(it)
                    }
                } catch (_: Exception) { }

                return try {
                    mUtilsPrefsGmh.gmhPrefDisplayModesObjectInJson = jo[KEY_JSON_REFRESH_RATES_PROFILE] as JSONObject
                    mUtilsPrefsGmh.gmhRefetchProfile = false
                    mUtilsPrefsGmh.prefProfileFetched = true
                    if (loadProfilesFromPref()) {
                        updateCacheSettings()
                        loadComplete = true
                        true
                    }else{
                        false
                    }
                } catch (_: Exception) {
                    false
                }
            }
            return false
        } else {
            return false
        }

    }


    private fun loadFromPredefinedProfiles(): Boolean{
        // Log.d(TAG, "isLocalProfileSaved: called")
        val isLocalProfileSaved = (PredefinedProfiles.get(mUtilsDeviceInfo.deviceModel))

        return if (isLocalProfileSaved != null) {
            mUtilsPrefsGmh.gmhPrefDisplayModesObjectInJson = isLocalProfileSaved
            if (loadProfilesFromPref()) {
                updateCacheSettings()
                loadComplete = true
                true
            }else{
                false
            }
        } else {
            false
        }
    }



    @ExperimentalCoroutinesApi
    @Synchronized
    private suspend fun readAndLoadProfileFromPhone(): Boolean {

        val internalProfilesJson = InternalProfiles.load( false, appCtx)

        if (internalProfilesJson.length() > 0){
            mUtilsPrefsGmh.gmhPrefDisplayModesObjectInJson = internalProfilesJson
            loadComplete = true
            refreshRateModes.forEach { rrm ->
                if (!refreshRateModeMap.keys.contains("${displayId}-${rrm}")) {
                    loadComplete = false
                }
            }

            updateCacheSettings()


            if (loadComplete) {
                CoroutineScope(Dispatchers.IO).launch {
                    //  Log.d(TAG, "Update config complete")
                    if (!mUtilsPrefsGmh.gmhPrefIsHzSynced && displayId == Display.DEFAULT_DISPLAY) {
                        // Log.d(TAG, "ProfilesSync(appCtx).postProfiles() called")
                        mSyncer.postProfileToBackEnd()?.let { jo ->
                            try {
                                jo.optJSONArray(KEY_JSON_ADAPTIVES)?.let { adt ->
                                    updateOfficialAdaptives(adt)
                                    mUtilsPrefsGmh.gmhPrefIsHzSynced = true
                                }
                            } catch (_: Exception) {
                            }
                        }// syncConfig()
                    }
                }
            }
            return true
        }else{
            return false
        }
    }


    private fun updateOfficialAdaptives(releaseArr: JSONArray){
        val adaptiveModels = mutableListOf<String>()
        for (i in 0 until releaseArr.length()) {
            adaptiveModels.add(
                releaseArr.getString(i).trim { it <= ' ' })
        }
        adaptiveModelsObj.clear()
        adaptiveModelsObj.addAll(adaptiveModels)
        mUtilsPrefsGmh.gmhPrefGetAdaptives = adaptiveModels
    }



    @Keep
    @Synchronized
    private fun loadProfilesFromPref(): Boolean {
        val gson = Gson()
        val jsonStr: String? =  mUtilsPrefsGmh.gmhPrefDisplayModesObjectInJson?.toString()
        val mainJson = jsonStr?.let { JSONObject(it) }
        if (mainJson != null) {
            //  Log.d(TAG, "Load saved saved refresh rate profiles")
            mainJson.keys().forEach { key ->
                val resArrayJson = mainJson[key] as JSONObject
                val resMapList = mutableListOf<Map<String, ResolutionDetails>>()
                resArrayJson.keys().forEach { resStr ->
                    val resDetailsJsonStr = JSONObject(resArrayJson[resStr].toString())
                    val resDetails = gson.fromJson(
                        resDetailsJsonStr.toString(),
                        ResolutionDetails::class.java
                    )
                    val resMap = mutableMapOf<String, ResolutionDetails>()
                    resMap[resStr] = resDetails
                    resMapList.add(resMap)
                }
                refreshRateModeMap[key] = resMapList
            }
            return true
        }else{
            //  Log.d(TAG, "No saved refresh rate profiles found.")
            return false
        }
    }



    private fun getModesWithHz(searchHz: Int): List<String>?{
        val arrStr: MutableList<String> = arrayListOf()
        refreshRateModes.forEach {mode ->
            refreshRateModeMap["$displayId-$mode"]?.forEach{ resoDetails ->
                resoDetails.keys.forEach {resoKey ->
                    for (hz in resoDetails[resoKey]!!.refreshRates) {
                        if (searchHz == hz.toInt()) {
                            arrStr.add(mode)
                        }
                    }
                }
            }
        }

        return if (arrStr.size >0){
            arrStr
        }else{
            null
        }
    }


    private fun isAdaptiveSupportedUpd(): Boolean {
        // if (BuildConfig.DEBUG) return false
        return mUtilsDeviceInfo.deviceModel.let { model ->
            adaptiveModelsObj.run{
                if (isNotEmpty()){
                    indexOf(model) != -1
                }else{//if empty
                    mUtilsPrefsGmh.gmhPrefGetAdaptives?.let{
                        //add if saved adaptive exists
                        addAll(it)
                    }
                    if (!isNotEmpty()){
                        addAll(adaptiveModelsLocal)
                    }
                    indexOf(model) != -1
                }
            }
        }
    }


    @Synchronized
    private fun getKey(): String{
        return "$displayId-${Settings.Secure.getString(appCtx.contentResolver, REFRESH_RATE_MODE)?:0}"
    }


    fun getCurrentResWithName(): String{
        val res = mUtilsDeviceInfo.getDisplayResStr("x").split("x")//getDisplayResolution()
        return "${getName(res[0].toInt(), res[1].toInt())}(${res[0]}x${res[1]})"
    }


    fun getCurrentResLxw(): String{
        return mUtilsDeviceInfo.getDisplayResStr("x")
    }

    private fun getCurrentResLxwFromMode(): String{
        return mUtilsDeviceInfo.getDisplayResFromModeStr("x")
    }


    private fun getSupportedHzIntAllModUpd(): List<Int>{
        val arrStr: MutableList<Int> = arrayListOf()
        refreshRateModes.forEach {mode ->
            refreshRateModeMap["$displayId-$mode"]?.forEach{ resoDetails ->
                resoDetails.keys.forEach {resoKey ->
                    for (i in resoDetails[resoKey]!!.refreshRates) {
                        arrStr.add(i.toInt())
                    }

                }
            }
        }

        if (arrStr.size == 0) {
            val curResStr =  getCurrentResLxw()
            val curResStrMode = getCurrentResLxwFromMode()
            val fromDev =  mUtilsDeviceInfo.getDisplayModesSet()[curResStr]?:mUtilsDeviceInfo.getDisplayModesSet()[curResStrMode]
            if (fromDev != null) {
                for (i in fromDev){
                    arrStr.add(i.toInt())
                }
            }
        }
        return arrStr.distinct().sorted()
    }

    private fun getMinHzListForAdpUpd(): List<Int> {
        val curResStr =  getCurrentResLxw()
        val curResStrMode = getCurrentResLxwFromMode()
        val arrStr: MutableList<Int> = arrayListOf()
        refreshRateModeMap["$displayId-$REFRESH_RATE_MODE_SEAMLESS"]?.forEach{
            if (it.containsKey(curResStr)) {
                it[curResStr]?.let { it1 ->
                    for (i in it1.refreshRates){
                        if (i <= STANDARD_REFRESH_RATE_HZ) {
                            arrStr.add(i.toInt())
                        }
                    }
                }
            }
            if (it.containsKey(curResStrMode)) {
                it[curResStrMode]?.let { it1 ->
                    for (i in it1.refreshRates){
                        if (i <= STANDARD_REFRESH_RATE_HZ) {
                            arrStr.add(i.toInt())
                        }
                    }
                }
            }
        }

        if (arrStr.size == 0) {
            val fromDev =  mUtilsDeviceInfo.getDisplayModesSet()[curResStr]?:mUtilsDeviceInfo.getDisplayModesSet()[curResStrMode]
            if (fromDev != null) {
                for (i in fromDev){
                    if (i <= STANDARD_REFRESH_RATE_HZ) {
                        arrStr.add(i.toInt())
                    }
                }
            }
        }
        return arrStr.distinct().sorted()
    }


    private fun getSupportedHzIntCurModUpd(): List<Int>{
        val curResStr =  getCurrentResLxw()
        val curResStrMode = getCurrentResLxwFromMode()
        val arrStr: MutableList<Int> = arrayListOf()
        refreshRateModeMap[getKey()]?.forEach{
            if (it.containsKey(curResStr)) {
                it[curResStr]?.let { it1 ->
                    for (i in it1.refreshRates){
                        arrStr.add(i.toInt())
                    }
                }
            }
            if (it.containsKey(curResStrMode)) {
                it[curResStrMode]?.let { it1 ->
                    for (i in it1.refreshRates){
                        arrStr.add(i.toInt())
                    }
                }
            }
        }
        if (arrStr.size == 0) {
            val fromDev =  mUtilsDeviceInfo.getDisplayModesSet()[curResStr]?:mUtilsDeviceInfo.getDisplayModesSet()[curResStrMode]
            if (fromDev != null) {
                for (i in fromDev){
                    arrStr.add(i.toInt())
                }
            }
        }
        return arrStr.distinct().sorted()
    }


    private fun isMultiResolutionUpd(): Boolean {
        refreshRateModes.forEach {
            try {
                if (getResolutionsForKey("$displayId-$it")?.size!! > 1) {
                    return true
                }
            }catch (_: Exception){ }
        }
        return false
    }


/*    private fun getLowestHzForAllModeUpd(): Float{
        var lowestResult =  60f
        refreshRateModes.forEach { rrm ->
            val key = "$displayId-$rrm"
            refreshRateModeMap[key]?.forEach {
                it.keys.forEach {key ->
                    it[key]?.lowestHz?.let {lh ->
                        if (lh < lowestResult) {
                            lowestResult = lh
                        }
                    }
                }
            }
        }

        return lowestResult
    }*/


/*    private val isPerModeDevice: Boolean
        get() {
            return (listOf(
                S20,
                S205G,
                S20P_S,
                S20P_E,
                S20U*//*,"SM-TEST"*//*
            ).indexOf(mUtilsDeviceInfo.deviceModel) != -1)
                    && (mUtilsDeviceInfo.oneUiVersion != null && mUtilsDeviceInfo.oneUiVersion!! >= 3.1)
        }*/


    private fun getForceLowestHzUpd(rrm: String): Int {
        var lowestResult = 60
        //if (isPerModeDevice){
        val key = "$displayId-$rrm"
        refreshRateModeMap[key]?.forEach {
            it.keys.forEach { key ->
                it[key]?.lowestHz?.let { lh ->
                    if (lh < lowestResult) {
                        lowestResult = lh.toInt()
                    }
                }
            }
        }
        /*}else {
            lowestResult = lowestHzForAllMode
        }*/
        return lowestResult
    }

/*    private fun getHighestHzForAllModeUpd(): Float{
        var highestResult =  _listedHighestHz?:60f
        refreshRateModes.forEach { rrm ->
            val key = "$displayId-$rrm"
            refreshRateModeMap[key]?.forEach {
                it.keys.forEach { key ->
                    it[key]?.highestHz?.let { hh ->
                        if (hh > highestResult) {
                            highestResult = hh
                        }
                    }
                }
            }
        }
        return highestResult
    }*/


    @ExperimentalCoroutinesApi
    fun getResoHighestHzForAllMode(resStrLxw: String?): Float{
        var highestResult = _listedHighestHz
        if (highestResult != null) return highestResult

        runBlocking {
            if (!loadComplete) {
                Log.d(TAG, "updateConfig() called from getResoHighestHzForAllMode")
                readAndLoadProfileFromPhone()
            }
        }

        val curResStr = resStrLxw ?: getCurrentResLxw()

        // if (mUtilsDeviceInfo.deviceIsSamsung) {
        refreshRateModes.forEach { rrm ->
            val key = "$displayId-$rrm"
            refreshRateModeMap[key]?.forEach {
                if (it[curResStr] != null && it[curResStr]?.highestHz!! > highestResult ?: 0f) {
                    highestResult = it[curResStr]?.highestHz!!
                }
            }
            if (highestResult == null){//maybe using custom resolution
                val curResStrFromMod = getCurrentResLxwFromMode()
                refreshRateModeMap[key]?.forEach {
                    if (it[curResStrFromMod] != null && it[curResStrFromMod]?.highestHz!! > highestResult ?: 0f) {
                        highestResult = it[curResStrFromMod]?.highestHz!!
                    }
                }
            }
        }
        return highestResult!!
    }

    @Synchronized
    fun getResoHighestHzForCurrentMode(resStrLxw: String?, rrm: String?): Float {
        val curResStrLxw = resStrLxw ?: getCurrentResLxw()
        val key = if (rrm != null) "$displayId-$rrm" else getKey()
        if (refreshRateModeMap[key] != null) {
            refreshRateModeMap[key]?.forEach {
                if (it.containsKey(curResStrLxw)) return it[curResStrLxw]?.highestHz!!
            }
        }
        //fallback if config is not yet loaded, will also checked for custom reso
        return mUtilsDeviceInfo.getMaxHzForCurrentReso(curResStrLxw)
    }


    fun getResolutionsForKey(key: String?): List<Map<String, ResolutionDetails>>? {
        return refreshRateModeMap[key ?: getKey()]
    }


    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun getDisplayModesStrGmh(): String  {
        var modes = ""
        val curResStrLxw = getCurrentResLxw()
        if (!loadComplete) {
            InternalProfiles.load(true, appCtx)
        }
        refreshRateModeMap[getKey()]?.forEach{
            modes += if (it.containsKey(curResStrLxw)) {
                ("[ ${it[curResStrLxw]!!.resName}(${it[curResStrLxw]!!.resStrLxw}) " +
                        "@ ${it[curResStrLxw]!!.refreshRates.joinToString("/")} hz ]\n")
            } else {
                val key = it.keys.first()
                ("${it[key]!!.resName}(${it[key]!!.resStrLxw}) " +
                        "@ ${it[key]!!.refreshRates.joinToString("/")} hz\n")
            }
        }

        return modes.trim()
    }

}