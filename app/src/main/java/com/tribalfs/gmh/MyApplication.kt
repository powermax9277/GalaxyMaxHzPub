package com.tribalfs.gmh

//import com.tribalfs.gmh.helpers.CacheSettings.isS20Series
import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
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
import android.provider.Settings.System.SCREEN_BRIGHTNESS
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.tribalfs.gmh.AccessibilityPermission.allowAccessibility
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.SETUP_ADAPTIVE
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.SETUP_NETWORK_CALLBACK
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.TIMEOUT_FACTOR
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveAccessTimeout
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveDelayMillis
import com.tribalfs.gmh.helpers.CacheSettings.brightnessThreshold
import com.tribalfs.gmh.helpers.CacheSettings.currentBrightness
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.ignoreRrmChange
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptiveValid
import com.tribalfs.gmh.helpers.CacheSettings.isHzNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.isNetSpeedRunning
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isOnePlus
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.isSamsung
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.isSpayInstalled
import com.tribalfs.gmh.helpers.CacheSettings.isXiaomi
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.lrrPref
import com.tribalfs.gmh.helpers.CacheSettings.preventHigh
import com.tribalfs.gmh.helpers.CacheSettings.turnOff5GOnPsm
import com.tribalfs.gmh.helpers.DozeUpdater.updateDozValues
import com.tribalfs.gmh.helpers.UtilNotificationBarSt.Companion.SYSUI_QS_TILES
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.BATTERY_SAVER_CONSTANTS
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.DEVICE_IDLE_CONSTANTS
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.DISPLAY_SIZE_FORCED
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.PSM_5G_MODE
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.SCREEN_BRIGHTNESS_FLOAT
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.STANDARD_REFRESH_RATE_HZ
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.profiles.ProfilesObj.isProfilesLoaded
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct.Companion.LIC_TYPE_ADFREE
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct.Companion.LIC_TYPE_TRIAL_ACTIVE
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt.Companion.NOT_USING
import com.tribalfs.gmh.tiles.QSTileResSw
import kotlinx.coroutines.*
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraHttpSender
import org.acra.data.StringFormat
import org.acra.sender.HttpSender

//TODO{reason: Comment out ACRA below}
@ExperimentalCoroutinesApi
@AcraCore(reportFormat= StringFormat.JSON)
@AcraHttpSender(uri = "https://script.google.com/macros/s/AKfycbybr-F6rCLr8fTk0jYvz_ohCOcNLwsSPCNxhYUlX-KtvLE9JT0/exec",
    httpMethod = HttpSender.Method.POST,
    basicAuthLogin = "",
    basicAuthPassword = "")

class MyApplication : Application() {

    companion object{
        lateinit var applicationName: String
        // private const val TAG = "MyApplication"
        @Volatile internal var ignoreAccessibilityChange = false
        internal val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    private val mUtilsRefreshRateSt by lazy {UtilsRefreshRateSt.instance(applicationContext)}
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

    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    inner class MyRequiredObservers(h: Handler?) : ContentObserver(h) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            when (uri) {
                refreshRateModeUri -> {
                    if (!ignoreRrmChange) {
                        applicationScope.launch {
                            while (!isProfilesLoaded) { delay(200) }
                            mUtilsRefreshRateSt.updateModeBasedVariables()
                            delay(250)
                            mUtilsRefreshRateSt.updateRefreshRateParams()
                            applicationContext.startService(
                                Intent(applicationContext, GalaxyMaxHzAccess::class.java).apply {
                                    putExtra(SETUP_ADAPTIVE, true)
                                }
                            )

                            mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefRefreshRateModePref = currentRefreshRateMode.get()//applies even on STANDARD_MODE
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
                            applicationContext.updateDozValues(
                                true,
                                mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefGDozeModOpt
                            )
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
                            if (UtilNotificationBarSt.instance(applicationContext).checkQsTileInPlace() != true) {
                                mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefSensorsOff = false
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
                                notifyAccessibilityNeed()
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val qsResoTileComponent =
                                ComponentName(applicationContext, QSTileResSw::class.java)
                            TileService.requestListeningState(
                                applicationContext,
                                qsResoTileComponent
                            )
                        }
                        mUtilsRefreshRateSt.requestListeningAllTiles()
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


        fun startBrightnessObserver(){
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

        fun stopBrightnessObserver(){
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                mContentResolver.registerContentObserver(
                    brightnessFloatUri, false, this
                )
            }else{
                mContentResolver.registerContentObserver(
                    brightnessUri, false, this)

            }
        }

        /* fun unObserve(){
             mContentResolver.unregisterContentObserver(this)
         }*/
    }



    //TODO{reason: Comment out method below}
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // Initialise ACRA
        ACRA.init(this)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        //Log.d(TAG, "onCreate called")
        super.onCreate()
        applicationName = when (val stringId = applicationInfo.labelRes) {
            0 -> applicationInfo.nonLocalizedLabel.toString()
            else -> getString(stringId)
        }
        checkAccessibility()

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
            //checkAccessibility()
            applicationContext.startService(
                Intent(applicationContext, GalaxyMaxHzAccess::class.java).apply {
                    putExtra(SETUP_ADAPTIVE, true)
                    putExtra(SETUP_NETWORK_CALLBACK, true)

                    // putExtra(SWITCH_AUTO_SENSORS, mUtilsPrefsGmh.gmhPrefSensorsOff)
                }
            )
        }

        HzServiceHelperStn.instance(applicationContext).startHertz(null, null, null)

        applicationScope.launch {

            setBrand()

            while (!isProfilesLoaded) {
                delay(250)
            }
            if ((isSamsung && mUtilsRefreshRateSt.mUtilsDeviceInfo.oneUiVersion == 4.0)
                /*   && highestHzForAllMode > REFRESH_RATE_MODE_STANDARD.toInt()*/
                && !mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefSettingListDone
            ) {
                mUtilsRefreshRateSt.mSyncer.postSettingsList()
                mUtilsRefreshRateSt.mUtilsPrefsGmh.gmhPrefSettingListDone = true
            }
        }
    }



    @SuppressLint("NewApi")
    private suspend fun cacheSettings() {
         val mUtilsPrefsGmh = mUtilsRefreshRateSt.mUtilsPrefsGmh

            isPowerSaveModeOn.set(
            if (mUtilsPrefsGmh.gmhPrefPsmIsOffCache)
                !mUtilsPrefsGmh.gmhPrefPsmIsOffCache
            else
                mUtilsRefreshRateSt.mUtilsDeviceInfo.isPowerSavingsModeOn
        )
        turnOff5GOnPsm = isTurnOff5GOnPsm()
        hasWriteSecureSetPerm = UtilsPermSt.instance(applicationContext).hasWriteSecurePerm()
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
        isPremium.set((mUtilsPrefsAct.gmhPrefLicType == LIC_TYPE_ADFREE || mUtilsPrefsAct.gmhPrefLicType == LIC_TYPE_TRIAL_ACTIVE)
                && (mUtilsPrefsAct.gmhPrefSignature == PackageInfo.getSignatureString(applicationContext)))


        isProfilesLoaded = withContext(Dispatchers.IO) { mUtilsRefreshRateSt.initProfiles() }

        applicationContext.startService(Intent(applicationContext, GalaxyMaxHzAccess::class.java).apply{ putExtra(SETUP_ADAPTIVE, true)})

        mUtilsRefreshRateSt.updateRefreshRateParams()

        preventHigh = mUtilsPrefsGmh.gmhPrefPreventHigh
        isHzNotifOn.set (mUtilsPrefsGmh.gmhPrefHzIsOn && mUtilsPrefsGmh.gmhPrefHzNotifIsOn)
        isNetSpeedRunning.set( mUtilsPrefsGmh.gmhPrefNetSpeedIsOn)

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
        when (mUtilsRefreshRateSt.mUtilsDeviceInfo.manufacturer) {
            "SAMSUNG" ->{
                isSamsung = true
                isXiaomi = false
                isOnePlus = false
            }
            "XIAOMI","POCO" ->{
                isSamsung = false
                isXiaomi = true
                isOnePlus = false
            }
            "ONEPLUS" ->{
                isSamsung = false
                isXiaomi = false
                isOnePlus = true
            }
        }
    }


    private fun checkAccessibility(): Boolean{
        return if (
            (isSpayInstalled == false || mUtilsRefreshRateSt.mUtilsPrefsGmh.hzPrefUsingSPay == NOT_USING)
            && hasWriteSecureSetPerm
        ) {
            allowAccessibility(
                applicationContext,
                GalaxyMaxHzAccess::class.java,
                true
            )
            true
        }else{
            false
        }
    }


    private fun notifyAccessibilityNeed() = applicationScope.launch{
        val mUtilsPrefsGmh = mUtilsRefreshRateSt.mUtilsPrefsGmh
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

        featuresOn.size.let{
            if (it > 0) {
                //Will auto turn it back ON if have permission
                launch(Dispatchers.Main) {
                    var toastRpt = it
                    val toastMsg = if (checkAccessibility()) {
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
                    while(toastRpt > 0) {
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
                mUtilsPrefsGmh.gmhPrefForceLowestSoIsOn = false
                mUtilsPrefsGmh.gmhPrefDisableSyncIsOn = false
                if (isFakeAdaptive.get() == true && isOfficialAdaptive) {
                    lrrPref.set(STANDARD_REFRESH_RATE_HZ)
                    mUtilsPrefsGmh.gmhPrefMinHzAdapt = STANDARD_REFRESH_RATE_HZ
                }
                mUtilsPrefsGmh.gmhPrefSensorsOff = false
            }
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