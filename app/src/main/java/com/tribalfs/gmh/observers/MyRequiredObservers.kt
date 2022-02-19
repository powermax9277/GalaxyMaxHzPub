package com.tribalfs.gmh.observers

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.GalaxyMaxHzAccess
import com.tribalfs.gmh.MyApplication
import com.tribalfs.gmh.MyApplication.Companion.appScopeIO
import com.tribalfs.gmh.R
import com.tribalfs.gmh.UtilAccessibilityService
import com.tribalfs.gmh.UtilAccessibilityService.checkAccessibility
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.ignoreRrmChange
import com.tribalfs.gmh.helpers.CacheSettings.turnOff5GOnPsm
import com.tribalfs.gmh.helpers.DozeUpdater.updateDozValues
import com.tribalfs.gmh.profiles.ProfilesObj
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.*

internal val refreshRateModeUri = Settings.Secure.getUriFor(REFRESH_RATE_MODE)
private val psm5gModeUri = Settings.Global.getUriFor(PSM_5G_MODE)
private val deviceIdleConstantsUri = Settings.Global.getUriFor(DEVICE_IDLE_CONSTANTS)
private val batterySaverConstantsUri = Settings.Global.getUriFor(BATTERY_SAVER_CONSTANTS)
private val sysuiQsTilesUri = Settings.Secure.getUriFor(SYSUI_QS_TILES)
private val currentResolutionUri = Settings.Global.getUriFor(DISPLAY_SIZE_FORCED)
private val devSettingsUri =  Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED)
private val accessibilityServiceUri =  Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)


@RequiresApi(Build.VERSION_CODES.M)
@ExperimentalCoroutinesApi
internal class MyRequiredObservers(h: Handler?, private val appCtx: Context) : ContentObserver(h) {
    private var dozeConfCtr = 0
    private var dozeConflictChecker: Job? = null

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        when (uri) {
            refreshRateModeUri -> {
                if (!ignoreRrmChange.compareAndSet(true, false)) {
                    appScopeIO.launch {
                        while (!ProfilesObj.isProfilesLoaded) { delay(200) }
                        UtilRefreshRateSt.instance(appCtx).updateModeBasedVariables()
                        UtilRefreshRateSt.instance(appCtx).updateAdaptiveModCachedParams()
                        GalaxyMaxHzAccess.gmhAccessInstance?.setupAdaptiveEnhancer()

                        CacheSettings.currentRefreshRateMode.get().let{
                            if (it != REFRESH_RATE_MODE_STANDARD) {
                                UtilsPrefsGmhSt.instance(appCtx).gmhPrefRefreshRateModePref = it
                            }
                        }
                    }
                }

            }

            psm5gModeUri -> {
                turnOff5GOnPsm = UtilsDeviceInfoSt.instance(appCtx).isTurnOff5GOnPsm()
            }


            deviceIdleConstantsUri -> {
                appScopeIO.launch {
                    if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefQuickDozeIsOn
                        && DozeUpdater.getDozeVal(UtilsPrefsGmhSt.instance(appCtx).gmhPrefGDozeModOpt)
                        != Settings.Global.getString(appCtx.contentResolver, DEVICE_IDLE_CONSTANTS)
                    ) {
                        dozeConfCtr += 1
                        appCtx.updateDozValues(
                            true,
                            UtilsPrefsGmhSt.instance(appCtx).gmhPrefGDozeModOpt
                        )

                        dozeConflictChecker?.cancel()
                        dozeConflictChecker = null
                        dozeConflictChecker = appScopeIO.launch(Dispatchers.IO) {
                            delay(4000)
                            if (dozeConfCtr > 5){
                                launch(Dispatchers.Main) {
                                    Toast.makeText(appCtx,"Another app is clashing ${MyApplication.applicationName} ${appCtx.getString(
                                        R.string.quick_doz_mod)}. Disable it now.", Toast.LENGTH_LONG).show()
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
                if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefQuickDozeIsOn) {
                    DozePSCChecker.check(
                        appCtx,
                        enable = true,
                        updateIfDisable = false
                    )
                }
            }


            sysuiQsTilesUri, devSettingsUri -> {
                if (UtilsPrefsGmhSt.instance(appCtx).gmhPrefSensorsOff) {
                    appScopeIO.launch {
                        if (UtilNotifBarSt.instance(appCtx).checkQsTileInPlace() != true) {
                            UtilsPrefsGmhSt.instance(appCtx).gmhPrefSensorsOff = false
                        }
                    }
                }
            }

            accessibilityServiceUri -> {
                if (!MyApplication.ignoreAccessibilityChange) {
                    appScopeIO.launch {
                        if (!UtilAccessibilityService.isAccessibilityEnabled(appCtx)) {
                            notifyAccessibilityNeed()
                            delay(1000)
                        }
                    }
                } else {
                    //reset to default
                    MyApplication.ignoreAccessibilityChange = false
                }

            }

            currentResolutionUri ->{
                appScopeIO.launch {
                    delay(1000)
                    UtilRefreshRateSt.instance(appCtx).requestListeningAllTiles()
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
            appCtx.contentResolver.registerContentObserver(
                it, false, this
            )
        }
    }

    private fun notifyAccessibilityNeed() = appScopeIO.launch{

        val featuresOn = UtilsPrefsGmhSt.instance(appCtx).getEnabledAccessibilityFeatures()

        featuresOn.size.let{
            if (it > 0) {

                launch(Dispatchers.Main) {
                    var toastRpt = it
                    //Will auto turn it back ON if have permission
                    val toastMsg = if (checkAccessibility(true, appCtx)) {
                        appCtx.resources.getQuantityString(
                            R.plurals.access_notif,
                            featuresOn.size,
                            "\n" + featuresOn.joinToString("" +
                                    "\n")
                        )
                    }else{
                        appCtx.getString(
                            R.string.no_access_notif,
                            "\n" + featuresOn.joinToString("" +
                                    "\n")
                        )
                    }
                    while(toastRpt >= 0) {
                        Toast.makeText(
                            appCtx,
                            toastMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                        delay(400)
                        toastRpt -= 1
                    }
                }

            } else  {
                UtilsPrefsGmhSt.instance(appCtx).gmhPrefForceLowestSoIsOn = false
                UtilsPrefsGmhSt.instance(appCtx).gmhPrefDisableSyncIsOn = false
                if (CacheSettings.isFakeAdaptive.get() == true && CacheSettings.isOfficialAdaptive) {
                    CacheSettings.lrrPref.set(SIXTY_HZ)
                    UtilsPrefsGmhSt.instance(appCtx).gmhPrefMinHzAdapt = SIXTY_HZ
                }
                UtilsPrefsGmhSt.instance(appCtx).gmhPrefSensorsOff = false

            }
        }
    }

}