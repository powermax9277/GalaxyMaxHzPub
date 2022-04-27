package com.tribalfs.gmh.helpers

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import android.view.Display
import android.widget.Toast
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.CacheSettings.canApplyFakeAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSystemSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.highestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isMultiResolution
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isOnePlus
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveMode
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.isXiaomi
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.lrrPref
import com.tribalfs.gmh.helpers.CacheSettings.minHzListForAdp
import com.tribalfs.gmh.helpers.CacheSettings.modesWithLowestHz
import com.tribalfs.gmh.helpers.CacheSettings.preventHigh
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.screenOffRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntAllMod
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntCurMod
import com.tribalfs.gmh.helpers.CacheSettings.typingRefreshRate
import com.tribalfs.gmh.profiles.*
import com.tribalfs.gmh.profiles.ModelNumbers.S20FE
import com.tribalfs.gmh.profiles.ModelNumbers.S20FE5G
import com.tribalfs.gmh.profiles.ModelNumbers.S21FE
import com.tribalfs.gmh.profiles.ModelNumbers.S21_U
import com.tribalfs.gmh.profiles.ModelNumbers.S22
import com.tribalfs.gmh.profiles.ModelNumbers.S22P
import com.tribalfs.gmh.profiles.ModelNumbers.S22U
import com.tribalfs.gmh.profiles.ModelNumbers.S22U_JP
import com.tribalfs.gmh.profiles.ModelNumbers.TS75G
import com.tribalfs.gmh.profiles.ModelNumbers.TS7L
import com.tribalfs.gmh.profiles.ModelNumbers.TS7LW
import com.tribalfs.gmh.profiles.ModelNumbers.TS7P
import com.tribalfs.gmh.profiles.ModelNumbers.TS7W
import com.tribalfs.gmh.profiles.ModelNumbers.TS8
import com.tribalfs.gmh.profiles.ModelNumbers.TS8P
import com.tribalfs.gmh.profiles.ModelNumbers.TS8U
import com.tribalfs.gmh.profiles.ModelNumbers.ZF3
import com.tribalfs.gmh.profiles.ModelNumbers.ZFp3
import com.tribalfs.gmh.profiles.ModelNumbers.adaptiveModelsLocal
import com.tribalfs.gmh.profiles.ModelNumbers.fordableWithHrrExternal
import com.tribalfs.gmh.profiles.ProfilesObj.refreshRateModeMap
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import com.tribalfs.gmh.tiles.QSTileMaxHz
import com.tribalfs.gmh.tiles.QSTileMinHz
import com.tribalfs.gmh.tiles.QSTileResSw
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

internal val refreshRateModes = listOf(REFRESH_RATE_MODE_ALWAYS, REFRESH_RATE_MODE_SEAMLESS, REFRESH_RATE_MODE_STANDARD)

class UtilRefreshRateSt private constructor (val appCtx: Context) {

    companion object : SingletonMaker<UtilRefreshRateSt, Context>(::UtilRefreshRateSt)

    internal val mSyncer by lazy { Syncer(appCtx) }

    private val _listedHighestHz: Float?
        get() {
            return when (UtilsDeviceInfoSt.instance(appCtx).deviceModel) {
                S20FE5G, S20FE, S21_U, TS7P, TS7W, TS7L, TS75G, TS7LW, ZF3, ZFp3, S22, S22P, S22U, S22U_JP, S21FE, TS8, TS8P, TS8U -> 120f
                else -> null
            }
        }


    @ExperimentalCoroutinesApi
    suspend fun initProfiles(): Boolean = withContext(Dispatchers.IO) {
        //Source 1: backend )
        if (loadProfilesFromBackEnd() ) {
            return@withContext true
        } else {
            //Restore the previously fetched profiled rom backend, if any
            if (UtilsPrefsGmhSt.instance(appCtx).prefProfileFetched && loadProfilesFromPref()) {
                updateCacheSettings()
                ProfilesObj.loadComplete = true
                return@withContext true
            }
        }

        //Source 2: predefined
        if ( loadFromPredefinedProfiles()) {
            return@withContext true
        }

        //Source 3: scan from device
        return@withContext readAndLoadProfileFromPhone()

    }


    @ExperimentalCoroutinesApi
    //@Synchronized
    private fun updateCacheSettings() {
        synchronized(mLock) {
            supportedHzIntAllMod = getSupportedHzIntAllModUpd()
            highestHzForAllMode =
                supportedHzIntAllMod?.maxOrNull() ?: UtilsDeviceInfoSt.instance(appCtx).regularMinHz
            lowestHzForAllMode =
                supportedHzIntAllMod?.minOrNull() ?:UtilsDeviceInfoSt.instance(appCtx).regularMinHz
            modesWithLowestHz = getModesWithHz(lowestHzForAllMode)
            isOfficialAdaptive = isAdaptiveSupportedUpd()
            isMultiResolution = isMultiResolutionUpd()
            minHzListForAdp = getMinHzListForAdpUpd()
            if (minHzListForAdp?.indexOf(48) != -1) {
                 typingRefreshRate = 48
            }
        }
        updateModeBasedVariables()
    }

    @ExperimentalCoroutinesApi
    fun updateModeBasedVariables() {
        synchronized(mLock) {
            supportedHzIntCurMod = getSupportedHzIntCurModUpd()
            getRefreshRateMode().let {
                if (!isOfficialAdaptive && preventHigh && it == REFRESH_RATE_MODE_ALWAYS) {
                    setRefreshRateMode(REFRESH_RATE_MODE_SEAMLESS)
                    return
                }
                lowestHzCurMode = getForceLowestHzUpd(it)
                currentRefreshRateMode.set(it) //should be after updating the variables above
                screenOffRefreshRateMode =
                    if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefForceLowestSoIsOn
                        && modesWithLowestHz?.size ?: 0 > 0
                        && !modesWithLowestHz!!.contains(it)
                    ) {
                        modesWithLowestHz!![0]
                    } else {
                        it
                    }

                requestListeningAllTiles()
            }
        }
    }

    @ExperimentalCoroutinesApi
    internal fun requestListeningAllTiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(
                appCtx,
                ComponentName(appCtx, QSTileMinHz::class.java)
            )
            TileService.requestListeningState(
                appCtx,
                ComponentName(appCtx, QSTileMaxHz::class.java)
            )
            TileService.requestListeningState(
                appCtx,
                ComponentName(appCtx, QSTileResSw::class.java)
            )
        }
    }

    @ExperimentalCoroutinesApi
    private fun requestListeningHzTiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(
                appCtx,
                ComponentName(appCtx, QSTileMinHz::class.java)
            )
            TileService.requestListeningState(
                appCtx,
                ComponentName(appCtx, QSTileMaxHz::class.java)
            )
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun loadProfilesFromBackEnd(): Boolean = withContext(Dispatchers.IO) {

        if (UtilsPrefsGmhSt.instance(appCtx).gmhRefetchProfile) {//Forced or Scheduled Refetch
            mSyncer.fetchProfileFromBackEnd()?.let { jo ->
                try {
                    jo.optJSONArray(KEY_JSON_ADAPTIVES)?.let {
                        updateOfficialAdaptives(it)
                    }
                } catch (_: Exception) {
                }

                return@withContext try {
                    UtilsPrefsGmhSt.instance(appCtx).gmhPrefDisplayModesObjectInJson =
                        jo[KEY_JSON_REFRESH_RATES_PROFILE] as JSONObject
                    UtilsPrefsGmhSt.instance(appCtx).gmhRefetchProfile = false
                    UtilsPrefsGmhSt.instance(appCtx).prefProfileFetched = true
                    if (loadProfilesFromPref()) {
                        updateCacheSettings()
                        ProfilesObj.loadComplete = true
                        true
                    } else {
                        false
                    }
                } catch (_: Exception) {
                    false
                }
            }
            return@withContext false
        } else {
            return@withContext false
        }

    }


    @ExperimentalCoroutinesApi
    private suspend fun loadFromPredefinedProfiles(): Boolean = withContext(Dispatchers.IO){
        // Log.d(TAG, "isLocalProfileSaved: called")
        val isLocalProfileSaved =
            (PredefinedProfiles.get(appCtx, UtilsDeviceInfoSt.instance(appCtx).deviceModel))

         if (isLocalProfileSaved != null) {
            UtilsPrefsGmhSt.instance(appCtx).gmhPrefDisplayModesObjectInJson = isLocalProfileSaved
            if (loadProfilesFromPref()) {
                updateCacheSettings()
                ProfilesObj.loadComplete = true
                return@withContext true
            } else {
                return@withContext false
            }
        } else {
             return@withContext false
        }
       /* return if (isLocalProfileSaved != null) {
            UtilsPrefsGmhSt.instance(appCtx).gmhPrefDisplayModesObjectInJson = isLocalProfileSaved
            if (loadProfilesFromPref()) {
                updateCacheSettings()
                ProfilesObj.loadComplete = true
                true
            } else {
                false
            }
        } else {
            false
        }*/
    }


    @ExperimentalCoroutinesApi
    private suspend fun readAndLoadProfileFromPhone(): Boolean = withContext(Dispatchers.IO) {

       val isStandardMode =
            (UtilsDeviceInfoSt.instance(appCtx).manufacturer == "SAMSUNG") && getRefreshRateMode() == REFRESH_RATE_MODE_STANDARD

        val internalProfilesJson = InternalProfiles.loadToProfilesObj(
            currentModeOnly = false,
            overwriteExisting = true,
            appCtx
        )

        if (internalProfilesJson.length() > 0) {
            UtilsPrefsGmhSt.instance(appCtx).gmhPrefDisplayModesObjectInJson = internalProfilesJson
            var isComplete = !isStandardMode
            if (isComplete) {
                refreshRateModes.forEach { rrm ->
                    if (!refreshRateModeMap.keys.contains("$displayId-${rrm}")) {
                        isComplete = false
                    }
                }
            }
            ProfilesObj.loadComplete = isComplete

            updateCacheSettings()


            if (!UtilsPrefsGmhSt.instance(appCtx).gmhPrefIsHzSynced && displayId == Display.DEFAULT_DISPLAY) {
                CoroutineScope(Dispatchers.IO).launch {
                    mSyncer.postProfileToBackEnd()?.let { jo ->
                        try {
                            jo.optJSONArray(KEY_JSON_ADAPTIVES)?.let { adt ->
                                updateOfficialAdaptives(adt)
                                UtilsPrefsGmhSt.instance(appCtx).gmhPrefIsHzSynced = true
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            return@withContext true
        } else {
            return@withContext false
        }
    }


    private fun updateOfficialAdaptives(releaseArr: JSONArray) {
        val adaptiveModels = mutableListOf<String>()
        for (i in 0 until releaseArr.length()) {
            adaptiveModels.add(
                releaseArr.getString(i).trim { it <= ' ' })
        }
        ProfilesObj.adaptiveModelsObj.clear()
        ProfilesObj.adaptiveModelsObj.addAll(adaptiveModels)
        UtilsPrefsGmhSt.instance(appCtx).gmhPrefGetAdaptives = adaptiveModels
    }


    @Keep
    private suspend fun loadProfilesFromPref(): Boolean = withContext(Dispatchers.IO) {
        val gson = Gson()
        val jsonStr: String? =
            UtilsPrefsGmhSt.instance(appCtx).gmhPrefDisplayModesObjectInJson?.toString()
        val mainJson = jsonStr?.let { JSONObject(it) }
        if (mainJson != null) {
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
           // return true
            return@withContext true
        } else {
            //  Log.d(TAG, "No saved refresh rate profiles found.")
            //return false
            return@withContext false
        }
    }


    private fun getModesWithHz(searchHz: Int): List<String>? {
        val arrStr: MutableList<String> = arrayListOf()
        refreshRateModes.forEach { mode ->
            refreshRateModeMap["$displayId-$mode"]?.forEach { resoDetails ->
                resoDetails.keys.forEach { resoKey ->
                    for (hz in resoDetails[resoKey]!!.refreshRates) {
                        if (searchHz == hz.toInt()) {
                            arrStr.add(mode)
                        }
                    }
                }
            }
        }

        return if (arrStr.size > 0) {
            arrStr
        } else {
            null
        }
    }



    fun getThisRrmAndResoHighestHz(resStrLxw: String?, rrm: String?): Float {
        synchronized(mLock) {
            val curResStrLxw =
                resStrLxw ?: UtilsDeviceInfoSt.instance(appCtx).getDisplayResoStr("x")
            val key = if (rrm != null) "${displayId}-$rrm" else getKey()
            if (refreshRateModeMap[key] != null) {
                refreshRateModeMap[key]?.forEach {
                    if (it.containsKey(curResStrLxw)) return it[curResStrLxw]?.highestHz!!
                }
            }
            //fallback if config is not yet loaded, will also checked for custom reso
            return UtilsDeviceInfoSt.instance(appCtx).getMaxHzForCurrentReso(curResStrLxw)
        }
    }


    fun getResolutionsForKey(key: String?): List<Map<String, ResolutionDetails>>? {
        return refreshRateModeMap[key ?: getKey()]
    }


    suspend fun getDisplayModesStrGmh(): String {

        if (!ProfilesObj.loadComplete) {
            InternalProfiles.loadToProfilesObj(
                true,
                overwriteExisting = false,
                appCtx
            )
        }

        var modes = ""
        val curResStrLxw = UtilsDeviceInfoSt.instance(appCtx).getDisplayResoStr("x")
        refreshRateModeMap[getKey()]?.forEach { map ->
            modes += if (map.containsKey(curResStrLxw)) {
                ("[ ${map[curResStrLxw]!!.resName}(${map[curResStrLxw]!!.resStrLxw}) " +
                        "@ ${map[curResStrLxw]!!.refreshRates.joinToString("/")} hz ]\n")
            } else {
                val key = map.keys.first()
                ("${map[key]!!.resName}(${map[key]!!.resStrLxw}) " +
                        "@ ${map[key]!!.refreshRates.joinToString("/")} hz\n")
            }
        }

        return modes.trim()

    }


    fun isAdaptiveSupportedUpd(): Boolean {
        return UtilsDeviceInfoSt.instance(appCtx).deviceModel.let { model ->
            ProfilesObj.adaptiveModelsObj.run {
                if (isNotEmpty()) {
                    indexOf(model) != -1
                } else {//if empty
                    UtilsPrefsGmhSt.instance(appCtx).gmhPrefGetAdaptives?.let {
                        //add if saved adaptive exists
                        addAll(it)
                    }
                    if (!isNotEmpty()) {
                        addAll(adaptiveModelsLocal)
                    }
                    indexOf(model) != -1
                }
            }
        }
    }


    private fun getKey(): String {
        return "$displayId-${
            Settings.Secure.getString(
                appCtx.contentResolver,
                REFRESH_RATE_MODE
            ) ?: 0
        }"
    }


    fun getCurrentResWithName(): String {
        val res = UtilsDeviceInfoSt.instance(appCtx).getDisplayResoStr("x")
            .split("x")//getDisplayResolution()
        return "${UtilsReso.getName(res[0].toInt(), res[1].toInt())}(${res[0]}x${res[1]})"
    }


    private fun getSupportedHzIntAllModUpd(): List<Int> {
        val arrayHz: MutableList<Int> = arrayListOf()
        refreshRateModes.forEach { mode ->
            refreshRateModeMap["$displayId-$mode"]?.forEach { resoDetails ->
                resoDetails.keys.forEach { resoKey ->
                    for (i in resoDetails[resoKey]!!.refreshRates) {
                        arrayHz.add(i.toInt())
                    }

                }
            }
        }

        if (arrayHz.size == 0) {
            getRefreshRatesFromActiveSettings()?.let {
                for (i in it) {
                    arrayHz.add(i.toInt())
                }
            }
        }

        return arrayHz.distinct().sorted()
    }


    private fun getMinHzListForAdpUpd(): List<Int> {
        val curResStr = UtilsDeviceInfoSt.instance(appCtx).getDisplayResoStr("x")
        val arrStr: MutableList<Int> = arrayListOf()
        refreshRateModeMap["$displayId-$REFRESH_RATE_MODE_SEAMLESS"]?.forEach {
            if (it.containsKey(curResStr)) {
                it[curResStr]?.let { it1 ->
                    for (i in it1.refreshRates) {
                        if (i < highestHzForAllMode) {
                            arrStr.add(i.toInt())
                        }
                    }
                }
            }
        }

        if (arrStr.size == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val curResStrMode = UtilsDeviceInfoSt.instance(appCtx).getDisplayResFromModeStr("x")
            refreshRateModeMap["$displayId-$REFRESH_RATE_MODE_SEAMLESS"]?.forEach {
                if (it.containsKey(curResStrMode)) {
                    it[curResStrMode]?.let { it1 ->
                        for (i in it1.refreshRates) {
                            if (i < highestHzForAllMode) {
                                arrStr.add(i.toInt())
                            }
                        }
                    }
                }
            }
        }

        if (arrStr.size == 0) {
            getRefreshRatesFromActiveSettings()?.let {
                if (it.size > 1) {
                    for (i in it) {
                        if (i < highestHzForAllMode) {
                            arrStr.add(i.toInt())
                        }
                    }
                }else{
                    arrStr.add(it[0].toInt())
                }
            }
        }

        return arrStr.distinct().sorted()
    }


    private fun getSupportedHzIntCurModUpd(): List<Int>{
        val curResStr = UtilsDeviceInfoSt.instance(appCtx).getDisplayResoStr("x")
        val arrayHz: MutableList<Int> = arrayListOf()
        refreshRateModeMap[getKey()]?.forEach {
            if (it.containsKey(curResStr)) {
                it[curResStr]?.let { it1 ->
                    for (i in it1.refreshRates) {
                        arrayHz.add(i.toInt())
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val curResStrMode = UtilsDeviceInfoSt.instance(appCtx).getDisplayResFromModeStr("x")
                if (it.containsKey(curResStrMode)) {
                    it[curResStrMode]?.let { it1 ->
                        for (i in it1.refreshRates) {
                            arrayHz.add(i.toInt())
                        }
                    }
                }
            }
        }


        if (arrayHz.size == 0) {
            getRefreshRatesFromActiveSettings()?.let {
                for (i in it) {
                    arrayHz.add(i.toInt())
                }
            }
        }

        return arrayHz.distinct().sorted()
    }


    private fun getRefreshRatesFromActiveSettings(): List<Float>?{

        var refreshRatesfromDevice =
            UtilsDeviceInfoSt.instance(appCtx).getDisplayModesSet()[UtilsDeviceInfoSt.instance(appCtx).getDisplayResoStr("x")]

        if (refreshRatesfromDevice == null) {
            refreshRatesfromDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                UtilsDeviceInfoSt.instance(appCtx).getDisplayModesSet()[UtilsDeviceInfoSt.instance(appCtx).getDisplayResFromModeStr(
                    "x"
                )]
            } else {
                @Suppress("DEPRECATION")
                UtilsDeviceInfoSt.instance(appCtx).getCurrentDisplay().supportedRefreshRates.toList()
            }
        }

        return refreshRatesfromDevice
    }


    private fun isMultiResolutionUpd(): Boolean {
        refreshRateModes.forEach {
            try {
                if (getResolutionsForKey("$displayId-$it")?.size!! > 1) {
                    return true
                }
            } catch (_: Exception) {
            }
        }
        return false
    }


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

        return lowestResult
    }



    @ExperimentalCoroutinesApi
    fun getResoHighestHzForAllMode(resStrLxw: String?): Float{

        var highestResult = _listedHighestHz
        if (highestResult != null) return highestResult

        runBlocking {
            if (!ProfilesObj.loadComplete) {
                //Log.d(TAG, "updateConfig() called from getResoHighestHzForAllMode")
                readAndLoadProfileFromPhone()
            }
        }

        val curResStr = resStrLxw ?: UtilsDeviceInfoSt.instance(appCtx).getDisplayResoStr("x")

        // if (UtilDeviceInfoSt.instance(appCtx).deviceIsSamsung) {
        refreshRateModes.forEach { rrm ->
            val key = "$displayId-$rrm"
            refreshRateModeMap[key]?.forEach {
                if (it[curResStr] != null && it[curResStr]?.highestHz!! > highestResult ?: 0f) {
                    highestResult = it[curResStr]?.highestHz!!
                }
            }

            if (highestResult == null) {//maybe using custom resolution
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val curResStrFromMod = UtilsDeviceInfoSt.instance(appCtx).getDisplayResFromModeStr("x")
                    refreshRateModeMap[key]?.forEach {
                        if (it[curResStrFromMod] != null && it[curResStrFromMod]?.highestHz!! > highestResult ?: 0f) {
                            highestResult = it[curResStrFromMod]?.highestHz!!
                        }
                    }
                }
            }
        }

        return highestResult!!
    }

    //Note: Don't add requestListening here - always called by makeAdaptive
    fun setPeakRefreshRate(refreshRate: Int){
        if (hasWriteSystemSetPerm){
            Settings.System.putString(appCtx.contentResolver, PEAK_REFRESH_RATE, refreshRate.toString())
            if (isXiaomi) {
                Settings.System.putString(
                    appCtx.contentResolver,
                    USER_REFRESH_RATE,
                    refreshRate.toString()
                )
            }
        }else{
           CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    appCtx,
                    "Error! ${appCtx.getString(R.string.enable_write_settings)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    internal fun setMinRefreshRate(refreshRate: Int){
        if (hasWriteSystemSetPerm){
            Settings.System.putString(appCtx.contentResolver, MIN_REFRESH_RATE, refreshRate.toString())
        }else{
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    appCtx,
                    "Error! ${appCtx.getString(R.string.enable_write_settings)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun deleteRefreshRate(name: String){
            appCtx.contentResolver.delete(
                Uri.parse("content://settings/system"), "name = ?", arrayOf(
                    name
                )
            )
    }


    internal fun clearPeakAndMinRefreshRate() {
        if (hasWriteSystemSetPerm) {
            deleteRefreshRate(PEAK_REFRESH_RATE)
            deleteRefreshRate(MIN_REFRESH_RATE)
            if (isXiaomi) {
                deleteRefreshRate(USER_REFRESH_RATE)
            }
        }
    }


    @ExperimentalCoroutinesApi
    internal fun setRefreshRate(refreshRate: Int, minHz: Int?): Boolean {
        if (refreshRate > 0) {
            setPeakRefreshRate(refreshRate)
            if(currentRefreshRateMode.get() == REFRESH_RATE_MODE_ALWAYS && isOfficialAdaptive) {
                setMinRefreshRate(refreshRate)
            } else {
                minHz?.let{setMinRefreshRate(it)}
            }
        }else{
            clearPeakAndMinRefreshRate()
        }
        requestListeningHzTiles()
        return true
    }


/*    internal fun getResoAndRefRateModeArr(currentRefreshRateMode: String?): ResoNameMode{
        val reso = UtilDeviceInfoSt.instance(appCtx).getDisplayResolution()
        val resoCat = UtilsResoName.getName(
            reso.height,
            reso.width
        )

        val mode = when (currentRefreshRateMode ?: samRefreshRateMode) {
            REFRESH_RATE_MODE_SEAMLESS -> appCtx.getString(R.string.adp_mode)
            REFRESH_RATE_MODE_STANDARD -> appCtx.getString(R.string.std_mode)
            REFRESH_RATE_MODE_ALWAYS -> appCtx.getString(R.string.high_mode)
            else -> "?"
        }
        return ResoNameMode(resoCat, mode)
    }*/

    private val mLock = Object()

    internal fun getRefreshRateMode(): String {
        synchronized(mLock) {
            if (!isOnePlus) {
                return (Settings.Secure.getString(appCtx.contentResolver, REFRESH_RATE_MODE)
                    ?: 0).toString()
            } else {
                return when ((Settings.Secure.getString(
                    appCtx.contentResolver,
                    ONEPLUS_SCREEN_REFRESH_RATE
                )
                    ?: 0).toString()) {
                    REFRESH_RATE_MODE_ALWAYS -> {
                        REFRESH_RATE_MODE_ALWAYS
                    }
                    ONEPLUS_RATE_MODE_SEAMLESS -> {
                        REFRESH_RATE_MODE_SEAMLESS
                    }
                    else -> {
                        REFRESH_RATE_MODE_STANDARD
                    }
                }
            }
        }
    }



    internal fun setRefreshRateMode(mode: String) : Boolean{
        synchronized(mLock) {
            return try {
                Settings.Secure.putInt(appCtx.contentResolver, REFRESH_RATE_MODE, mode.toInt())
                        && (
                        if (fordableWithHrrExternal.indexOf(UtilsDeviceInfoSt.instance(appCtx).deviceModel) != -1) {
                            Settings.Secure.putString(
                                appCtx.contentResolver,
                                REFRESH_RATE_MODE_COVER,
                                mode
                            )
                        } else {
                            true
                        })
                        && (
                        if (isOnePlus) {
                            when (mode) {
                                REFRESH_RATE_MODE_ALWAYS -> {
                                    Settings.Global.putString(
                                        appCtx.contentResolver,
                                        ONEPLUS_SCREEN_REFRESH_RATE,
                                        ONEPLUS_RATE_MODE_ALWAYS
                                    )
                                }
                                REFRESH_RATE_MODE_SEAMLESS -> {
                                    Settings.Global.putString(
                                        appCtx.contentResolver,
                                        ONEPLUS_SCREEN_REFRESH_RATE,
                                        ONEPLUS_RATE_MODE_SEAMLESS
                                    )
                                }
                                REFRESH_RATE_MODE_STANDARD -> {
                                    Settings.Global.putString(
                                        appCtx.contentResolver,
                                        ONEPLUS_SCREEN_REFRESH_RATE,
                                        ONEPLUS_RATE_MODE_STANDARD
                                    )
                                }
                            }
                            true
                        } else {
                            true
                        }
                        )
            } catch (_: Exception) {
                false
            }
        }
    }


    internal fun getPeakRefreshRate(): Int {
        var prr =  if (isFakeAdaptive.get()!!) {
            (if (keepModeOnPowerSaving && isPowerSaveMode.get() ==true)
                UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRatePsm
            else
                UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRate
                    ).let{
                    if (it != -1){
                        it
                    }else{
                        getThisRrmAndResoHighestHz(null, null).toInt().let{ highestHz ->
                            UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRate = highestHz
                            highestHz
                        }
                    }
                }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getPeakRefreshRateFromSettings()
            } else {
                null
            }
        }

        if (prr == null) {
            prr = getThisRrmAndResoHighestHz(null, null).toInt()
        }
        //Log.d(TAG,"getPeakRefreshRate $prr")
        return prr
    }


    internal fun getPeakRefreshRateFromSettings(): Int {
        val prr = Settings.System.getString(appCtx.contentResolver, PEAK_REFRESH_RATE)
        return prr?.toIntOrNull() ?: UtilsDeviceInfoSt.instance(appCtx)
                .getCurrentDisplay().refreshRate.toInt()
    }

    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun setPrefOrAdaptOrHighRefreshRateMode(resStrLxw: String?): Boolean{
        return setPrefOrAdaptOrHighRefreshRateMode(resStrLxw, false)
    }

    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun setPrefOrAdaptOrHighRefreshRateMode(resStrLxw: String?, autoApplyStandard: Boolean): Boolean{
        return try {
            val rrm =
                if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefRefreshRateModePref != null) {
                    UtilsPrefsGmhSt.instance(appCtx).gmhPrefRefreshRateModePref
                } else {
                    if (isOfficialAdaptive) {
                        REFRESH_RATE_MODE_SEAMLESS
                    } else {
                        REFRESH_RATE_MODE_ALWAYS
                    }
                }
            tryThisRrm(rrm!!, resStrLxw,autoApplyStandard)
        }catch (_:Exception){
            false
        }
    }


    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun tryThisRrm(rrm: String, resStrLxw: String?) : Boolean {
        return tryThisRrm(rrm, resStrLxw, false)
    }


    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun tryThisRrm(rrm: String, resStrLxw: String?, autoApplyStandard: Boolean) : Boolean {
        if (hasWriteSecureSetPerm) {
            return if (rrm != REFRESH_RATE_MODE_STANDARD) {
                val highest = getThisRrmAndResoHighestHz(resStrLxw, rrm)
                if (highest > SIXTY_HZ) {
                    setRefreshRateMode(rrm) && setRefreshRate(prrActive.get()!!, null)
                } else {
                    if (autoApplyStandard) {
                        setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
                    }
                    false
                }
            }else{
                setRefreshRateMode(rrm)
            }
        }
        return false
    }


    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    internal fun applyMinHz(){
        val regMinHz = UtilsDeviceInfoSt.instance(appCtx).regularMinHz
        if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt > regMinHz) {
            setMinRefreshRate(UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt)
        }else{
            setMinRefreshRate(0)
        }
        UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt.let{
            lrrPref.set(it)
            typingRefreshRate = it.coerceAtLeast(if(minHzListForAdp?.indexOf(48) != -1) {48} else{ 60})
        }
        isFakeAdaptive.set(isFakeAdaptive())//don't interchange
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(appCtx, ComponentName(appCtx, QSTileMinHz::class.java))
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    internal fun updateAdaptiveModCachedParams() {
        synchronized(mLock) {
            canApplyFakeAdaptive = canApplyFakeAdaptiveInt()//don't interchange
            isFakeAdaptive.set(isFakeAdaptive())//don't interchange
            prrActive.set(
                if (isPowerSaveMode.get() == true && isPremium.get()!!) {
                    UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRatePsm
                } else {
                    UtilsPrefsGmhSt.instance(appCtx).hzPrefMaxRefreshRate
                }
            )
            lrrPref.set(UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt)
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    private fun canApplyFakeAdaptiveInt(): Boolean {

        return isOfficialAdaptive && (currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS)
                && (hasWriteSecureSetPerm || gmhAccessInstance != null/*isAccessibilityEnabled(
            appCtx, GalaxyMaxHzAccess::class.java
        )*/)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    private fun isFakeAdaptive(): Boolean {
        return (currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS)
                && (hasWriteSecureSetPerm || gmhAccessInstance != null)
                && (if (isOfficialAdaptive) (UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt < UtilsDeviceInfoSt.instance(appCtx).regularMinHz) else true)
    }



/* private fun getPrefOrCurrentRefreshRateMode(): String{
     return UtilsPrefsGmhSt.instance(appCtx).gmhPrefRefreshRateModePref ?: UtilDeviceInfoSt.instance(appCtx).getSamRefreshRateMode()
 }*/
}