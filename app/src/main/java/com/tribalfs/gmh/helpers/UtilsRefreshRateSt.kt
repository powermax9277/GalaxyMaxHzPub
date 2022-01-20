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
import com.tribalfs.gmh.AccessibilityPermission.isAccessibilityEnabled
import com.tribalfs.gmh.GalaxyMaxHzAccess
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.CacheSettings.canApplyFakeAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.highestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isMultiResolution
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isOnePlus
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
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
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.MIN_REFRESH_RATE
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.ONEPLUS_RATE_MODE_ALWAYS
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.ONEPLUS_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.ONEPLUS_SCREEN_REFRESH_RATE
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.PEAK_REFRESH_RATE
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE_ALWAYS
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE_COVER
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.STANDARD_REFRESH_RATE_HZ
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.USER_REFRESH_RATE
import com.tribalfs.gmh.profiles.*
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
import com.tribalfs.gmh.profiles.ModelNumbers.fordableWithHrrExternal
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import com.tribalfs.gmh.tiles.QSTileMaxHz
import com.tribalfs.gmh.tiles.QSTileMinHz
import com.tribalfs.gmh.tiles.QSTileResSw
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject


class UtilsRefreshRateSt private constructor (val context: Context)  {

    companion object : SingletonMaker<UtilsRefreshRateSt, Context>(::UtilsRefreshRateSt){
        val refreshRateModes = listOf(REFRESH_RATE_MODE_SEAMLESS, REFRESH_RATE_MODE_ALWAYS, REFRESH_RATE_MODE_STANDARD)
    }

    private val appCtx = context.applicationContext
    internal val mContentResolver = appCtx.contentResolver
    internal val mUtilsPrefsGmh by lazy {UtilsPrefsGmhSt.instance(appCtx)}
    internal val mUtilsDeviceInfo by lazy { UtilsDeviceInfoSt.instance(appCtx)}
    internal val mSyncer by lazy { Syncer(appCtx) }
    private val qsMinHzTileComponent = ComponentName(appCtx, QSTileMinHz::class.java)
    private val qsMaxHzTileComponent = ComponentName(appCtx, QSTileMaxHz::class.java)
    @ExperimentalCoroutinesApi
    private val qsResoTileComponent = ComponentName(appCtx, QSTileResSw::class.java)

    //var adaptiveHzListLimiter = highestHzForAllMode //STANDARD_REFRESH_RATE_HZ

    private val _listedHighestHz: Float?
        get() {
            return when (mUtilsDeviceInfo.deviceModel) {
                S20FE5G, S20FE, S21_U, TS7P, TS7W, TS7L, TS7LW, ZF3, ZFp3 -> 120f
                else -> null
            }
        }


    @ExperimentalCoroutinesApi
    suspend fun initProfiles(): Boolean {
        //Source 1: backend
        val isLoadedFromBackEnd = withContext(Dispatchers.IO) { loadProfilesFromBackEnd() }
        if (isLoadedFromBackEnd) {
            return true
        } else {
            //Restore the previously fetched profiled rom backend, if any
            if (mUtilsPrefsGmh.prefProfileFetched && loadProfilesFromPref()) {
                updateCacheSettings()
                ProfilesObj.loadComplete = true
                return true
            }
        }

        //Source 2: predefined
        val isLoadedFromPredefined = withContext(Dispatchers.IO) { loadFromPredefinedProfiles() }
        if (isLoadedFromPredefined) {
            return true
        }

        //Source 3: scan from device
        return withContext(Dispatchers.IO) {
            readAndLoadProfileFromPhone()
        }

    }


    @ExperimentalCoroutinesApi
    @Synchronized
    private fun updateCacheSettings(){
        supportedHzIntAllMod = getSupportedHzIntAllModUpd()
        highestHzForAllMode = supportedHzIntAllMod?.maxOrNull()?: STANDARD_REFRESH_RATE_HZ//?:getHighestHzForAllModeUpd().toInt()
        lowestHzForAllMode = supportedHzIntAllMod?.minOrNull()?: STANDARD_REFRESH_RATE_HZ//?:getLowestHzForAllModeUpd().toInt()
        modesWithLowestHz = getModesWithHz(lowestHzForAllMode)
        isOfficialAdaptive = isAdaptiveSupportedUpd()
        isMultiResolution = isMultiResolutionUpd()
        minHzListForAdp = getMinHzListForAdpUpd()
        updateModeBasedVariables()
    }

    @ExperimentalCoroutinesApi
    @Synchronized
    fun updateModeBasedVariables(){
        supportedHzIntCurMod = getSupportedHzIntCurModUpd()
        samRefreshRateMode.let {
            if (!isOfficialAdaptive && preventHigh && it == REFRESH_RATE_MODE_ALWAYS){
                setRefreshRateMode(REFRESH_RATE_MODE_SEAMLESS)
                return
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

            requestListeningAllTiles()
        }
    }

    @ExperimentalCoroutinesApi
    internal fun requestListeningAllTiles(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(appCtx, qsMinHzTileComponent)
            TileService.requestListeningState(appCtx, qsMaxHzTileComponent)
            TileService.requestListeningState(appCtx, qsResoTileComponent)
        }
    }

    private fun requestListeningHzTiles(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(appCtx, qsMinHzTileComponent)
            TileService.requestListeningState(appCtx, qsMaxHzTileComponent)
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun loadProfilesFromBackEnd(): Boolean{

        if (mUtilsPrefsGmh.gmhRefetchProfile) {//Forced or Scheduled Refetch
            mSyncer.fetchProfileFromBackEnd()?.let { jo ->
                try {
                    jo.optJSONArray(Syncer.KEY_JSON_ADAPTIVES)?.let{
                        updateOfficialAdaptives(it)
                    }
                } catch (_: Exception) { }

                return try {
                    mUtilsPrefsGmh.gmhPrefDisplayModesObjectInJson = jo[Syncer.KEY_JSON_REFRESH_RATES_PROFILE] as JSONObject
                    mUtilsPrefsGmh.gmhRefetchProfile = false
                    mUtilsPrefsGmh.prefProfileFetched = true
                    if (loadProfilesFromPref()) {
                        updateCacheSettings()
                        ProfilesObj.loadComplete = true
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


    @ExperimentalCoroutinesApi
    private fun loadFromPredefinedProfiles(): Boolean{
        // Log.d(TAG, "isLocalProfileSaved: called")
        val isLocalProfileSaved = (PredefinedProfiles.get(mUtilsDeviceInfo.deviceModel))

        return if (isLocalProfileSaved != null) {
            mUtilsPrefsGmh.gmhPrefDisplayModesObjectInJson = isLocalProfileSaved
            if (loadProfilesFromPref()) {
                updateCacheSettings()
                ProfilesObj.loadComplete = true
                true
            }else{
                false
            }
        } else {
            false
        }
    }


    private suspend fun readAndLoadProfileFromPhone(): Boolean = withContext(Dispatchers.IO) {


        val isStandardMode = (mUtilsDeviceInfo.manufacturer == "SAMSUNG") && samRefreshRateMode == REFRESH_RATE_MODE_STANDARD

        if (isStandardMode){
            launch(Dispatchers.Main) {
                var x =3
                while (x > 0) {
                    delay(1000)
                    Toast.makeText(
                        appCtx,
                        "If profiles are not loaded properly, turn ON 'High' or 'Adaptive' refresh rate in this phone's settings and try to 'Reload Profiles'.",
                        Toast.LENGTH_LONG
                    ).show()
                    x--
                }
            }
        }

        val internalProfilesJson = InternalProfiles.loadToProfilesObj(
            currentModeOnly = false,
            overwriteExisting = true,
            mUtilsRefreshRateSt = this@UtilsRefreshRateSt
        )

        if (internalProfilesJson.length() > 0) {
            mUtilsPrefsGmh.gmhPrefDisplayModesObjectInJson = internalProfilesJson
            var isComplete = !isStandardMode
            if (isComplete) {
                refreshRateModes.forEach { rrm ->
                    if (!ProfilesObj.refreshRateModeMap.keys.contains("$displayId-${rrm}")) {
                        isComplete = false
                    }
                }
            }
            ProfilesObj.loadComplete = isComplete

            updateCacheSettings()

            //if (ProfilesObj.loadComplete) {
            //  Log.d(TAG, "Update config complete")
            if (!mUtilsPrefsGmh.gmhPrefIsHzSynced && displayId == Display.DEFAULT_DISPLAY) {
                // Log.d(TAG, "ProfilesSync(appCtx).postProfiles() called")
                CoroutineScope(Dispatchers.IO).launch {
                    mSyncer.postProfileToBackEnd()?.let { jo ->
                        try {
                            jo.optJSONArray(Syncer.KEY_JSON_ADAPTIVES)?.let { adt ->
                                updateOfficialAdaptives(adt)
                                mUtilsPrefsGmh.gmhPrefIsHzSynced = true
                            }
                        } catch (_: Exception) {
                        }
                    }// syncConfig()
                }
            }
            // }
            return@withContext true
        } else {
            return@withContext false
        }
    }


    private fun updateOfficialAdaptives(releaseArr: JSONArray){
        val adaptiveModels = mutableListOf<String>()
        for (i in 0 until releaseArr.length()) {
            adaptiveModels.add(
                releaseArr.getString(i).trim { it <= ' ' })
        }
        ProfilesObj.adaptiveModelsObj.clear()
        ProfilesObj.adaptiveModelsObj.addAll(adaptiveModels)
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
                ProfilesObj.refreshRateModeMap[key] = resMapList
            }
            return true
        }else{
            //  Log.d(TAG, "No saved refresh rate profiles found.")
            return false
        }
    }



    private fun getModesWithHz(searchHz: Int): List<String>?{
        val arrStr: MutableList<String> = arrayListOf()
        refreshRateModes.forEach { mode ->
            ProfilesObj.refreshRateModeMap["$displayId-$mode"]?.forEach{ resoDetails ->
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


    @Synchronized
    fun getThisRrmAndResoHighestHz(resStrLxw: String?, rrm: String?): Float {
        val curResStrLxw = resStrLxw ?: mUtilsDeviceInfo.getDisplayResoStr("x")
        val key = if (rrm != null) "${displayId}-$rrm" else getKey()
        if (ProfilesObj.refreshRateModeMap[key] != null) {
            ProfilesObj.refreshRateModeMap[key]?.forEach {
                if (it.containsKey(curResStrLxw)) return it[curResStrLxw]?.highestHz!!
            }
        }
        //fallback if config is not yet loaded, will also checked for custom reso
        return mUtilsDeviceInfo.getMaxHzForCurrentReso(curResStrLxw)
    }



    fun getResolutionsForKey(key: String?): List<Map<String, ResolutionDetails>>? {
        return ProfilesObj.refreshRateModeMap[key ?: getKey()]
    }


    
    suspend fun getDisplayModesStrGmh(): String {

        if (!ProfilesObj.loadComplete) {
            InternalProfiles.loadToProfilesObj(true,
                overwriteExisting = false,
                mUtilsRefreshRateSt = this@UtilsRefreshRateSt
            )
        }

        var modes = ""
        val curResStrLxw = mUtilsDeviceInfo.getDisplayResoStr("x")
        ProfilesObj.refreshRateModeMap[getKey()]?.forEach { map ->
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


    private fun isAdaptiveSupportedUpd(): Boolean {
        // if (BuildConfig.DEBUG) return false
        return mUtilsDeviceInfo.deviceModel.let { model ->
            ProfilesObj.adaptiveModelsObj.run {
                if (isNotEmpty()) {
                    indexOf(model) != -1
                } else {//if empty
                    mUtilsPrefsGmh.gmhPrefGetAdaptives?.let {
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


    private fun getKey(): String{
        return "$displayId-${
            Settings.Secure.getString(
                appCtx.contentResolver,
                REFRESH_RATE_MODE) ?: 0}"
    }


    fun getCurrentResWithName(): String{
        val res = mUtilsDeviceInfo.getDisplayResoStr("x").split("x")//getDisplayResolution()
        return "${UtilsResoName.getName(res[0].toInt(), res[1].toInt())}(${res[0]}x${res[1]})"
    }



    private fun getSupportedHzIntAllModUpd(): List<Int>{
        val arrayHz: MutableList<Int> = arrayListOf()
        refreshRateModes.forEach { mode ->
            ProfilesObj.refreshRateModeMap["$displayId-$mode"]?.forEach { resoDetails ->
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


    @Synchronized
    private fun getMinHzListForAdpUpd(): List<Int> {
        val curResStr = mUtilsDeviceInfo.getDisplayResoStr("x")
        val arrStr: MutableList<Int> = arrayListOf()
        ProfilesObj.refreshRateModeMap["$displayId-$REFRESH_RATE_MODE_SEAMLESS"]?.forEach {
            if (it.containsKey(curResStr)) {
                it[curResStr]?.let { it1 ->
                    for (i in it1.refreshRates) {
                        if (i < highestHzForAllMode) {
                            arrStr.add(i.toInt())
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val curResStrMode = mUtilsDeviceInfo.getDisplayResFromModeStr("x")
                if (it.containsKey(curResStrMode)) {
                    it[curResStrMode]?.let { it1 ->
                        for (i in it1.refreshRates) {
                            // if (i <= STANDARD_REFRESH_RATE_HZ) {
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
                for (i in it) {
                    if (i <= highestHzForAllMode) {
                        arrStr.add(i.toInt())
                    }
                }
            }
        }

        return arrStr.distinct().sorted()
    }


    private fun getSupportedHzIntCurModUpd(): List<Int>{
        val curResStr = mUtilsDeviceInfo.getDisplayResoStr("x")
        val arrayHz: MutableList<Int> = arrayListOf()
        ProfilesObj.refreshRateModeMap[getKey()]?.forEach {
            if (it.containsKey(curResStr)) {
                it[curResStr]?.let { it1 ->
                    for (i in it1.refreshRates) {
                        arrayHz.add(i.toInt())
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val curResStrMode = mUtilsDeviceInfo.getDisplayResFromModeStr("x")
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
            mUtilsDeviceInfo.getDisplayModesSet()[mUtilsDeviceInfo.getDisplayResoStr("x")]

        if (refreshRatesfromDevice == null) {
            refreshRatesfromDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mUtilsDeviceInfo.getDisplayModesSet()[mUtilsDeviceInfo.getDisplayResFromModeStr(
                    "x"
                )]
            } else {
                @Suppress("DEPRECATION")
                mUtilsDeviceInfo.currentDisplay.supportedRefreshRates.toList()
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
        ProfilesObj.refreshRateModeMap[key]?.forEach {
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


    
    fun getResoHighestHzForAllMode(resStrLxw: String?): Float{

        var highestResult = _listedHighestHz
        if (highestResult != null) return highestResult

        runBlocking {
            if (!ProfilesObj.loadComplete) {
                //Log.d(TAG, "updateConfig() called from getResoHighestHzForAllMode")
                readAndLoadProfileFromPhone()
            }
        }

        val curResStr = resStrLxw ?: mUtilsDeviceInfo.getDisplayResoStr("x")

        // if (mUtilsDeviceInfo.deviceIsSamsung) {
        refreshRateModes.forEach { rrm ->
            val key = "$displayId-$rrm"
            ProfilesObj.refreshRateModeMap[key]?.forEach {
                if (it[curResStr] != null && it[curResStr]?.highestHz!! > highestResult ?: 0f) {
                    highestResult = it[curResStr]?.highestHz!!
                }
            }

            if (highestResult == null) {//maybe using custom resolution
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val curResStrFromMod = mUtilsDeviceInfo.getDisplayResFromModeStr("x")
                    ProfilesObj.refreshRateModeMap[key]?.forEach {
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
        try {
            Settings.System.putString(mContentResolver, PEAK_REFRESH_RATE, refreshRate.toString())
            if (isXiaomi) {
                Settings.System.putString(
                    mContentResolver,
                    USER_REFRESH_RATE,
                    refreshRate.toString()
                )
            }
        }catch(_:Exception){
            Toast.makeText(appCtx, "Error! ${appCtx.getString(R.string.enable_write_settings)}", Toast.LENGTH_SHORT).show()
        }
    }

    internal fun setMinRefreshRate(refreshRate: Int){
        try {
            Settings.System.putString(mContentResolver, MIN_REFRESH_RATE, refreshRate.toString())
        }catch(_:Exception){
            Toast.makeText(appCtx, "Error! ${appCtx.getString(R.string.enable_write_settings)}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun deleteRefreshRate(name: String){
        try {
            mContentResolver.delete(
                Uri.parse("content://settings/system"), "name = ?", arrayOf(
                    name
                )
            )
        } catch (_: Exception) {
        }
    }


    internal fun clearPeakAndMinRefreshRate() {
        deleteRefreshRate(PEAK_REFRESH_RATE)
        deleteRefreshRate(MIN_REFRESH_RATE)
    }


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
        val reso = mUtilsDeviceInfo.getDisplayResolution()
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


    internal var samRefreshRateMode: String
        get() {
            return (Settings.Secure.getString(mContentResolver, REFRESH_RATE_MODE)
                ?: 0).toString()
        }
        set(mode) {try{Settings.Secure.putString(mContentResolver, REFRESH_RATE_MODE, mode)}catch(_:Exception){}}



    @Synchronized
    internal fun setRefreshRateMode(mode: String) : Boolean{
        return try {
            Settings.Secure.putInt(mContentResolver, REFRESH_RATE_MODE, mode.toInt())
                    && (
                    if (fordableWithHrrExternal.indexOf(mUtilsDeviceInfo.deviceModel) != -1) {
                        Settings.Secure.putString(mContentResolver, REFRESH_RATE_MODE_COVER, mode)
                    } else {
                        true
                    })
                    && (
                    if (isOnePlus) {
                        val onePlusModeEq = if (mode == REFRESH_RATE_MODE_ALWAYS) ONEPLUS_RATE_MODE_ALWAYS else ONEPLUS_RATE_MODE_SEAMLESS
                        Settings.Global.putString(mContentResolver, ONEPLUS_SCREEN_REFRESH_RATE, onePlusModeEq)
                    } else {
                        true
                    }
                    )
        }catch(_:java.lang.Exception){false}
    }


    internal fun getPeakRefreshRate(): Int {
        var prr =  if (isFakeAdaptive.get()!!) {
            (if (keepModeOnPowerSaving && isPowerSaveModeOn.get() ==true)
                mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm
            else
                mUtilsPrefsGmh.hzPrefMaxRefreshRate
                    ).let{
                    if (it != -1){
                        it
                    }else{
                        getThisRrmAndResoHighestHz(null, null).toInt().let{ highestHz ->
                            mUtilsPrefsGmh.hzPrefMaxRefreshRate = highestHz
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


    private fun getPeakRefreshRateFromSettings(): Int? {
        return try {
            val prr =  Settings.System.getString(mContentResolver, PEAK_REFRESH_RATE)
            prr.toInt()
        } catch (_: Exception) {
           //TODO(REFLECTION: android.provider.Settings.config)
            //null
            mUtilsDeviceInfo.currentDisplay.refreshRate.toInt()
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    internal fun setPrefOrAdaptOrHighRefreshRateMode(resStrLxw: String?): Boolean{
        try {
            val rrm =
                if (mUtilsPrefsGmh.gmhPrefRefreshRateModePref != null
                    && mUtilsPrefsGmh.gmhPrefRefreshRateModePref != REFRESH_RATE_MODE_STANDARD) {
                    mUtilsPrefsGmh.gmhPrefRefreshRateModePref
                } else {
                    if (isOfficialAdaptive) {
                        REFRESH_RATE_MODE_SEAMLESS
                    } else {
                        REFRESH_RATE_MODE_ALWAYS
                    }
                }
            return tryThisRrm(rrm!!, resStrLxw)
        }catch (_:Exception){
            return false
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    internal fun tryThisRrm(rrm: String, resStrLxw: String?) : Boolean {
        if (hasWriteSecureSetPerm) {
            return if (rrm != REFRESH_RATE_MODE_STANDARD) {
                val highest = getThisRrmAndResoHighestHz(resStrLxw, rrm)
                if (highest > STANDARD_REFRESH_RATE_HZ) {
                    setRefreshRateMode(rrm) && setRefreshRate(prrActive.get()!!, null)
                } else {
                    false
                }
            }else{
                setRefreshRateMode(rrm)
            }
        }
        return false
    }


    internal fun applyMinHz(){
        setMinRefreshRate(mUtilsPrefsGmh.gmhPrefMinHzAdapt)
        lrrPref.set(mUtilsPrefsGmh.gmhPrefMinHzAdapt)
        isFakeAdaptive.set(isFakeAdaptive())//don't interchange
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(appCtx, qsMinHzTileComponent)
        }
    }


    internal fun updateRefreshRateParams() {
        canApplyFakeAdaptive = canApplyFakeAdaptiveInt()//don't interchange
        isFakeAdaptive.set(isFakeAdaptive())//don't interchange
        prrActive.set(
            if (isPowerSaveModeOn.get() == true && isPremium.get()!!) {
                mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm
            } else {
                mUtilsPrefsGmh.hzPrefMaxRefreshRate
            }
        )
        lrrPref.set(mUtilsPrefsGmh.gmhPrefMinHzAdapt)
    }


    private fun canApplyFakeAdaptiveInt(): Boolean {

        return isOfficialAdaptive && (currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS)
                && (hasWriteSecureSetPerm || isAccessibilityEnabled(
            appCtx, GalaxyMaxHzAccess::class.java
        ))
    }


    private fun isFakeAdaptive(): Boolean {
        return (currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS)
                && (hasWriteSecureSetPerm || isAccessibilityEnabled(
            appCtx,
            GalaxyMaxHzAccess::class.java
        ))
                && (if (isOfficialAdaptive) (mUtilsPrefsGmh.gmhPrefMinHzAdapt < STANDARD_REFRESH_RATE_HZ) else true)
    }

/* private fun getPrefOrCurrentRefreshRateMode(): String{
     return mUtilsPrefsGmh.gmhPrefRefreshRateModePref ?: mUtilsDeviceInfo.getSamRefreshRateMode()
 }*/
}