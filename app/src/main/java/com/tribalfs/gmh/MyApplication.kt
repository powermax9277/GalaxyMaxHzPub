package com.tribalfs.gmh

//import com.tribalfs.gmh.helpers.CacheSettings.isS20Series
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
import android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.tribalfs.gmh.AccessibilityPermission.allowAccessibility
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.SETUP_ADAPTIVE
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.SETUP_NETWORK_CALLBACK
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.SWITCH_AUTO_SENSORS
import com.tribalfs.gmh.SensorsOffSt.Companion.SYSUI_QS_TILES
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.TIMEOUT_FACTOR
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveAccessTimeout
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveDelayMillis
import com.tribalfs.gmh.helpers.CacheSettings.brightnessThreshold
import com.tribalfs.gmh.helpers.CacheSettings.canApplyFakeAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.currentBrightness
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.highestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.ignoreRrmChange
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptiveValid
import com.tribalfs.gmh.helpers.CacheSettings.isHzNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.isNsNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
import com.tribalfs.gmh.helpers.CacheSettings.isSamsung
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.isSpayInstalled
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.lrrPref
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.turnOff5GOnPsm
import com.tribalfs.gmh.helpers.DozeUpdater.updateDozValues
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.BATTERY_SAVER_CONSTANTS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.DEVICE_IDLE_CONSTANTS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.PSM_5G_MODE
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.SCREEN_BRIGHTNESS_FLOAT
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.STANDARD_REFRESH_RATE_HZ
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.profiles.ModelNumbers.ZF3
import com.tribalfs.gmh.profiles.ProfilesInitializer
import com.tribalfs.gmh.profiles.ProfilesObj.isProfilesLoaded
import com.tribalfs.gmh.profiles.Syncer
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct.Companion.LIC_TYPE_ADFREE
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct.Companion.LIC_TYPE_TRIAL_ACTIVE
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh.Companion.NOT_USING
import kotlinx.coroutines.*

/*@AcraCore(reportFormat= StringFormat.JSON)
@AcraHttpSender(uri = "https://script.google.com/macros/s/AKfycbybr-F6rCLr8fTk0jYvz_ohCOcNLwsSPCNxhYUlX-KtvLE9JT0/exec",
    httpMethod = HttpSender.Method.POST,
    basicAuthLogin = "",
    basicAuthPassword = "")
*/
@ExperimentalCoroutinesApi
class MyApplication : Application() {

    companion object{
        lateinit var applicationName: String
        // private const val TAG = "MyApplication"
        @Volatile internal var ignoreAccessibilityChange = false
        internal val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }



    private val mUtilsPrefsGmh by lazy {UtilsPrefsGmh(applicationContext)}
    private val mUtilsDeviceInfo by lazy { UtilsDeviceInfo(applicationContext) }
    private val internalCert by lazy { Certificate.getEncSig(applicationContext)}
    private val mUtilsPrefsAct by lazy { UtilsPrefsAct(applicationContext) }

    private val brightnessFloatUri = Settings.System.getUriFor(SCREEN_BRIGHTNESS_FLOAT)
    private val refreshRateModeUri = Settings.Secure.getUriFor(REFRESH_RATE_MODE)
    private val psm5gModeUri = Settings.Global.getUriFor(PSM_5G_MODE)
    private val deviceIdleConstantsUri = Settings.Global.getUriFor(DEVICE_IDLE_CONSTANTS)
    private val batterySaverConstantsUri = Settings.Global.getUriFor(BATTERY_SAVER_CONSTANTS)
    private val sysuiQsTilesUri = Settings.Secure.getUriFor(SYSUI_QS_TILES)
    private val devSettingsUri =  Settings.Global.getUriFor(DEVELOPMENT_SETTINGS_ENABLED)
    private val accessibilityServiceUri =  Settings.Secure.getUriFor(ENABLED_ACCESSIBILITY_SERVICES)
    private lateinit var mContentResolver: ContentResolver
    private lateinit var myBrightnessObserver: MyBrightnessObserver

    @RequiresApi(Build.VERSION_CODES.M)
    inner class MyRequiredObservers(h: Handler?) : ContentObserver(h) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            when (uri) {
                refreshRateModeUri -> {
                    if (!ignoreRrmChange) {
                        applicationScope.launch {
                            while (!isProfilesLoaded) { delay(150) }
                            ProfilesInitializer.instance(applicationContext).updateModeBasedVariables()
                            delay(235)
                            updateRefreshRateParams()
                            applicationContext.startService(
                                Intent(applicationContext, GalaxyMaxHzAccess::class.java).apply {
                                    putExtra(SETUP_ADAPTIVE, true)
                                }
                            )
                            //TODO: check
                            mUtilsPrefsGmh.gmhPrefRefreshRateModePref = currentRefreshRateMode.get()

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
                        if (mUtilsPrefsGmh.gmhPrefQuickDozeIsOn && DozeUpdater.getDozeVal(mUtilsPrefsGmh.gmhPrefGDozeModOpt)
                            != Settings.Global.getString(mContentResolver, DEVICE_IDLE_CONSTANTS)
                        ) {
                            applicationContext.updateDozValues(
                                true,
                                mUtilsPrefsGmh.gmhPrefGDozeModOpt
                            )
                        }
                    }
                }

                /*Settings.System.getUriFor(SCREEN_BRIGHTNESS), */
                brightnessFloatUri -> {
                    applicationScope.launch {
                        if (isFakeAdaptiveValid.get() == true && isScreenOn) {
                            currentBrightness.set(mUtilsDeviceInfo.getScreenBrightnessPercent())
                        }
                    }
                }

                batterySaverConstantsUri -> {
                    if (mUtilsPrefsGmh.gmhPrefQuickDozeIsOn) {
                        DozePSCChecker.check(
                            applicationContext,
                            enable = true,
                            updateIfDisable = false
                        )
                    }
                }


                sysuiQsTilesUri, devSettingsUri -> {
                    if (mUtilsPrefsGmh.gmhPrefSensorsOff) {
                        applicationScope.launch {
                            if (!SensorsOffSt.instance(applicationContext).checkQsTileInPlace()) {
                                mUtilsPrefsGmh.gmhPrefSensorsOff = false
                            }
                        }
                    }
                }

                accessibilityServiceUri -> {
                    if (!ignoreAccessibilityChange) {
                        applicationScope.launch {
                            if (!AccessibilityPermission.isAccessibilityEnabled(
                                    applicationContext,
                                    GalaxyMaxHzAccess::class.java
                                )
                            ) {
                                notifyUserEffectOfAccessibility()
                            }
                        }
                    } else {
                        //reset to default
                        ignoreAccessibilityChange = false
                    }
                }
            }
        }

        fun startRequiredObservers(){
            listOf(
                refreshRateModeUri,
                psm5gModeUri,
                deviceIdleConstantsUri,
                batterySaverConstantsUri,
                sysuiQsTilesUri,
                devSettingsUri,
                accessibilityServiceUri
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
                brightnessFloatUri -> {
                    applicationScope.launch {
                        if (isScreenOn) {
                            currentBrightness.set(mUtilsDeviceInfo.getScreenBrightnessPercent())
                        }
                    }
                }
            }
        }


        fun startBrightnessObserver(){
            mContentResolver.registerContentObserver(
                brightnessFloatUri, false, this
            )
        }

        fun stopBrightnessObserver(){
            mContentResolver.registerContentObserver(
                brightnessFloatUri, false, this
            )
        }

        /* fun unObserve(){
             mContentResolver.unregisterContentObserver(this)
         }*/
    }



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        //Log.d(TAG, "onCreate called")
        super.onCreate()
        applicationName = when (val stringId = applicationInfo.labelRes) {
            0 -> applicationInfo.nonLocalizedLabel.toString()
            else -> getString(stringId)
        }

        mContentResolver = applicationContext.contentResolver

        val mHandler = Handler(Looper.getMainLooper())

        MyRequiredObservers(mHandler).startRequiredObservers()

        myBrightnessObserver = MyBrightnessObserver(mHandler)

        isFakeAdaptiveValid.addOnPropertyChangedCallback (
            object: OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    if (isFakeAdaptiveValid.get() == true) {
                        myBrightnessObserver.startBrightnessObserver()
                    } else {
                        myBrightnessObserver.stopBrightnessObserver()
                    }
                }
            }
        )

        if (!BuildConfig.DEBUG && (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)) { throw NullPointerException() }

        applicationScope.launch {
            withContext(Dispatchers.IO){ cacheSettings()} //suspend function
            delay(6500)//DON'T reduce
            checkAccessibility()
        }

        HzServiceHelperStn.instance(applicationContext).startHertz(null, null, null)

        applicationScope.launch {

            setBrand()

            while (!isProfilesLoaded) {
                delay(250)
            }
            if ((!isSamsung || ZF3 == mUtilsDeviceInfo.deviceModel)
                && highestHzForAllMode > REFRESH_RATE_MODE_STANDARD.toInt()
                && !mUtilsPrefsGmh.gmhPrefSettingListDone
            ) {
                Syncer(applicationContext).postSettingsList()
                mUtilsPrefsGmh.gmhPrefSettingListDone = true
            }
        }
    }


    private fun checkAccessibility(){
        if ((isSpayInstalled == false || mUtilsPrefsGmh.hzPrefUsingSPay == NOT_USING) && hasWriteSecureSetPerm) {
            allowAccessibility(
                applicationContext,
                GalaxyMaxHzAccess::class.java,
                true
            )
            applicationContext.startService(
                Intent(applicationContext, GalaxyMaxHzAccess::class.java).apply {
                    putExtra(SETUP_ADAPTIVE, true)
                    putExtra(SETUP_NETWORK_CALLBACK, true)
                    putExtra(SWITCH_AUTO_SENSORS, mUtilsPrefsGmh.gmhPrefSensorsOff)
                }
            )
        }
    }


    @SuppressLint("NewApi")
    @ExperimentalCoroutinesApi
    private suspend fun cacheSettings() {
        isPowerSaveModeOn.set(
            if (mUtilsPrefsGmh.gmhPrefPsmIsOffCache)
                !mUtilsPrefsGmh.gmhPrefPsmIsOffCache
            else
                mUtilsDeviceInfo.isPowerSavingsModeOn()
        )
        turnOff5GOnPsm = isTurnOff5GOnPsm()
        hasWriteSecureSetPerm = UtilsPermSt.instance(applicationContext).hasWriteSecurePerm()
        isSpayInstalled = isSpayInstalled()
        keepModeOnPowerSaving = mUtilsPrefsGmh.gmhPrefKmsOnPsm
        brightnessThreshold.set(mUtilsPrefsGmh.gmhPrefGAdaptBrightnessMin)
        currentBrightness.set(mUtilsDeviceInfo.getScreenBrightnessPercent())
        mUtilsPrefsGmh.hzPrefAdaptiveDelay.let {
            adaptiveDelayMillis = it
            adaptiveAccessTimeout = it * TIMEOUT_FACTOR.toLong()
        }
        isPremium.set( (mUtilsPrefsAct.gmhPrefLicType == LIC_TYPE_ADFREE || mUtilsPrefsAct.gmhPrefLicType == LIC_TYPE_TRIAL_ACTIVE)
                && (mUtilsPrefsAct.gmhPrefSignature == internalCert))


        isProfilesLoaded = withContext(Dispatchers.IO) { ProfilesInitializer.instance(applicationContext).initProfiles() }

        applicationContext.startService(Intent(applicationContext, GalaxyMaxHzAccess::class.java).apply{
            putExtra(SETUP_ADAPTIVE, true)
        })

        updateRefreshRateParams()

        isHzNotifOn.set (mUtilsPrefsGmh.gmhPrefHzIsOn && mUtilsPrefsGmh.gmhPrefHzNotifIsOn)
        isNsNotifOn.set( mUtilsPrefsGmh.gmhPrefNetSpeedIsOn)
        //fixedHzOnSystemUi =

    }


    private fun updateRefreshRateParams(){
        canApplyFakeAdaptive = canApplyFakeAdaptive()//don't interchange
        isFakeAdaptive.set(isFakeAdaptive())//don't interchange
        prrActive.set (if (isPowerSaveModeOn.get() == true && isPremium.get()!!) {
            mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm
        } else {
            mUtilsPrefsGmh.hzPrefMaxRefreshRate
        })
        lrrPref.set(mUtilsPrefsGmh.gmhPrefMinHzAdapt)
    }

    private fun canApplyFakeAdaptive(): Boolean {
        return isOfficialAdaptive && (currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS) && hasWriteSecureSetPerm
    }

    private fun isFakeAdaptive(): Boolean {
        return if (!isOfficialAdaptive){//Adaptive NOT Supported
            (currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS) && hasWriteSecureSetPerm
        }else{//Adaptive Supported devices
            canApplyFakeAdaptive && mUtilsPrefsGmh.gmhPrefMinHzAdapt < STANDARD_REFRESH_RATE_HZ
        }
    }

    private fun isSpayInstalled(): Boolean {
        return try {
            packageManager.getApplicationInfo("com.samsung.android.spay", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun setBrand(){
        when (mUtilsDeviceInfo.manufacturer) {
            "SAMSUNG" ->{
                isSamsung = true
                CacheSettings.isXiaomi = false
                CacheSettings.isOnePlus = false
            }
            "XIAOMI","POCO" ->{
                isSamsung = false
                CacheSettings.isXiaomi = true
                CacheSettings.isOnePlus = false
            }
            "ONEPLUS" ->{
                isSamsung = false
                CacheSettings.isXiaomi = false
                CacheSettings.isOnePlus = true
            }
        }
    }


    private fun notifyUserEffectOfAccessibility() = applicationScope.launch{
        if (hasWriteSecureSetPerm) {
            //Check if using Access requiring features
            val featuresOn = mutableListOf<String>()
            if (mUtilsPrefsGmh.gmhPrefForceLowestSoIsOn) {
                featuresOn.add("-${applicationContext.getString(R.string.force_hz_mod)}")
            }
            if (mUtilsPrefsGmh.gmhPrefPsmOnSo) {
                featuresOn.add("-${applicationContext.getString(R.string.auto_psm)}")
            }
            if (mUtilsPrefsGmh.gmhPrefDisableSyncIsOn) {
                featuresOn.add("-${applicationContext.getString(R.string.disable_sync_on_so)}")
            }
            if (mUtilsPrefsGmh.gmhPrefSensorsOff) {
                featuresOn.add("-${applicationContext.getString(R.string.auto_sensors_off_exp)}")
            }
            if (isFakeAdaptive.get() == true) {
                featuresOn.add("-${applicationContext.getString(R.string.adaptive)} mod")
            }

            if (featuresOn.size > 0) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Turn off the following Galaxy Max Hz features first:\n ${
                            featuresOn.joinToString(
                                ",\n"
                            )
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }
                allowAccessibility(
                    applicationContext,
                    GalaxyMaxHzAccess::class.java,
                    true
                )
            }
        } else {
            launch(Dispatchers.Main) {
                Toast.makeText(
                    applicationContext,
                    "Turning off accessibility disables some features of Galaxy Max Hz.",
                    Toast.LENGTH_LONG
                ).show()
            }
            mUtilsPrefsGmh.gmhPrefForceLowestSoIsOn = false
            mUtilsPrefsGmh.gmhPrefDisableSyncIsOn = false
        }
    }


    private fun isTurnOff5GOnPsm(): Boolean{
        return (Settings.Global.getString(mContentResolver, PSM_5G_MODE)?:"0").split(",")[0] == "1"
    }

/*    private val mDisplayManager by lazy {(applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)}

private val displayListener: DisplayListener = object:DisplayListener{
    override fun onDisplayChanged(displayId: Int){
        Log.d(TAG, "onDisplayChanged" )
        val newHz =  (applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(displayId).refreshRate.toInt()
        // if (prevHz.get() != newHz){
        //  mUtilsDisplayInfo.getRefreshRateInt().let {
        if (isHzNotifOn.get()!!) {
            updateNotifContent(newHz.toString())
        }
        if (overlayOn == true) {
            hzText.setTextColor(if (newHz <= 60.05) Color.RED else Color.GREEN)
            hzText.text = getString(R.string.hz_holder, newHz)
        }
        // }
    }

    override fun onDisplayAdded(displayId: Int) {
        Log.d(TAG, "onDisplayAdded" )
    }

    override fun onDisplayRemoved(displayId: Int) {
        Log.d(TAG, "onDisplayRemoved" )
    }
}*/


    /*val forName = Class::class.java.getDeclaredMethod("forName", String::class.java)
    val getDeclaredMethod = Class::class.java.getDeclaredMethod("getDeclaredMethod", String::class.java, arrayOf<Class<*>>()::class.java)
    val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
    val getRuntime = getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null) as Method
    val setHiddenApiExemptions = getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", arrayOf(arrayOf<String>()::class.java)) as Method
    val vmRuntime = getRuntime.invoke(null)
    setHiddenApiExemptions.invoke(vmRuntime, arrayOf("L"))*/

}