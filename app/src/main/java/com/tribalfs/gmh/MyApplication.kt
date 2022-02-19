package com.tribalfs.gmh

//import com.tribalfs.gmh.helpers.CacheSettings.isS20Series
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.UtilAccessibilityService.checkAccessibility
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
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptiveValid
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveMode
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.isSpayInstalled
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.preventHigh
import com.tribalfs.gmh.helpers.CacheSettings.turnOff5GOnPsm
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.observers.MyBrightnessObserver
import com.tribalfs.gmh.observers.MyRequiredObservers
import com.tribalfs.gmh.profiles.ProfilesObj.isProfilesLoaded
import com.tribalfs.gmh.sharedprefs.LIC_TYPE_ADFREE
import com.tribalfs.gmh.sharedprefs.LIC_TYPE_TRIAL_ACTIVE
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.*
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraHttpSender
import org.acra.annotation.AcraLimiter
import org.acra.data.StringFormat
import org.acra.sender.HttpSender


@ExperimentalCoroutinesApi
@AcraCore(reportFormat= StringFormat.JSON)
@AcraHttpSender(uri = "https://script.google.com/macros/s/AKfycbybr-F6rCLr8fTk0jYvz_ohCOcNLwsSPCNxhYUlX-KtvLE9JT0/exec",
    httpMethod = HttpSender.Method.POST,
    basicAuthLogin = "",
    basicAuthPassword = "")
@AcraLimiter(stacktraceLimit = 1, failedReportLimit = 3)
class MyApplication : Application() {

    companion object{
        lateinit var applicationName: String
        @Volatile internal var ignoreAccessibilityChange = false
        internal val appScopeIO = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }


    private val mUtilsPrefsAct by lazy { UtilsPrefsAct(applicationContext) }
    private lateinit var mContentResolver: ContentResolver
    private lateinit var myBrightnessObserver: MyBrightnessObserver



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        //Log.d(TAG, "onCreate called")
        super.onCreate()
        applicationName = when (val stringId = applicationInfo.labelRes) {
            0 -> applicationInfo.nonLocalizedLabel.toString()
            else -> getString(stringId)
        }

        /***Required before checkAccessibility call**/
        currentRefreshRateMode.set(UtilRefreshRateSt.instance(applicationContext).getRefreshRateMode())
        isOfficialAdaptive = UtilRefreshRateSt.instance(applicationContext).isAdaptiveSupportedUpd()
        /***Required before checkAccessibility call**/

        checkAccessibility(null, applicationContext)

        mContentResolver = applicationContext.contentResolver

        val mHandler = Handler(Looper.getMainLooper())

        MyRequiredObservers(mHandler, applicationContext).start()

        myBrightnessObserver = MyBrightnessObserver(mHandler, applicationContext)

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

        appScopeIO.launch {
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


        appScopeIO.launch {

            Brand.set(UtilsDeviceInfoSt.instance(applicationContext).manufacturer)

            /*while (!isProfilesLoaded) {
                delay(250)
            }

            if (
                isSamsung
                && !UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSettingListDone
                && UtilsDeviceInfoSt.instance(applicationContext).oneUiVersion != null
                &&  UtilsDeviceInfoSt.instance(applicationContext).oneUiVersion!!  >= 4.1
            *//*   && highestHzForAllMode > REFRESH_RATE_MODE_STANDARD.toInt()*//*
            ) {
                UtilRefreshRateSt.instance(applicationContext).mSyncer.postSettingsList()
                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSettingListDone = true
            }*/
        }

    }


    private suspend fun cacheSettings() {

        isPowerSaveMode.set(
            if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefPsmIsOffCache)
                !UtilsPrefsGmhSt.instance(applicationContext).gmhPrefPsmIsOffCache
            else
                UtilsDeviceInfoSt.instance(applicationContext).isPowerSavingsMode()
        )

        turnOff5GOnPsm = UtilsDeviceInfoSt.instance(applicationContext).isTurnOff5GOnPsm()

        UtilPermSt.instance(applicationContext).apply{
            hasWriteSecureSetPerm = this.hasWriteSecurePerm()
            hasWriteSystemSetPerm = this.hasWriteSystemPerm()
        }

        isSpayInstalled = isSpayInstalled()

        keepModeOnPowerSaving = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefKmsOnPsm
        brightnessThreshold.set(
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefGAdaptBrightnessMin
            }else {
                (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefGAdaptBrightnessMin.toFloat() * 2.55f).toInt()
            }
        )
        currentBrightness.set(UtilsDeviceInfoSt.instance(applicationContext).getScreenBrightnessPercent())

        UtilsPrefsGmhSt.instance(applicationContext).hzPrefAdaptiveDelay.let {
            adaptiveDelayMillis = it
            adaptiveAccessTimeout = it * TIMEOUT_FACTOR.toLong()
        }

        isPremium.set((mUtilsPrefsAct.gmhPrefLicType == LIC_TYPE_ADFREE
                || mUtilsPrefsAct.gmhPrefLicType == LIC_TYPE_TRIAL_ACTIVE)
                && (mUtilsPrefsAct.gmhPrefSignature == PackageInfo.getSignatureString(applicationContext)))

        isProfilesLoaded = withContext(Dispatchers.IO) { UtilRefreshRateSt.instance(applicationContext).initProfiles() }

        preventHigh = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefPreventHigh

        hzNotifOn.set (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHzIsOn
                && UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHzNotifIsOn)

        UtilRefreshRateSt.instance(applicationContext).updateAdaptiveModCachedParams()
    }


    private fun isSpayInstalled(): Boolean {
        return try {
            packageManager.getApplicationInfo("com.samsung.android.spay", 0)
            true
        } catch (_: Exception) {
            false
        }
    }


 /* private fun notifyAccessibilityNeed() = appScopeIO.launch{

        val featuresOn = UtilsPrefsGmhSt.instance(applicationContext).getEnabledAccessibilityFeatures()

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
                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefForceLowestSoIsOn = false
                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefDisableSyncIsOn = false
                if (isFakeAdaptive.get() == true && isOfficialAdaptive) {
                    lrrPref.set(STANDARD_REFRESH_RATE_HZ)
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt = STANDARD_REFRESH_RATE_HZ
                }
                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorsOff = false

            }
        }
    }
*/



    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        ACRA.init(this)
    }
}