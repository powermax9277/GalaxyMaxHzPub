package com.tribalfs.gmh

//import com.tribalfs.gmh.helpers.CacheSettings.isS20Series
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
import android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
import android.provider.Settings.System.SCREEN_BRIGHTNESS
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.UtilAccessibilityService.allowAccessibility
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.TIMEOUT_FACTOR
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveAccessTimeout
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveDelayMillis
import com.tribalfs.gmh.helpers.CacheSettings.brightnessThreshold
import com.tribalfs.gmh.helpers.CacheSettings.currentBrightness
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSystemSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.hzNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.ignoreRrmChange
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptiveValid
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveMode
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.isSamsung
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.isSpayInstalled
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.lrrPref
import com.tribalfs.gmh.helpers.CacheSettings.preventHigh
import com.tribalfs.gmh.helpers.CacheSettings.turnOff5GOnPsm
import com.tribalfs.gmh.helpers.DozeUpdater.updateDozValues
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.profiles.ProfilesObj.isProfilesLoaded
import com.tribalfs.gmh.sharedprefs.LIC_TYPE_ADFREE
import com.tribalfs.gmh.sharedprefs.LIC_TYPE_TRIAL_ACTIVE
import com.tribalfs.gmh.sharedprefs.NOT_USING
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct
import kotlinx.coroutines.*
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraHttpSender
import org.acra.data.StringFormat
import org.acra.sender.HttpSender


@ExperimentalCoroutinesApi
@AcraCore(reportFormat= StringFormat.JSON)
@AcraHttpSender(uri = "https://script.google.com/macros/s/AKfycbybr-F6rCLr8fTk0jYvz_ohCOcNLwsSPCNxhYUlX-KtvLE9JT0/exec",
    httpMethod = HttpSender.Method.POST,
    basicAuthLogin = "",
    basicAuthPassword = "")
class MyApplication : Application() {

    companion object{
        lateinit var applicationName: String
        @Volatile internal var ignoreAccessibilityChange = false
        internal val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    private val mUtilsRefreshRateSt by lazy {UtilRefreshRateSt.instance(applicationContext)}
    private val mUtilsPrefsAct by lazy { UtilsPrefsAct(applicationContext) }
    private val brightnessFloatUri = Settings.System.getUriFor(SCREEN_BRIGHTNESS_FLOAT)
    private val brightnessUri = Settings.System.getUriFor(SCREEN_BRIGHTNESS)
    private val refreshRateModeUri = Settings.Secure.getUriFor(REFRESH_RATE_MODE)
    private val psm5gModeUri = Settings.Global.getUriFor(PSM_5G_MODE)
    private val deviceIdleConstantsUri = Settings.Global.getUriFor(DEVICE_IDLE_CONSTANTS)
    private val batterySaverConstantsUri = Settings.Global.getUriFor(BATTERY_SAVER_CONSTANTS)
    private val sysuiQsTilesUri = Settings.Secure.getUriFor(SYSUI_QS_TILES)
    private val currentResolutionUri = Settings.Global.getUriFor(DISPLAY_SIZE_FORCED)
    private val devSettingsUri =  Settings.Global.getUriFor(DEVELOPMENT_SETTINGS_ENABLED)
    private val accessibilityServiceUri =  Settings.Secure.getUriFor(ENABLED_ACCESSIBILITY_SERVICES)
    private lateinit var mContentResolver: ContentResolver
    private lateinit var myBrightnessObserver: MyBrightnessObserver
    private var dozeConfCtr = 0

    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    inner class MyRequiredObservers(h: Handler?) : ContentObserver(h) {

        private var dozeConflictChecker: Job? = null

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            when (uri) {
                refreshRateModeUri -> {
                    if (!ignoreRrmChange) {
                        applicationScope.launch {
                            while (!isProfilesLoaded) { delay(200) }
                            mUtilsRefreshRateSt.updateModeBasedVariables()
                            delay(250)
                            mUtilsRefreshRateSt.updateAdaptiveModCachedParams()
                            gmhAccessInstance?.setupAdaptiveEnhancer()

                            currentRefreshRateMode.get().let{
                                if (it != REFRESH_RATE_MODE_STANDARD) {
                                    mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefRefreshRateModePref = it
                                }
                            }
                        }
                    }else{
                        ignoreRrmChange = false
                    }
                }

                psm5gModeUri -> {
                    turnOff5GOnPsm = isTurnOff5GOnPsm()
                }


                deviceIdleConstantsUri -> {
                    applicationScope.launch {
                        if (mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefQuickDozeIsOn && DozeUpdater.getDozeVal(mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefGDozeModOpt)
                            != Settings.Global.getString(mContentResolver, DEVICE_IDLE_CONSTANTS)
                        ) {
                            dozeConfCtr += 1
                            applicationContext.updateDozValues(
                                true,
                                mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefGDozeModOpt
                            )

                            dozeConflictChecker?.cancel()
                            dozeConflictChecker = null
                            dozeConflictChecker = applicationScope.launch(Dispatchers.IO) {
                                delay(4000)
                                if (dozeConfCtr > 5){
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(applicationContext,"Another app is clashing $applicationName ${getString(R.string.quick_doz_mod)}. Disable it now.", Toast.LENGTH_LONG).show()
                                    }
                                }else{
                                    dozeConfCtr = 0
                                }
                            }
                            dozeConflictChecker?.start()
                        }
                    }
                }



                batterySaverConstantsUri -> {
                    if (mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefQuickDozeIsOn) {
                        DozePSCChecker.check(
                            applicationContext,
                            enable = true,
                            updateIfDisable = false
                        )
                    }
                }


                sysuiQsTilesUri, devSettingsUri -> {
                    if (mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefSensorsOff) {
                        applicationScope.launch {
                            if (UtilNotifBarSt.instance(applicationContext).checkQsTileInPlace() != true) {
                                mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefSensorsOff = false
                            }
                        }
                    }
                }

                accessibilityServiceUri -> {
                    if (!ignoreAccessibilityChange) {
                        applicationScope.launch {
                            if (!UtilAccessibilityService.isAccessibilityEnabled(applicationContext/*, GalaxyMaxHzAccess::class.java*/)) {
                                notifyAccessibilityNeed()
                                delay(1000)
                            }
                        }
                    } else {
                        //reset to default
                        ignoreAccessibilityChange = false
                    }

                }

                currentResolutionUri ->{
                    applicationScope.launch {
                        delay(1000)
                        mUtilsRefreshRateSt.requestListeningAllTiles()
                    }
                }
            }
        }


        fun start(){
            listOf(
                refreshRateModeUri,
                psm5gModeUri,
                deviceIdleConstantsUri,
                batterySaverConstantsUri,
                sysuiQsTilesUri,
                devSettingsUri,
                accessibilityServiceUri,
                currentResolutionUri
            ).forEach {
                mContentResolver.registerContentObserver(
                    it, false, this
                )
            }
        }
    }


    inner class MyBrightnessObserver(h: Handler?) : ContentObserver(h) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            when (uri) {
                brightnessFloatUri, brightnessUri -> {
                    applicationScope.launch {
                        if (isScreenOn) {
                            currentBrightness.set(mUtilsRefreshRateSt.mUtilsDeviceInfo.getScreenBrightnessPercent())
                        }
                    }
                }
            }
        }


        fun start(){
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                mContentResolver.registerContentObserver(
                    brightnessFloatUri, false, this
                )
            }else {
                mContentResolver.registerContentObserver(
                    brightnessUri, false, this
                )
            }

        }

        fun stop(){
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                mContentResolver.registerContentObserver(
                    brightnessFloatUri, false, this
                )
            }else{
                mContentResolver.registerContentObserver(
                    brightnessUri, false, this)

            }
        }

    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        //Log.d(TAG, "onCreate called")
        super.onCreate()
        applicationName = when (val stringId = applicationInfo.labelRes) {
            0 -> applicationInfo.nonLocalizedLabel.toString()
            else -> getString(stringId)
        }

        /***Required before checkAccessibility call**/
        currentRefreshRateMode.set(mUtilsRefreshRateSt.samRefreshRateMode)
        isOfficialAdaptive = mUtilsRefreshRateSt.isAdaptiveSupportedUpd()
        /***Required before checkAccessibility call**/

        checkAccessibility(null)

        mContentResolver = applicationContext.contentResolver

        val mHandler = Handler(Looper.getMainLooper())

        MyRequiredObservers(mHandler).start()

        myBrightnessObserver = MyBrightnessObserver(mHandler)

        isFakeAdaptiveValid.addOnPropertyChangedCallback (
            object: OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    if (isFakeAdaptiveValid.get() == true) {
                        myBrightnessObserver.start()
                    } else {
                        myBrightnessObserver.stop()
                    }
                }
            }
        )

        if (!BuildConfig.DEBUG && (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)) { throw NullPointerException() }

        applicationScope.launch {
            withContext(Dispatchers.IO){ cacheSettings()} //suspend function

            var waited = 0
            var gmhInstanceCreated = gmhAccessInstance != null
            while (waited <= 5000 && !gmhInstanceCreated) {
                delay(500)
                waited += 500
                gmhInstanceCreated = gmhAccessInstance != null
            }

            gmhAccessInstance?.setupAdaptiveEnhancer()

            HzServiceHelperStn.instance(applicationContext).switchHz()

            NetSpeedServiceHelperStn.instance(applicationContext).updateNetSpeed()

        }


        applicationScope.launch {

            Brand.set(mUtilsRefreshRateSt.mUtilsDeviceInfo.manufacturer)

            while (!isProfilesLoaded) {
                delay(250)
            }

            if (
                isSamsung
                && !mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefSettingListDone
                && mUtilsRefreshRateSt.mUtilsDeviceInfo.oneUiVersion != null
                &&  mUtilsRefreshRateSt.mUtilsDeviceInfo.oneUiVersion!!  >= 4.0
            /*   && highestHzForAllMode > REFRESH_RATE_MODE_STANDARD.toInt()*/
            ) {
                mUtilsRefreshRateSt.mSyncer.postSettingsList()
                mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefSettingListDone = true
            }
        }

    }



    private suspend fun cacheSettings() {
        val mUtilsPrefsGmh = mUtilsRefreshRateSt.mUtilsPrefsGmh

        isPowerSaveMode.set(
            if (mUtilsPrefsGmh.gmhPrefPsmIsOffCache)
                !mUtilsPrefsGmh.gmhPrefPsmIsOffCache
            else
                mUtilsRefreshRateSt.mUtilsDeviceInfo.isPowerSavingsMode()
        )
        turnOff5GOnPsm = isTurnOff5GOnPsm()

        UtilPermSt.instance(applicationContext).apply{
            hasWriteSecureSetPerm = this.hasWriteSecurePerm()
            hasWriteSystemSetPerm = this.hasWriteSystemPerm()
        }

        isSpayInstalled = isSpayInstalled()

        keepModeOnPowerSaving = mUtilsPrefsGmh.gmhPrefKmsOnPsm
        brightnessThreshold.set(
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                mUtilsPrefsGmh.gmhPrefGAdaptBrightnessMin
            }else {
                (mUtilsPrefsGmh.gmhPrefGAdaptBrightnessMin.toFloat() * 2.55f).toInt()
            }
        )
        currentBrightness.set(mUtilsRefreshRateSt.mUtilsDeviceInfo.getScreenBrightnessPercent())

        mUtilsPrefsGmh.hzPrefAdaptiveDelay.let {
            adaptiveDelayMillis = it
            adaptiveAccessTimeout = it * TIMEOUT_FACTOR.toLong()
        }

        isPremium.set((mUtilsPrefsAct.gmhPrefLicType == LIC_TYPE_ADFREE
                || mUtilsPrefsAct.gmhPrefLicType == LIC_TYPE_TRIAL_ACTIVE)
                && (mUtilsPrefsAct.gmhPrefSignature == PackageInfo.getSignatureString(applicationContext)))

        isProfilesLoaded = withContext(Dispatchers.IO) { mUtilsRefreshRateSt.initProfiles() }

        preventHigh = mUtilsPrefsGmh.gmhPrefPreventHigh

        hzNotifOn.set (mUtilsPrefsGmh.gmhPrefHzIsOn && mUtilsPrefsGmh.gmhPrefHzNotifIsOn)

        mUtilsRefreshRateSt.updateAdaptiveModCachedParams()

    }


    private fun isSpayInstalled(): Boolean {
        return try {
            packageManager.getApplicationInfo("com.samsung.android.spay", 0)
            true
        } catch (_: Exception) {
            false
        }
    }


    private fun checkAccessibility(required:Boolean?): Boolean{
        val accesRequired = required?:mUtilsRefreshRateSt.mUtilsPrefsGmh.getEnabledAccessibilityFeatures().isNotEmpty()

        if (accesRequired) {
            return if (
                (isSpayInstalled == false || mUtilsRefreshRateSt.mUtilsPrefsGmh.hzPrefSPayUsage == NOT_USING)
                && hasWriteSecureSetPerm
            ) {
                allowAccessibility(
                    applicationContext,
                    true
                )
                true
            } else {
                false
            }
        }else{
           return gmhAccessInstance != null
        }
    }


    private fun notifyAccessibilityNeed() = applicationScope.launch{

        val featuresOn = mUtilsRefreshRateSt.mUtilsPrefsGmh.getEnabledAccessibilityFeatures()

        featuresOn.size.let{
            if (it > 0) {

                launch(Dispatchers.Main) {
                    var toastRpt = it
                    //Will auto turn it back ON if have permission
                    val toastMsg = if (checkAccessibility(true)) {
                        applicationContext.resources.getQuantityString(
                            R.plurals.access_notif,
                            featuresOn.size,
                            "\n" + featuresOn.joinToString("" +
                                    "\n")
                        )
                    }else{
                        applicationContext.getString(
                            R.string.no_access_notif,
                            "\n" + featuresOn.joinToString("" +
                                    "\n")
                        )
                    }
                    while(toastRpt >= 0) {
                        Toast.makeText(
                            applicationContext,
                            toastMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                        delay(400)
                        toastRpt -= 1
                    }
                }

            } else  {
                mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefForceLowestSoIsOn = false
                mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefDisableSyncIsOn = false
                if (isFakeAdaptive.get() == true && isOfficialAdaptive) {
                    lrrPref.set(STANDARD_REFRESH_RATE_HZ)
                    mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefMinHzAdapt = STANDARD_REFRESH_RATE_HZ
                }
                mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefSensorsOff = false

            }
        }
    }


    private fun isTurnOff5GOnPsm(): Boolean{
        return (Settings.Global.getString(mContentResolver, PSM_5G_MODE)?:"0").split(",")[0] == "1"
    }


    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        ACRA.init(this)
    }
}