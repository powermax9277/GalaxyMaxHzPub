package com.tribalfs.gmh

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.app.NotificationManager.*
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
import android.provider.Settings.*
import android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.MeasureSpec
import android.view.animation.AnimationUtils.loadAnimation
import android.view.animation.AnticipateInterpolator
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.tribalfs.appupdater.AppUpdaterLite
import com.tribalfs.appupdater.UpdateDownloader.Companion.isDownloading
import com.tribalfs.appupdater.interfaces.OnUpdateCheckedCallback
import com.tribalfs.gmh.BuildConfig.APPLICATION_ID
import com.tribalfs.gmh.BuildConfig.VERSION_NAME
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.MyApplication.Companion.appScopeIO
import com.tribalfs.gmh.MyApplication.Companion.applicationName
import com.tribalfs.gmh.UtilAccessibilityService.allowAccessibility
import com.tribalfs.gmh.callbacks.LvlSbMsgCallback
import com.tribalfs.gmh.databinding.ActivityMainBinding
import com.tribalfs.gmh.dialogs.DialogActCode
import com.tribalfs.gmh.dialogs.InfoDialog
import com.tribalfs.gmh.dialogs.InfoDialog.Companion.CHANGE_RES_INFO
import com.tribalfs.gmh.dialogs.InfoDialog.Companion.QDM_INFO
import com.tribalfs.gmh.dialogs.InfoDialog.Companion.SENSORS_OFF_INFO
import com.tribalfs.gmh.dialogs.QSDialogs.getOverlaySettingIntent
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveDelayMillis
import com.tribalfs.gmh.helpers.CacheSettings.brightnessThreshold
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
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.limitTyping
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.minHzListForAdp
import com.tribalfs.gmh.helpers.CacheSettings.offScreenRefreshRate
import com.tribalfs.gmh.helpers.CacheSettings.preventHigh
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.sensorOnKey
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntAllMod
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntCurMod
import com.tribalfs.gmh.helpers.CacheSettings.turnOffAutoSensorsOff
import com.tribalfs.gmh.helpers.CacheSettings.updateSwitchDown
import com.tribalfs.gmh.helpers.DozeUpdater.getDozeVal
import com.tribalfs.gmh.helpers.DozeUpdater.mwInterval
import com.tribalfs.gmh.helpers.DozeUpdater.updateDozValues
import com.tribalfs.gmh.helpers.Superuser.grantSecPerm
import com.tribalfs.gmh.helpers.UtilCommon.closestValue
import com.tribalfs.gmh.helpers.UtilSettingsIntents.autoSyncSettingsIntent
import com.tribalfs.gmh.helpers.UtilSettingsIntents.dataUsageSettingsIntent
import com.tribalfs.gmh.helpers.UtilSettingsIntents.dataUsageSettingsIntentOP
import com.tribalfs.gmh.helpers.UtilSettingsIntents.deviceInfoActivity
import com.tribalfs.gmh.helpers.UtilSettingsIntents.powerSavingModeSettingsIntent
import com.tribalfs.gmh.hertz.HzGravity
import com.tribalfs.gmh.hertz.HzNotifGlobal.CHANNEL_ID_HZ
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.netspeed.CHANNEL_ID_NET_SPEED
import com.tribalfs.gmh.netspeed.NetSpeedService.Companion.netSpeedService
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.profiles.*
import com.tribalfs.gmh.profiles.ProfilesObj.isProfilesLoaded
import com.tribalfs.gmh.sharedprefs.*
import com.tribalfs.gmh.viewmodels.MyViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

internal const val GMH_WEB_APP ="https://script.google.com/macros/s/AKfycbzlRKh4-YXyXLufXZfDqAs1xJEJK7BF8zmhEDGDpbP1luu97trI/exec"
internal const val ACTION_HIDE_MAIN_ACTIVITY = "$APPLICATION_ID.ACTION_HIDE"
internal const val ACTION_CLOSE_MAIN_ACTIVITY = "$APPLICATION_ID.ACTION_CLOSE"
internal const val KEY_JSON_LIC_TYPE = "0x11"

private const val REQUEST_LATEST_UPDATE = 0x5
private const val KEY_JSON_PAYPAL_BUY_URL = "0x12"
private const val KEY_JSON_HELP_URL = "0x24"

//TODO keep standard mode on reboot, touch and hold detection

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivity : AppCompatActivity()/*, OnUserEarnedRewardListener, MyClickHandler*/, CoroutineScope {

    private val viewModel: MyViewModel by viewModels()
    private lateinit var mBinding: ActivityMainBinding
    private val mUtilsPrefsAct by lazy{ UtilsPrefsAct(applicationContext)}
    private val mUtilsRefreshRate by lazy{UtilRefreshRateSt.instance(applicationContext)}
    private val mNetspeedService by lazy {NetSpeedServiceHelperStn.instance(applicationContext)}
    private val hzOverlaySizes = listOf(10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40)
    private val hzAdaptiveDelays = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    // private var mList: Menu? = null
    private var ignoreUnblockHzNotifState = true
    private var ignoreUnblockNetSpeedNotifState = true
    private lateinit var thumbView : View
    private var buyLink: String? = null

    private val masterJob: Job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + masterJob

    private val mReceiver = object: BroadcastReceiver() {
        @SuppressLint("InlinedApi")
        @RequiresApi(VERSION_CODES.M)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action){
                ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED -> {
                    when (intent.getStringExtra(EXTRA_NOTIFICATION_CHANNEL_ID)) {
                        CHANNEL_ID_HZ -> {
                            if (SDK_INT >= VERSION_CODES.M) {
                                if (intent.getBooleanExtra(EXTRA_BLOCKED_STATE, false)) {
                                    mBinding.chNotifHz.isChecked = false

                                    showHertz(
                                        UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHzIsOn,
                                        null,
                                        false
                                    )

                                } else {
                                    if (!ignoreUnblockHzNotifState) {
                                        mBinding.chNotifHz.isChecked = true
                                        showHertz(
                                            UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHzIsOn,
                                            null,
                                            true
                                        )
                                        ignoreUnblockHzNotifState = true
                                    }
                                }
                            }
                        }

                        CHANNEL_ID_NET_SPEED -> {
                            if (intent.getBooleanExtra(EXTRA_BLOCKED_STATE, false)) {
                                mBinding.swNetspeed.isChecked = false
                                mNetspeedService.runNetSpeed(false)
                            } else {
                                if (!ignoreUnblockNetSpeedNotifState) {
                                    mBinding.swNetspeed.isChecked = true
                                    mNetspeedService.runNetSpeed(true)
                                    ignoreUnblockNetSpeedNotifState = true
                                }
                            }
                        }
                    }
                }

                ACTION_HIDE_MAIN_ACTIVITY -> {
                    //Force app to go background on Change Resolution to prevent crash (pre-api 23 issue)
                    val startMain = Intent(Intent.ACTION_MAIN)
                    startMain.addCategory(Intent.CATEGORY_HOME)
                    startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(startMain)
                }

                ACTION_CLOSE_MAIN_ACTIVITY -> finish()
            }
        }
    }


    private val gmhPrefChngListener = OnSharedPreferenceChangeListener { _, key ->
        val mUtilsPrefsGmh = UtilsPrefsGmhSt.instance(applicationContext)
        when (key){
            KEEP_RRM -> {
                (mUtilsPrefsGmh.gmhPrefKmsOnPsm).let {
                    if (mBinding.swKeepMode.isChecked != it) {
                        mBinding.swKeepMode.isChecked = it
                    }
                }
            }
            IS_HZ_ON -> {
                (mUtilsPrefsGmh.gmhPrefHzIsOn).let {
                    if (mBinding.swHzOn.isChecked != it) {
                        mBinding.swHzOn.isChecked = it
                    }
                }
            }
            //Trigger with ChangeMaxHz function
            PREF_MAX_REFRESH_RATE -> {
                //Only max refresh rate changes
                mUtilsPrefsGmh.hzPrefMaxRefreshRate.let{mrr ->
                    if (mBinding.sbPeakHz.progress != mrr) {
                        mBinding.sbPeakHz.progress = mrr
                    }
                }
            }

            //Trigger with ChangeMaxHz function
            MIN_HZ_ADAPT -> {
                //Only max refresh rate changes
                mUtilsPrefsGmh.gmhPrefMinHzAdapt.let{mnrr ->
                    if (mBinding.sbMinHzAdapt.progress != mnrr) {
                        val regMinHz = UtilsDeviceInfoSt.instance(applicationContext).regularMinHz
                        if (mnrr <  regMinHz && isPremium.get() != true){
                            mBinding.sbMinHzAdapt.progress = regMinHz
                            mBinding.minHzAdaptive = regMinHz
                        }else {
                            mBinding.sbMinHzAdapt.progress = mnrr
                            mBinding.minHzAdaptive = mnrr
                        }
                    }
                }
            }

            PREF_MAX_REFRESH_RATE_PSM -> {
                //Only max refresh rate changes
                mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm.let{mrrPsm ->
                    if (mBinding.sbPeakHzPsm.progress != mrrPsm) {
                        mBinding.sbPeakHzPsm.progress = mrrPsm
                    }
                }
            }
            QUICK_DOZE -> {
                mUtilsPrefsGmh.gmhPrefQuickDozeIsOn.let {qdOn ->
                    if (mBinding.swScreenOffDoze.isChecked != qdOn)
                        mBinding.swScreenOffDoze.isChecked = qdOn
                }
            }

            HZ_OVERLAY_ON -> {
                mUtilsPrefsGmh.gmhPrefHzOverlayIsOn.let {
                    mBinding.hideHzOverlaySettings = !it
                    mBinding.chOverlayHz.isChecked = it
                }
            }

            APPLY_SENSORS_OFF ->{
                mUtilsPrefsGmh.gmhPrefSensorsOff.let {isAutoSensorsOffOn ->
                    if (mBinding.swAutoSensorsOff.isChecked != isAutoSensorsOffOn) {
                        mBinding.swAutoSensorsOff.isChecked = isAutoSensorsOffOn
                    }
                }
            }

            FORCE_LOWEST_HZ_SO ->{
                mUtilsPrefsGmh.gmhPrefForceLowestSoIsOn.let{isForceLowestOn ->
                    if (mBinding.swSoForceLowestHz.isChecked != isForceLowestOn) {
                        mBinding.swSoForceLowestHz.isChecked = isForceLowestOn
                    }
                }
            }

            DISABLE_SYNC ->{
                mUtilsPrefsGmh.gmhPrefDisableSyncIsOn.let{isAutoDisableSync ->
                    if (mBinding.swAutoOffSync.isChecked != isAutoDisableSync) {
                        mBinding.swAutoOffSync.isChecked = isAutoDisableSync
                    }
                }
            }

            //For tasker
            DEEP_DOZ_OPT -> {
                if (mBinding.sbMwInterval.progress != mUtilsPrefsGmh.gmhPrefGDozeModOpt) {
                    mBinding.sbMwInterval.progress = mUtilsPrefsGmh.gmhPrefGDozeModOpt
                }
            }

        }
    }


    private val mPropertyChangeCallback: OnPropertyChangedCallback = object : OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable, propertyId: Int) {
            when (sender) {
                currentRefreshRateMode -> { //triggered by MyContentObserver -> updateCacheSettings()
                    if (SDK_INT >= VERSION_CODES.M) {
                        currentRefreshRateMode.get().let {
                            if (!isOfficialAdaptive && !isPremium.get()!! && it == REFRESH_RATE_MODE_SEAMLESS) {
                                if (highestHzForAllMode > SIXTY_HZ) {
                                    mUtilsRefreshRate.setRefreshRateMode(REFRESH_RATE_MODE_ALWAYS)
                                    return
                                } else {
                                    if (ProfilesObj.loadComplete) {
                                        mUtilsRefreshRate.setRefreshRateMode(
                                            REFRESH_RATE_MODE_STANDARD
                                        )
                                        return
                                    }
                                }
                            }
                            mBinding.refreshRateMode = it
                            updateRefreshRateLabels()
                            updateMinHzSbMinMax()
                            updateAdaptMinHzSbMinMax()
                            updateMaxHzSbMinMax()
                            updatePsmMaxHzSbMinMax()
                        }
                    }
                }

                isPowerSaveMode -> {
                    mBinding.powerSavingIsOn = isPowerSaveMode.get()
                }
            }
        }
    }

    @RequiresApi(VERSION_CODES.M)
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (UtilPermSt.instance(applicationContext).hasOverlayPerm()) {
                HzServiceHelperStn.instance(applicationContext).switchOverlay(true)
                mBinding.hideHzOverlaySettings = false
                mBinding.chOverlayHz.isChecked = true
            } else {
                mBinding.chOverlayHz.isChecked = false
                showSbMsg(R.string.sot_no_perm, Snackbar.LENGTH_SHORT, null, null)
                mBinding.hideHzOverlaySettings = true
            }
        }
    }


    private fun showAppearOnTopRequest(){
        if (SDK_INT >= VERSION_CODES.M) {
            startForResult.launch(getOverlaySettingIntent(this@MainActivity))
        }
    }

    @RequiresApi(VERSION_CODES.M)
    fun onClickView(v: View) {
        //val mUtilsPrefsGmh = UtilsPrefsGmhSt.instance(applicationContext)
        when(v.id){
            mBinding.tvBattOptimSettings.id -> {
                startActivity(Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }

            mBinding.chBytes.id -> {
                if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedUnit != BYTE_PER_SEC) {
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedUnit = BYTE_PER_SEC
                    mNetspeedService.updateSpeedUnit()
                }
            }

            mBinding.chBits.id -> {
                if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedUnit != BIT_PER_SEC) {
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedUnit = BIT_PER_SEC
                    mNetspeedService.updateSpeedUnit()
                }
            }

            mBinding.chDownStream.id -> {
                if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedToShow != DOWNLOAD_SPEED) {
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedToShow = DOWNLOAD_SPEED
                    mNetspeedService.updateStreamType()
                }
            }

            mBinding.chUpStream.id -> {
                if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedToShow != UPLOAD_SPEED) {
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedToShow = UPLOAD_SPEED
                    mNetspeedService.updateStreamType()
                }
            }

            mBinding.chCombinedStream.id -> {
                if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedToShow != TOTAL_SPEED) {
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedToShow = TOTAL_SPEED
                    mNetspeedService.updateStreamType()
                }
            }

            mBinding.swNetspeed.id -> {
                switchNetSpeed((v as Switch).isChecked)
            }

            mBinding.tvDataUsage.id -> {
                try {
                    val i = dataUsageSettingsIntent
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(i)
                }catch(_: Exception){
                    if (isOnePlus){
                        val i = dataUsageSettingsIntentOP
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(i)
                    }
                }
            }

            /* mBinding.chLeftHz.id, mBinding.chRightHz.id, mBinding.chCentHz.id -> {
                 mUtilsPrefsGmh.gmhPrefChipIdLrc = v.id
                 HzServiceHelperStn.instance(applicationContext).updateHzGravity(
                     chIdsToGravity(mBinding.cgTopBottom.checkedChipId, v.id)
                 )
             }*/

            mBinding.chTopHz.id, mBinding.chBottomHz.id, mBinding.chCentHz.id -> {
                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefChipIdTb = v.id
                HzServiceHelperStn.instance(applicationContext).updateHzGravity(
                    /*chIdsToGravity(v.id, mBinding.cgLeftCentRightHz.checkedChipId)*/
                    when (v.id){
                        mBinding.chBottomHz.id -> HzGravity.BOTTOM_LEFT
                        mBinding.chCentHz.id -> HzGravity.CENTER_LEFT
                        else -> HzGravity.TOP_LEFT
                    }
                )
            }

            mBinding.swSoForceLowestHz.id -> {
                mBinding.swSoForceLowestHz.isChecked.let { checked ->
                    if (!checked || !checkAccessibilityPerm(true)) {
                        UtilsPrefsGmhSt.instance(applicationContext).gmhPrefForceLowestSoIsOn = false
                        mBinding.swSoForceLowestHz.isChecked = false
                    } else {
                        UtilsPrefsGmhSt.instance(applicationContext).gmhPrefForceLowestSoIsOn = true
                    }
                }
            }


            mBinding.swAutoSensorsOff.id ->{
                mBinding.swAutoSensorsOff.isChecked.let { checked ->
                    if (checked){
                        if (!checkAccessibilityPerm(true)){
                            mBinding.swAutoSensorsOff.isChecked = false
                            return
                        }

                        when (UtilNotifBarSt.instance(applicationContext).checkQsTileInPlace()){
                            true -> {
                                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorsOff = checked
                                return
                            }

                            //already put in qs but not in place
                            false -> {
                                mBinding.swAutoSensorsOff.isChecked = false
                                showSbMsg(
                                    R.string.so_location_info,
                                    Snackbar.LENGTH_INDEFINITE,
                                    android.R.string.ok
                                ) {UtilNotifBarSt.instance(applicationContext).expandNotificationBar()}
                                return
                            }

                            //not yet enabled
                            null -> {
                                mBinding.swAutoSensorsOff.isChecked = false
                                val isDevOptEnabled = Global.getString(
                                    applicationContext.contentResolver,
                                    DEVELOPMENT_SETTINGS_ENABLED
                                ) == "1"
                                showSbMsg(
                                    getString(R.string.sensor_off_setup_info) +
                                            if (!isDevOptEnabled)
                                                getString(R.string.dev_opt_setup_info)
                                            else "",
                                    Snackbar.LENGTH_INDEFINITE,
                                    android.R.string.ok
                                ) {
                                    if (isDevOptEnabled) {
                                        startActivity(Intent(ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                                    } else {
                                        try {
                                            startActivity(deviceInfoActivity)
                                        } catch (_: Exception) {
                                            startActivity(Intent(ACTION_SETTINGS))
                                        }
                                    }
                                }
                                return
                            }
                        }

                    } else {
                        turnOffAutoSensorsOff = false
                        UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorsOff = checked /*&&
                                (CheckBlacklistApiSt.instance(applicationContext).isAllowed()
                                        || CheckBlacklistApiSt.instance(applicationContext).setAllowed())*/
                        if (!UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorOnKey.isNullOrEmpty()) {
                            sensorOnKey = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorOnKey
                        }

                    }
                }
            }

            mBinding.swSoPsm.id -> {
                if ((v as Switch).isChecked && !checkAccessibilityPerm(hasWriteSecureSetPerm)) {
                    mBinding.swSoPsm.isChecked = false
                } else {
                    mBinding.swSoPsm.isChecked.let {isChecked ->
                        UtilsPrefsGmhSt.instance(applicationContext).gmhPrefPsmOnSo = isChecked
                    }
                }
            }

            mBinding.tvSoPsmOptions.id -> {
                val i = powerSavingModeSettingsIntent
                startActivity(i)
            }

            mBinding.swScreenOffDoze.id -> {
                mBinding.swScreenOffDoze.isChecked.let {
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefQuickDozeIsOn = it
                    /*Keep this*/
                    mBinding.dozeIsOn = it
                    applicationContext.updateDozValues(it, UtilsPrefsGmhSt.instance(applicationContext).gmhPrefGDozeModOpt)
                    DozePSCChecker.check(applicationContext, it, true)
                }
                return
            }


            mBinding.chOverlayHz.id -> {
                (v as Chip)
                if (gmhAccessInstance != null || UtilPermSt.instance(applicationContext).hasOverlayPerm()) {
                    HzServiceHelperStn.instance(applicationContext).switchOverlay(v.isChecked)
                    mBinding.hideHzOverlaySettings = !v.isChecked

                } else {

                    if (v.isChecked) {
                        mBinding.chOverlayHz.isChecked = false
                        showSbMsg(
                            getString(R.string.aot_perm_inf),
                            Snackbar.LENGTH_INDEFINITE,
                            android.R.string.ok
                        ) {
                            showAppearOnTopRequest()
                        }
                    }
                }
                return
            }

            mBinding.chNotifHz.id -> {
                showHertz(
                    mBinding.swHzOn.isChecked,
                    mBinding.chOverlayHz.isChecked,
                    (v as Chip).isChecked
                )
                return
            }

            mBinding.swHzOn.id -> {
                //    Log.d(TAG, "swHzOn setOnClickListener called")
                showHertz(
                    (v as Switch).isChecked,
                    mBinding.chOverlayHz.isChecked,
                    mBinding.chNotifHz.isChecked
                )
                return
            }


            mBinding.chStandard.id ->{
                mUtilsRefreshRate.setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
            }

            mBinding.swKeepMode.id -> {
                (v as Switch).isChecked.let { isOn ->
                    if (isOn && (!checkAccessibilityPerm(hasWriteSecureSetPerm) || !confirmHasPipPermission())) {
                        keepModeOnPowerSaving = false
                        mBinding.swKeepMode.isChecked = false
                        return
                    }

                    keepModeOnPowerSaving = isOn
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefPsmIsOffCache = (isPowerSaveMode.get() != true)
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefKmsOnPsm = isOn

                    PsmChangeHandler.instance(applicationContext).handle()
                    return
                }
            }

            mBinding.chHigh.id -> {
                if ((v as Chip).isChecked == (currentRefreshRateMode.get() == REFRESH_RATE_MODE_ALWAYS)) {
                    return
                }

                if (isPowerSaveMode.get() == true && !confirmHasPipPermission()){
                    mBinding.chStandard.isChecked = true
                    return
                }

                if (!mUtilsRefreshRate.tryThisRrm(REFRESH_RATE_MODE_ALWAYS, null)){
                    v.isChecked = false
                    when (currentRefreshRateMode.get()){
                        REFRESH_RATE_MODE_SEAMLESS ->{
                            mBinding.chAdaptive.isChecked = true

                        }
                        REFRESH_RATE_MODE_STANDARD ->{
                            mBinding.chStandard.isChecked = true
                            InfoDialog.newInstance(CHANGE_RES_INFO).show(supportFragmentManager, null)
                        }
                    }
                    return
                }

                PsmChangeHandler.instance(applicationContext).startPipActivityIfS()

                return
            }

            mBinding.chAdaptive.id -> {
                if ((v as Chip).isChecked == (currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS)) {
                    return
                }

                if(!isOfficialAdaptive && isPremium.get() != true){
                    Toast.makeText(applicationContext,
                        getString(R.string.is_prem_ft,
                            "${UtilsDeviceInfoSt.instance(applicationContext).deviceModelVariant} ${getString(R.string.adp_mode)}"),
                        Toast.LENGTH_LONG).show()
                }

                if (isPowerSaveMode.get() == true && !confirmHasPipPermission()){
                    mBinding.chStandard.isChecked = true
                    return
                }

                if (!isOfficialAdaptive && !checkAccessibilityPerm(hasWriteSecureSetPerm)) {
                    mBinding.chAdaptive.isChecked = false
                    when (currentRefreshRateMode.get()){
                        REFRESH_RATE_MODE_ALWAYS ->{
                            mBinding.chHigh.isChecked = true
                        }
                        REFRESH_RATE_MODE_STANDARD ->{
                            mBinding.chStandard.isChecked = true
                        }
                    }
                    return
                }


                if (!mUtilsRefreshRate.tryThisRrm(REFRESH_RATE_MODE_SEAMLESS, null)){
                    v.isChecked = false
                    when (currentRefreshRateMode.get()){
                        REFRESH_RATE_MODE_ALWAYS ->{
                            mBinding.chHigh.isChecked = true

                        }
                        REFRESH_RATE_MODE_STANDARD ->{
                            mBinding.chStandard.isChecked = true
                            InfoDialog.newInstance(CHANGE_RES_INFO).show(supportFragmentManager, null)
                        }
                    }
                    return
                }
                PsmChangeHandler.instance(applicationContext).startPipActivityIfS()
                return
            }


            mBinding.tvAutoSensorsOffInfo.id -> {
                if (v.id == mBinding.tvAutoSensorsOffInfo.id) {
                    InfoDialog.newInstance(SENSORS_OFF_INFO)
                        .show(supportFragmentManager, null)
                    return
                }
            }


            mBinding.tvUseQsTileInf.id, mBinding.tvRrmAdbInf.id,
            mBinding.tvKeepModeAdbInf.id, mBinding.tvSoPsmAdbInf.id,
            mBinding.screenOffDozeInfo.id -> {
                if (hasWriteSecureSetPerm) {
                    if (v.id == mBinding.screenOffDozeInfo.id) {
                        InfoDialog.newInstance(QDM_INFO).show(supportFragmentManager, null)
                    } else{
                        if (mBinding.hasWssPerm == false) mBinding.hasWssPerm = true
                    }
                } else {
                    checkWssPermission(true)
                }
            }

            mBinding.swAutoOffSync.id ->{
                (v as Switch).isChecked.let { checked ->
                    if (!checkAccessibilityPerm(true)){
                        UtilsPrefsGmhSt.instance(applicationContext).gmhPrefDisableSyncIsOn = false
                        v.isChecked = false
                        return
                    }
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefDisableSyncIsOn = checked
                }
            }

            mBinding.tvAutoSyncSettings.id ->{
                val i = autoSyncSettingsIntent
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(i)
            }

            mBinding.btnBuyAdfree.id -> openBuyAdFreeLink()

            mBinding.btnSyncLicense.id -> syncLicense(false, trial = false)

            mBinding.swPreventHigh.id -> {
                (v as Switch).isChecked.let { checked ->
                    preventHigh = checked
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefPreventHigh = checked
                }
            }

            mBinding.swLimitTyping.id -> {
                (v as Switch).isChecked.let { checked ->
                    limitTyping = checked
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefLimitTyping = checked
                }
            }

        }
    }

    private fun confirmHasPipPermission(): Boolean{
        if ( SDK_INT >= VERSION_CODES.S && !UtilPermSt.instance(applicationContext).hasPipPermission()
        ) {
            mBinding.chStandard.isChecked = true
            showSbMsg("Picture-in-Picture in $applicationName info settings needs to be enabled.",
                Snackbar.LENGTH_INDEFINITE, android.R.string.ok){
                val i = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri.parse("package:$packageName")
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
            }
            return false
        }else{
            return true
        }
    }

    private fun checkWssPermission(showDialog: Boolean){
        if (UtilPermSt.instance(applicationContext).hasWriteSecurePerm()) {
            hasWriteSecureSetPerm = true
            if (mBinding.hasWssPerm == false) mBinding.hasWssPerm = true
            Toast.makeText(
                this@MainActivity,
                "WRITE_SECURE_SETTINGS permission successfully granted.",
                Toast.LENGTH_SHORT
            ).show()
        }else{
            if (showDialog) InfoDialog.newInstance(InfoDialog.ADB_PERM_INFO).show(supportFragmentManager, null)
        }
    }

    @RequiresApi(VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (SDK_INT < VERSION_CODES.M) {
            Toast.makeText(applicationContext, "$applicationName is not compatible to this device android version", Toast.LENGTH_LONG).show()
            appScopeIO.launch {
                delay(2500)
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(1)
            }
        }
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener{splashScreenView ->
            splashScreenView.view.background.alpha = 15
            splashScreenView.iconView.startAnimation(loadAnimation(this@MainActivity, R.anim.pulse))
            val animator: ObjectAnimator = ObjectAnimator.ofFloat(
                splashScreenView.iconView,
                View.SCALE_Y,
                1.0f, 0f
            ).apply{
                interpolator = AnticipateInterpolator()
                duration = 1500
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animator: Animator) {
                        splashScreenView.remove()
                    }
                })
            }
            launch(Dispatchers.Main) {
                while (!isProfilesLoaded) {
                    delay(400)
                }
                animator.start()
            }
        }


        inflateViews()
        updateDisplayId()
        updateViewModelSig()
        updateWssPerm()
        setupActionBar()
        registerSharedPrefListener()

        setupKeepMod()
        initNeedSpeed()
        setupHzMon()
        setupForceHzOnSo()
        setupScreenOffPsm() //Free Feature
        setupSizeSeekBar()
        setupAdaptDelaySeekBar()
        setupBrightnessSeekBar()

        viewModel.isValidAdFree.observe(this) { adFree ->
            isPremium.set(adFree)
            updateNetSpeed(adFree)
            mBinding.premium = adFree
            initDozeMod(adFree)
            initDisableAutoSync(adFree)
            setupScreenOffSensorsOff(adFree) //New
        }

        viewModel.setAdFreeCode(mUtilsPrefsAct.gmhPrefLicType)

        updateDynamicViews()
        mBinding.powerSavingIsOn = isPowerSaveMode.get()
        currentRefreshRateMode.addOnPropertyChangedCallback(mPropertyChangeCallback)
        isPowerSaveMode.addOnPropertyChangedCallback(mPropertyChangeCallback)

        setupBroadcastReceiver()
        checkIfAllowedBackgroundTask()

        if (savedInstanceState == null){
            oneTimeAutoChecks()
        }


        if (/*!isOfficialAdaptive && */isPremium.get() == true){
            (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefPreventHigh).let {
                preventHigh = it
                mBinding.swPreventHigh.isChecked = it
            }
            (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefLimitTyping).let {
                limitTyping = it
                mBinding.swLimitTyping.isChecked = it
            }
        }


        launch{
            getHelpUrl()

        }
    }


    private fun updateDynamicViews(){
        /*Wait for loadDone to Update Dynamic Views*/
        if (SDK_INT >= VERSION_CODES.M) {
            launch {
                while(!isProfilesLoaded) delay(300)
                mBinding.hasMotionSmoothness = highestHzForAllMode > SIXTY_HZ
                mBinding.isMultiRes = isMultiResolution
                mBinding.adaptiveSupported = isOfficialAdaptive
                mBinding.refreshRateMode = currentRefreshRateMode.get()
                updateRefreshRateLabels()
                setupMinHzSeekBar()
                updateMinHzSbMinMax()
                setupAdaptMinHzSeekBar()
                updateAdaptMinHzSbMinMax()
                setupMaxHzSeekBar()
                updateMaxHzSbMinMax()
                setupPsmMaxHzSeekBar()
                updatePsmMaxHzSbMinMax()
                setupResoSwitcherFilter()
            }
        }
    }


    private fun checkIfAllowedBackgroundTask(){
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (SDK_INT >= VERSION_CODES.P) {
            if (activityManager.isBackgroundRestricted){
                showSbMsg(
                    getString(R.string.allow_bg_act),
                    Snackbar.LENGTH_INDEFINITE,
                    android.R.string.ok
                ) {

                    startActivity(
                        Intent(
                            ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.fromParts("package", packageName, null)
                        )
                    )
                }
            }
        }
    }



    private fun setupMenuVisibility(menu: Menu) {
        viewModel.hideBuyActMenu.observe(this){
            menu.apply {
                findItem(R.id.menuBuy).isVisible = it != LIC_TYPE_ADFREE/ 2
                findItem(R.id.menuAct).isVisible = (it != LIC_TYPE_ADFREE/ 2 && it != LIC_TYPE_TRIAL_ACTIVE/ 2)
                findItem(R.id.menuAc).isVisible = it != LIC_TYPE_ADFREE/ 2
                findItem(R.id.menuNs).isVisible = it >= LIC_TYPE_ADFREE/ 2
                findItem(R.id.menuPrem).isVisible = it < LIC_TYPE_ADFREE/ 2
                findItem(R.id.menuNs).isChecked = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefShowNetSpeedTool
                /* findItem(R.id.menuUsingSpay).isVisible = isSpayInstalled == true
                 findItem(R.id.menuUsingSpay).isChecked = UtilsPrefsGmhSt.instance(applicationContext).hzPrefSPayUsage == USING*/
            }
        }
    }


    @RequiresApi(VERSION_CODES.M)
    private fun initDisableAutoSync(adFree: Boolean?){
        //Disable if not Ad-free
        if ((adFree?:viewModel.isValidAdFree.value != true) || gmhAccessInstance == null){
            UtilsPrefsGmhSt.instance(applicationContext).gmhPrefDisableSyncIsOn = false
        }else{
            mBinding.swAutoOffSync.isChecked = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefDisableSyncIsOn
        }
    }

    private fun initDozeMod(adFree: Boolean?){
        val mUtilsPrefsGmh = UtilsPrefsGmhSt.instance(applicationContext)
        //Disable if not Ad-free
        if (adFree?:viewModel.isValidAdFree.value != true) {
            if (hasWriteSecureSetPerm){
                applicationContext.updateDozValues(false,null)
                mUtilsPrefsGmh.gmhPrefQuickDozeIsOn = false
            }
        }else{
            setupDozeSeekBar()
            if (hasWriteSecureSetPerm) {
                //Update if Dozeval is not consistent with preference
                if (mUtilsPrefsGmh.gmhPrefQuickDozeIsOn && getDozeVal(mUtilsPrefsGmh.gmhPrefGDozeModOpt)
                    != Global.getString(applicationContext.contentResolver, DEVICE_IDLE_CONSTANTS)){
                    applicationContext.updateDozValues(true, mUtilsPrefsGmh.gmhPrefGDozeModOpt)
                }
            }else{
                //Check if Dozeval still persist - reflect it in doze views accordingly
                Global.getString(applicationContext.contentResolver, DEVICE_IDLE_CONSTANTS).let{
                    for (i in mwInterval) {
                        if (getDozeVal(i) == it){
                            mUtilsPrefsGmh.gmhPrefGDozeModOpt = i
                            return@let
                        }
                    }
                }
            }
        }
        //Update databinding
        mBinding.dozeIsOn = mUtilsPrefsGmh.gmhPrefQuickDozeIsOn
        //mBinding.dozeOptSelected =  mUtilsPrefsGmh.gmhPrefGDozeModOpt

        //initial value
        mBinding.sbMwInterval.progress = mUtilsPrefsGmh.gmhPrefGDozeModOpt
    }

    private fun setupDozeSeekBar() {
        val mListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mwInterval.closestValue(progress)?.let{
                    seekBar.progress = it
                    seekBar.thumb = getThumb(it)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekBar.progress.let {
                    seekBar.thumb = getThumb(it)
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefGDozeModOpt = it
                    applicationContext.updateDozValues(UtilsPrefsGmhSt.instance(applicationContext).gmhPrefQuickDozeIsOn, it)
                }
            }
        }
        mBinding.sbMwInterval.setOnSeekBarChangeListener(mListener)
        if (SDK_INT >= VERSION_CODES.O) {
            mBinding.sbMwInterval.min = mwInterval.minOrNull()!!
        }
        mBinding.sbMwInterval.max = mwInterval.maxOrNull()!!

    }


    @RequiresApi(VERSION_CODES.M)
    private fun switchNetSpeed(bool: Boolean) {

        if ((netSpeedService != null) == bool) return

        if (bool) {
            if (isNetSpeedNotificationEnabled()) {
                mNetspeedService.runNetSpeed(bool)
            } else {
                ignoreUnblockNetSpeedNotifState = false
                mBinding.swNetspeed.isChecked = false
                val settingsIntent: Intent =
                    if (SDK_INT >= VERSION_CODES.O) {
                        Intent(ACTION_APP_NOTIFICATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(EXTRA_APP_PACKAGE, packageName)
                    } else {
                        Intent()
                            .setAction("android.settings.APP_NOTIFICATION_SETTINGS")
                            .putExtra("app_package", packageName)
                            .putExtra("app_uid", applicationInfo.uid)
                    }
                startActivity(settingsIntent)
            }
        } else {
            mNetspeedService.runNetSpeed(bool)
        }

        if (SDK_INT >= VERSION_CODES.M) {
            gmhAccessInstance?.checkAutoSensorsOff(switchOn = true, screenOffOnly = true)

        }
    }


    private fun oneTimeAutoChecks(){
        if (viewModel.isValidAdFree.value != true && !mUtilsPrefsAct.gmhPrefAdFreeAutoChecked){
            syncLicense(silent = true, trial = false)
        }
        checkUpdate(false)
    }


    private fun updateRefreshRateLabels(){
        launch {
            mUtilsRefreshRate.apply {
                mBinding.tvResAndRatesLbl.text = getResAndRateLbl()
                mBinding.tvResAndRates.text = getDisplayModesStrGmh()
                //if (waitForSo) delay(1200)// wait for screen-off
                // mBinding.btnToggleMaxHz.text = getMaxRefreshRateLabel()
            }
        }
    }


    private fun setupMinHzSeekBar() {
        mBinding.sbMinHz.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                supportedHzIntCurMod?.closestValue(progress)?.let{
                    seekBar.progress = it
                    seekBar.thumb = getThumb(it)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekBar.progress.let {
                    seekBar.thumb = getThumb(it)
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzForToggle = it
                }
            }
        })
    }


    private fun updateMinHzSbMinMax() {
        //Log.d(TAG, "updateMinHzSeekBar() called lh: $lowestHzForAllMode")
        launch {
            delay(500)
            if (SDK_INT >= VERSION_CODES.O) {
                mBinding.sbMinHz.min = lowestHzForAllMode
            }
            mBinding.sbMinHz.max = highestHzForAllMode
            mBinding.sbMinHz.progress = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzForToggle.coerceAtLeast(lowestHzForAllMode)//coerce only here
        }
    }


    private fun setupBroadcastReceiver(){
        IntentFilter().let{
            it.addAction(ACTION_HIDE_MAIN_ACTIVITY)
            it.addAction(ACTION_CLOSE_MAIN_ACTIVITY)
            it.addAction(ACTION_POWER_SAVE_MODE_CHANGED)
            if (SDK_INT >= VERSION_CODES.P) {
                it.addAction(ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED)
            }
            registerReceiver(mReceiver, it)
        }
    }

    private var mHandler: Handler? = null
    private var wssCheckerRunnable: Runnable? = null

    override fun onResume() {
        super.onResume()
        mBinding.tvSoRefresh.text = getString(
            R.string.s_off_hz_inf,
            offScreenRefreshRate ?: "-- Hz"
        )


        if (!hasWriteSecureSetPerm) {
            mHandler =  Handler(Looper.getMainLooper())
            wssCheckerRunnable = object : Runnable {
                override fun run() {
                    checkWssPermission(false)
                    if (!hasWriteSecureSetPerm) {
                        mHandler?.postDelayed(
                            this,
                            1000 * 10
                        )
                    }
                }
            }
            mHandler!!.post(wssCheckerRunnable!!)
        }
    }

    override fun onPause() {
        mHandler?.removeCallbacks(wssCheckerRunnable!!)
        super.onPause()
    }


    /* override fun onNewIntent(intent: Intent?) {
         Log.d("TESTTESTTEST", "onNewIntent called")
         super.onNewIntent(intent)
     //  showDialogIfTileExpired()
     }*/

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.appbar_menu, menu)
        // mList = menu
        setupMenuVisibility(menu)
        return true
    }


    @RequiresApi(VERSION_CODES.M)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuUpd -> {
                checkUpdate(true)
                if (isPremium.get() == true) {
                    syncLicense(true, trial = true)
                }
                true
            }
            R.id.menuBuy -> {
                openBuyAdFreeLink()
                true
            }
            R.id.menuAct -> {
                syncLicense(false, trial = false)
                true
            }
            R.id.menuAc -> {
                DialogActCode().show(supportFragmentManager, null)
                true
            }
            R.id.menuRf -> {
                forceReloadProfile()
                true
            }
            R.id.menuHlp -> {
                openHelpLink()
                true
            }
            R.id.menuPrem -> {
                openPremiumLink()
                true
            }
            R.id.menuNs -> {
                item.isChecked.let { checked ->
                    item.isChecked = !checked
                    mBinding.showNsTool = !checked
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefShowNetSpeedTool = !checked
                }
                true
            }
            /* R.id.menuUsingSpay ->{
                 item.isChecked.let { checked ->
                     item.isChecked = !checked
                     UtilsPrefsGmhSt.instance(applicationContext).hzPrefSPayUsage = if (!checked) USING else NOT_USING
                 }
                 true
             }*/
            else -> super.onOptionsItemSelected(item)
        }
    }


    @Synchronized
    private fun checkUpdate(force: Boolean) {
        // Log.d(TAG, "MainActivity: checkUpdate called")

        if (isDownloading) return
        if (force) {
            showLoading(true)
        }
        // Log.d(TAG, "MainActivity: checkUpdate called")
        with(AppUpdaterLite(this)) {
            setUpdateJSON("$GMH_WEB_APP?Rq=$REQUEST_LATEST_UPDATE")
            setUpdateForce(force)
            if (force) {
                setCallback(
                    object : OnUpdateCheckedCallback {
                        override fun onUpdateChecked(newUpdate: Boolean) {
                            runOnUiThread {
                                showLoading(false)
                            }
                        }
                    }
                )
            }
            start()
        }
    }


    private fun isHzNotificationEnabled(): Boolean {
        return if (SDK_INT >= VERSION_CODES.O) {
            if (!NotificationManagerCompat.from(applicationContext)
                    .areNotificationsEnabled()
            ) return false
            val manager =
                applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                manager.getNotificationChannel(CHANNEL_ID_HZ) ?: return true //not yet created
            return channel.importance != IMPORTANCE_NONE
        } else {
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        }
    }


    @RequiresApi(VERSION_CODES.M)
    fun showHertz(isSwOn: Boolean, showOverlayHz: Boolean?, showNotifHz: Boolean?) {

        fun openNotifSettings() {
            @SuppressLint("InlinedApi")
            val settingsIntent = Intent(ACTION_APP_NOTIFICATION_SETTINGS).apply{
                if (SDK_INT >= VERSION_CODES.O) {
                    putExtra(EXTRA_APP_PACKAGE, packageName)
                    putExtra(EXTRA_CHANNEL_ID, CHANNEL_ID_HZ)
                } else  {
                    intent.putExtra("app_package", packageName)
                    intent.putExtra("app_uid", applicationInfo.uid)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(settingsIntent)
        }

        isHzNotificationEnabled().let{
            if (showNotifHz != it){
                mBinding.chNotifHz.isChecked = it
                ignoreUnblockHzNotifState = false
                openNotifSettings()
            }
        }

        if (isSwOn && gmhAccessInstance == null
            && showOverlayHz == true
            && !UtilPermSt.instance(applicationContext).hasOverlayPerm()
        ) {
            showSbMsg(
                getString(R.string.aot_perm_inf),
                Snackbar.LENGTH_INDEFINITE,
                android.R.string.ok
            ) {
                showAppearOnTopRequest()

            }
        }

        HzServiceHelperStn.instance(applicationContext).switchHz(
            isSwOn,
            showOverlayHz,
            showNotifHz
        )
    }


    private fun setupSizeSeekBar() {
        mBinding.sbFontSizeHz.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newProgress = hzOverlaySizes.closestValue(progress)!!
                seekBar.progress = newProgress
                seekBar.thumb = getThumb(newProgress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            @RequiresApi(VERSION_CODES.M)
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekBar.thumb = getThumb(seekBar.progress)
                HzServiceHelperStn.instance(applicationContext).updateHzSize(seekBar.progress)
            }
        })
        if (SDK_INT >= VERSION_CODES.O) {
            mBinding.sbFontSizeHz.min = hzOverlaySizes.minOrNull()!!
        }
        mBinding.sbFontSizeHz.max = hzOverlaySizes.maxOrNull()!!
        //initial value
        mBinding.sbFontSizeHz.progress = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHzOverlaySize.toInt()
    }


    private fun setupAdaptDelaySeekBar() {
        mBinding.sbAdaptiveDelay.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seekBar.progress = progress
                seekBar.thumb = getThumb(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekBar.thumb = getThumb(seekBar.progress)
                (seekBar.progress.toLong() * 1000).let {
                    adaptiveDelayMillis = it
                    //adaptiveAccessTimeout = it* TIMEOUT_FACTOR.toLong()
                    //updateAdaptiveAccessTimeOut()
                    UtilsPrefsGmhSt.instance(applicationContext).hzPrefAdaptiveDelay = it
                    updateSwitchDown()
                }
            }
        })
        if (SDK_INT >= VERSION_CODES.O) {
            mBinding.sbAdaptiveDelay.min = hzAdaptiveDelays.minOrNull()!!
        }
        mBinding.sbAdaptiveDelay.max = hzAdaptiveDelays.maxOrNull()!!
        //initial value
        mBinding.sbAdaptiveDelay.progress = UtilsPrefsGmhSt.instance(applicationContext).hzPrefAdaptiveDelay.toInt()/1000
    }

    private fun setupBrightnessSeekBar() {
        mBinding.sbBrightness.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seekBar.progress = progress
                seekBar.thumb = getThumb(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekBar.progress.let {
                    seekBar.thumb = getThumb(it)
                    val converted =
                        if (SDK_INT == VERSION_CODES.R) {
                            it
                        }else {
                            (it.toFloat() * 2.55f).toInt()
                        }
                    brightnessThreshold.set(converted)
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefGAdaptBrightnessMin = it
                }
            }
        })
        if (SDK_INT >= VERSION_CODES.O) {
            mBinding.sbBrightness.min = 0
        }
        mBinding.sbBrightness.max = BRIGHTNESS_RESOLUTION
        //initial value
        mBinding.sbBrightness.progress = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefGAdaptBrightnessMin
        mBinding.regMinHzAdaptive = UtilsDeviceInfoSt.instance(applicationContext).regularMinHz
    }


    /* private fun chIdsToGravity(topBotId: Int?, lcrId: Int?): Int {
         return when (lcrId) {
             mBinding.chLeftHz.id -> {
                 when (topBotId) {
                     mBinding.chBottomHz.id -> HzGravity.BOTTOM_LEFT
                     else -> HzGravity.TOP_LEFT
                 }
             }
             mBinding.chCentHz.id -> {
                 when (topBotId) {
                     mBinding.chBottomHz.id -> HzGravity.BOTTOM_CENTER
                     else -> HzGravity.TOP_CENTER
                 }
             }
             else -> {
                 when (topBotId) {
                     mBinding.chBottomHz.id -> HzGravity.BOTTOM_RIGHT
                     else -> HzGravity.TOP_RIGHT
                 }
             }
         }
     }
 */

    @RequiresApi(VERSION_CODES.M)
    @Synchronized
    private fun checkAccessibilityPerm(showRequest: Boolean): Boolean{
        return if (gmhAccessInstance == null){
            if (hasWriteSecureSetPerm) {
                //if (hasWriteSecureSetPerm && (isSpayInstalled == false ||  UtilsPrefsGmhSt.instance(applicationContext).hzPrefSPayUsage == NOT_USING)) {
                allowAccessibility(applicationContext, true)
                true
            }else{
                if (showRequest) {
                    InfoDialog.newInstance(InfoDialog.ENABLE_ACCESS).show(supportFragmentManager, null)
                }
                false
            }
        }else{
            true
        }
    }

    private fun setupKeepMod() {
        launch {
            while (!isProfilesLoaded) {
                delay(150)
            }
            if (currentRefreshRateMode.get() != REFRESH_RATE_MODE_SEAMLESS
                && currentRefreshRateMode.get() != REFRESH_RATE_MODE_ALWAYS
            //Standard mode
            ) {
                if (isPowerSaveMode.get() == true
                    && hasWriteSecureSetPerm && keepModeOnPowerSaving && isPremium.get()!!
                ) {
                    if (SDK_INT >= VERSION_CODES.M) {
                        mUtilsRefreshRate.setPrefOrAdaptOrHighRefreshRateMode(null)
                    }
                }
            } else {
                //High/Adaptive mode
                if (isPowerSaveMode.get() == true
                    && (currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS || currentRefreshRateMode.get() == REFRESH_RATE_MODE_ALWAYS)
                    && isPremium.get()!!
                ) {
                    keepModeOnPowerSaving = true
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefKmsOnPsm = true && isPremium.get() == true
                }
            }
            mBinding.swKeepMode.isChecked = keepModeOnPowerSaving
        }
    }


    @SuppressLint("NewApi")
    private fun setupMaxHzSeekBar() {
        var oldProg = 60
        mBinding.sbPeakHz.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newProgress = supportedHzIntAllMod?.closestValue(
                    progress
                )!!
                seekBar.progress = newProgress
                seekBar.thumb = getThumb(newProgress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                oldProg = seekBar.progress
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekBar.progress.let {prog ->
                    seekBar.thumb = getThumb(prog)
                    if (!UtilPermSt.instance(applicationContext).hasWriteSystemPerm()) {
                        seekBar.progress = oldProg
                        UtilPermSt.instance(applicationContext).requestWriteSettings()
                        return
                    }
                    UtilsPrefsGmhSt.instance(applicationContext).hzPrefMaxRefreshRate = prog
                    if (isPowerSaveMode.get() != true || !isPremium.get()!!) {
                        UtilsPrefsGmhSt.instance(applicationContext).hzPrefMaxRefreshRate.let{
                            prrActive.set( it)
                            if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt > UtilsDeviceInfoSt.instance(applicationContext).regularMinHz) {
                                mUtilsRefreshRate.setRefreshRate(it, UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt)
                            }else{
                                mUtilsRefreshRate.setRefreshRate(it, 0)
                            }
                        }
                    }

                }
            }
        })
    }


    private fun updateMaxHzSbMinMax() {
        launch {
            delay(500)
            //Log.d(TAG,"forceLowestHz $forceLowestHz vs ${mUtilsPrefsGmh.hzPrefMaxRefreshRate}")
            if (SDK_INT >= VERSION_CODES.O) {
                mBinding.sbPeakHz.min = lowestHzCurMode
            }
            mBinding.sbPeakHz.max = highestHzForAllMode
            //initial value
            mBinding.sbPeakHz.progress = UtilsPrefsGmhSt.instance(applicationContext).hzPrefMaxRefreshRate
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("NewApi")
    private fun setupAdaptMinHzSeekBar() {
        if (minHzListForAdp?.size?:0 < 2) {
            mBinding.hasMinHzOptions = false
            return
        }

        mBinding.hasMinHzOptions = true

        mBinding.sbMinHzAdapt.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val newProgress = (supportedHzIntCurMod?.closestValue(progress)!!).coerceAtMost(prrActive.get()!!)
                    seekBar.progress = newProgress
                    seekBar.thumb = getThumb(newProgress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    if (!UtilPermSt.instance(applicationContext).hasWriteSystemPerm()) {
                        UtilPermSt.instance(applicationContext).requestWriteSettings()
                        return
                    }
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    seekBar.thumb = getThumb(seekBar.progress)

                    val regMinHz = UtilsDeviceInfoSt.instance(applicationContext).regularMinHz

                    mBinding.sbMinHzAdapt.progress.let{
                        if (it <  regMinHz && isPremium.get() != true){
                            Toast.makeText(this@MainActivity, getString(R.string.is_prem_ft, "$it Hz"), Toast.LENGTH_SHORT).show()
                            seekBar.progress = regMinHz
                        }//dont return
                    }

                    if (isOfficialAdaptive && seekBar.progress < regMinHz) {
                        if (!checkAccessibilityPerm(true)) {
                            seekBar.progress = regMinHz
                        }//dont return
                    }

                    // Log.d("TESTEST", "gmhPrefMinHzAdapt2 to:${seekBar.progress}")
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt = seekBar.progress

                    mBinding.minHzAdaptive = seekBar.progress

                    mUtilsRefreshRate.applyMinHz()

                    gmhAccessInstance?.setupAdaptiveEnhancer()

                }
            })
    }


    @RequiresApi(VERSION_CODES.M)
    private fun updateAdaptMinHzSbMinMax() {
        if (minHzListForAdp?.size?:0 < 2) {
            return
        }

        launch {
            delay(500)//don't decrease
            if (isOfficialAdaptive || isFakeAdaptive.get()!!) {
                mBinding.sbMinHzAdapt.max = minHzListForAdp!!.maxOrNull()!!
                if (SDK_INT >= VERSION_CODES.O) {
                    mBinding.sbMinHzAdapt.min = minHzListForAdp!!.minOrNull()!!
                }

                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt.let {
                    if (gmhAccessInstance != null) {
                        mBinding.sbMinHzAdapt.progress = it
                        mBinding.minHzAdaptive = it
                    }else{
                        mBinding.sbMinHzAdapt.progress = it.coerceAtLeast(UtilsDeviceInfoSt.instance(applicationContext).regularMinHz)
                        mBinding.minHzAdaptive = it.coerceAtLeast(UtilsDeviceInfoSt.instance(applicationContext).regularMinHz)
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun setupPsmMaxHzSeekBar() {
        var oldProg = UtilsDeviceInfoSt.instance(applicationContext).regularMinHz
        mBinding.sbPeakHzPsm.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newProgress = supportedHzIntAllMod?.closestValue(
                    progress
                )!!
                seekBar.progress = newProgress
                seekBar.thumb = getThumb(newProgress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                oldProg = seekBar.progress
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekBar.progress.let {prog ->
                    seekBar.thumb = getThumb(prog)
                    if (!UtilPermSt.instance(applicationContext).hasWriteSystemPerm()) {
                        seekBar.progress = oldProg
                        UtilPermSt.instance(applicationContext).requestWriteSettings()
                        return
                    }
                    UtilsPrefsGmhSt.instance(applicationContext).hzPrefMaxRefreshRatePsm = prog
                    if (isPowerSaveMode.get() == true) {
                        UtilsPrefsGmhSt.instance(applicationContext).hzPrefMaxRefreshRatePsm.let{
                            prrActive.set( it)
                            mUtilsRefreshRate.setRefreshRate(it, UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt)
                        }
                    }
                }
            }
        })
    }

    @SuppressLint("NewApi")
    private fun updatePsmMaxHzSbMinMax() {
        launch {
            delay(500)
            //Log.d(TAG,"forceLowestHz PSM $forceLowestHz vs ${mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm}")
            if (SDK_INT >= VERSION_CODES.O) {
                mBinding.sbPeakHzPsm.min = lowestHzCurMode
            }
            mBinding.sbPeakHzPsm.max = highestHzForAllMode
            //initial value
            mBinding.sbPeakHzPsm.progress = UtilsPrefsGmhSt.instance(applicationContext).hzPrefMaxRefreshRatePsm/*.let {
                if (it != -1) {
                    it
                } else {
                    mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm = highestHzForAllMode
                    highestHzForAllMode
                }
            }*/
        }
    }




    @RequiresApi(VERSION_CODES.M)
    private fun forceReloadProfile(){
        launch {
            showLoading(true)
            UtilsPrefsGmhSt.instance(applicationContext).gmhRefetchProfile = true
            UtilsPrefsGmhSt.instance(applicationContext).prefProfileFetched = false
            mUtilsRefreshRate.initProfiles()
            updateDynamicViews()
            showLoading(false)
        }
    }

    private fun openPremiumLink() = launch {
        showLoading(true)
        openUrl(Uri.parse("https://github.com/tribalfs/GalaxyMaxHzPub/blob/main/Premium.md"))
        showLoading(false)
    }


    private fun openHelpLink() = launch {
        showLoading(true)
        openUrl(Uri.parse(getHelpUrl()))
        showLoading(false)
    }

    private suspend fun getHelpUrl(): String?= withContext(Dispatchers.IO){
        return@withContext if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHelpUrl != null){
            UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHelpUrl!!
        }else{
            val result = mUtilsRefreshRate.mSyncer.getHelpUrl()?.get(KEY_JSON_HELP_URL) as String?
            if (result != null){
                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHelpUrl = result
                result
            }else{
                null
            }
        }
    }

    private suspend fun getBuyLink(): String? {
        if (buyLink != null) return buyLink
        val resultJson = mUtilsRefreshRate.mSyncer.getBuyAdFreeLink()
        return if (resultJson != null && resultJson[KEY_JSON_RESULT] == JSON_RESPONSE_OK) {
            try {
                buyLink = (resultJson[KEY_JSON_PAYPAL_BUY_URL] as String)
                buyLink
            } catch (_: Exception) {
                null
            }
        }else{
            null
        }
    }

    private fun openBuyAdFreeLink() = launch {
        showLoading(true)
        getBuyLink()?.let{
            openUrl(Uri.parse(it))
        }?: run{
            showSbMsg(R.string.cie, null, null, null)
        }
        showLoading(false)
    }

    private fun openUrl(uri: Uri) = launch {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                )
            )
        }catch(_: ActivityNotFoundException){
            showSbMsg("Unable to open link. Install an internet browser to open.", null, null, null)
        }catch (_: Exception){
            showSbMsg(R.string.cie, null, null, null)
        }
    }



    //@RequiresApi(VERSION_CODES.M)
    fun syncLicense(silent: Boolean, trial: Boolean) = launch {
        if (!silent) showLoading(true)

        val resultJson = mUtilsRefreshRate.mSyncer.syncLicense(mUtilsPrefsAct.gmhPrefActivationCode?:"", trial)

        if (resultJson != null ){
            //  Log.d(TAG, "License check server response OK")

            if (resultJson[KEY_JSON_LIC_TYPE] == LIC_TYPE_INVALID_CODE &&
                mUtilsPrefsAct.gmhPrefLicType == LIC_TYPE_TRIAL_ACTIVE
            ) {
                if (!silent) {
                    LvlResultMsg(
                        this@MainActivity,
                        mUtilsPrefsAct,
                        UtilsDeviceInfoSt.instance(applicationContext).deviceModelVariant
                    ).showMsg(
                        object : LvlSbMsgCallback {
                            override fun onResult(
                                msg: String,
                                actionStr: Int?,
                                action: View.OnClickListener?,
                                sbLen: Int?
                            ) {
                                showSbMsg(msg, sbLen, actionStr, action)
                            }
                        },
                        LIC_TYPE_INVALID_CODE
                    )
                }
                showLoading(false)

            } else {
                launch(Dispatchers.IO) {
                    mUtilsPrefsAct.apply {
                        if (silent) gmhPrefAdFreeAutoChecked = true
                        gmhPrefSignature = resultJson[KEY_JSON_SIGNATURE] as String
                        gmhPrefTrialStartDate = resultJson[KEY_JSON_TRIAL_START_DATE] as String
                        gmhPrefPremiumTrialDays = resultJson[KEY_JSON_TRIAL_DAYS] as Int
                        gmhPrefActivationCode = resultJson[KEY_JSON_ACTIVATION_CODE] as String//set to trial if trial
                        gmhPrefLicType = resultJson[KEY_JSON_LIC_TYPE] as Int
                    }
                    delay(500)

                    withContext(Dispatchers.Main) {
                        mUtilsPrefsAct.gmhPrefSignature?.let { viewModel.setServerSign(it) }
                        viewModel.setAdFreeCode(mUtilsPrefsAct.gmhPrefLicType)
                    }

                    if (!silent) {
                        LvlResultMsg(this@MainActivity, mUtilsPrefsAct, UtilsDeviceInfoSt.instance(applicationContext).deviceModelVariant).showMsg(
                            object : LvlSbMsgCallback {
                                override fun onResult(
                                    msg: String,
                                    actionStr: Int?,
                                    action: View.OnClickListener?,
                                    sbLen: Int?
                                ) {
                                    showSbMsg(msg, sbLen, actionStr, action)
                                }
                            }, null
                        )
                    }

                    //Needed
                    if (SDK_INT >= VERSION_CODES.M) {
                        gmhAccessInstance?.setupAdaptiveEnhancer()
                        // mNetspeedService.setupNetworkCallback()
                    }

                    showLoading(false)
                }
            }
        } else {
            //  Log.d(TAG, "License check server response Error")
            showLoading(false)
            if (!silent) showSbMsg(R.string.cie, null, null, null)
        }
    }



    private fun getResAndRateLbl(): String {
        // Log.d(TAG,"getResAndRateLbl called" )
        val sfx = //if (mUtilsDeviceInfo.deviceIsSamsung) {
            when (currentRefreshRateMode.get()) {
                REFRESH_RATE_MODE_ALWAYS -> getString(R.string.high_mode)
                REFRESH_RATE_MODE_STANDARD -> getString(R.string.std_mode)
                REFRESH_RATE_MODE_SEAMLESS -> getString(R.string.adp_mode)
                else -> ""
            }
        return getString(R.string.lbl_profiles_h, if (sfx.isNotEmpty()) "$sfx\n" else "")
    }



    private fun setupResoSwitcherFilter() {
        if (mBinding.cgResSwFilter.childCount > 0) {
            mBinding.cgResSwFilter.removeAllViews()
        }

        mUtilsRefreshRate.getResolutionsForKey(null)?.forEach {
            val key = it.keys.first()
            val res = "$key(${it[key]?.resName})"

            (layoutInflater.inflate(R.layout.chip_layout, null, false) as Chip).apply {

                text = res
                isCheckable = true
                isCheckedIconVisible = true
                chipIconSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    18f,
                    resources.displayMetrics
                )

                isChecked = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefGetSkippedRes?.let{ skips -> !skips.contains(res) }?:true

                setOnClickListener { view ->
                    (view as Chip).let { ch ->
                        updateSkipRes(ch.text.toString(), !ch.isChecked)
                    }
                }

                //isEnabled = hasWriteSecureSetPerm

                mBinding.cgResSwFilter.addView(this)
            }
        }
    }


    private fun updateSkipRes(res: String, skip: Boolean){
        UtilsPrefsGmhSt.instance(applicationContext).gmhPrefGetSkippedRes?.let {
            val hs = HashSet<String>(it)
            hs.apply {
                if (skip) this.add(res) else this.remove(res)
                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefGetSkippedRes = this
            }
        }?: run {
            if (skip) {
                HashSet<String>().let {
                    it.add(res)
                    UtilsPrefsGmhSt.instance(applicationContext).gmhPrefGetSkippedRes = it.toSet()
                }
            }
        }
    }


    @RequiresApi(VERSION_CODES.M)
    private fun updateNetSpeed(addFree: Boolean){
        if (!addFree){
            if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefNetSpeedIsOn) {
                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefNetSpeedIsOn = false
                switchNetSpeed(false)
            }
        }else{
            UtilsPrefsGmhSt.instance(applicationContext).gmhPrefNetSpeedIsOn.let{
                mBinding.swNetspeed.isChecked = it
                switchNetSpeed(it)
            }
        }
    }

    private fun initNeedSpeed(){
        when (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedUnit) {
            BIT_PER_SEC -> mBinding.cgBytesBits.check(mBinding.chBits.id)
            BYTE_PER_SEC -> mBinding.cgBytesBits.check(mBinding.chBytes.id)
        }
        when (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSpeedToShow){
            UPLOAD_SPEED -> mBinding.cgDataStream.check(mBinding.chUpStream.id)
            DOWNLOAD_SPEED -> mBinding.cgDataStream.check(mBinding.chDownStream.id)
            TOTAL_SPEED -> mBinding.cgDataStream.check(mBinding.chCombinedStream.id)
        }
        mBinding.showNsTool = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefShowNetSpeedTool
        mBinding.tvDataUsage.movementMethod = LinkMovementMethod.getInstance()
        mBinding.tvDataUsage.setLinkTextColor(Color.BLUE)
    }


    private fun isNetSpeedNotificationEnabled(): Boolean {
        return if (SDK_INT >= VERSION_CODES.O) {
            if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return false
            val manager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(CHANNEL_ID_NET_SPEED) ?: return true //not yet created
            return channel.importance != IMPORTANCE_NONE
        } else {
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        }
    }



    private fun inflateViews(){
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mBinding.clickListener = this@MainActivity

        @SuppressLint("InflateParams")
        thumbView = LayoutInflater.from(this).inflate(R.layout.sb_thumb, null, false)
    }

    private fun updateDisplayId(){
        if (SDK_INT >= VERSION_CODES.R) {
            displayId = display!!.displayId
        }
    }

    @RequiresApi(VERSION_CODES.M)
    private fun updateViewModelSig() {
        mUtilsPrefsAct.gmhPrefSignature?.let { viewModel.setServerSign(it) }
    }

    private fun updateWssPerm() {
        //Updater
        UtilPermSt.instance(applicationContext).hasWriteSecurePerm().let {hasWsp ->
            if (hasWsp){
                mBinding.hasWssPerm = true
                hasWriteSecureSetPerm = true
                hasWriteSystemSetPerm = true
            }else{
                launch(Dispatchers.IO) {
                    grantSecPerm().let{rootGranted ->
                        mBinding.hasWssPerm = rootGranted
                        hasWriteSecureSetPerm = rootGranted
                        hasWriteSystemSetPerm = rootGranted || UtilPermSt.instance(applicationContext).hasWriteSystemPerm()

                    }
                }
            }
        }
    }



    private fun setupActionBar() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.apply {
            subtitle = "${UtilsDeviceInfoSt.instance(applicationContext).deviceModelVariant} | " +
                    "AOS ${UtilsDeviceInfoSt.instance(applicationContext).androidVersion}" +
                    if (UtilsDeviceInfoSt.instance(applicationContext).oneUiVersion != null) " | " +
                            "OneUI ${UtilsDeviceInfoSt.instance(applicationContext).oneUiVersion}" else ""
            title = "$applicationName v$VERSION_NAME"
            setDisplayShowHomeEnabled(true)
        }
    }


    @RequiresApi(VERSION_CODES.M)
    private fun registerSharedPrefListener() {
        UtilsPrefsGmhSt.instance(applicationContext).hzSharedPref.registerOnSharedPreferenceChangeListener(gmhPrefChngListener)
    }

    @RequiresApi(VERSION_CODES.M)
    private fun setupHzMon() {
        UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHzOverlayIsOn.let {
            mBinding.chOverlayHz.isEnabled = true
            mBinding.chOverlayHz.isChecked = it
            mBinding.hideHzOverlaySettings = !it
        }
        mBinding.chNotifHz.isChecked = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHzNotifIsOn && isHzNotificationEnabled()
        mBinding.swHzOn.isChecked = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHzIsOn
        mBinding.cgTopBottom.check(UtilsPrefsGmhSt.instance(applicationContext).gmhPrefChipIdTb)
        // mBinding.cgLeftCentRightHz.check(UtilsPrefsGmhSt.instance(applicationContext).gmhPrefChipIdLrc)

    }



    private fun setupForceHzOnSo() {
//Note: Place after Hz status observer
        if (SDK_INT >= VERSION_CODES.M) {
            mBinding.swSoForceLowestHz.isChecked =
                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefForceLowestSoIsOn &&
                        hasWriteSystemSetPerm && checkAccessibilityPerm(false)
        }
    }


    private fun setupScreenOffPsm(){
        if (hasWriteSecureSetPerm) {
            mBinding.swSoPsm.isEnabled = true
            mBinding.swSoPsm.isChecked = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefPsmOnSo //free features
        }else{
            mBinding.swSoPsm.isEnabled = false
            UtilsPrefsGmhSt.instance(applicationContext).gmhPrefPsmOnSo = false
        }
    }


    private fun setupScreenOffSensorsOff(adfree: Boolean){
        if (SDK_INT < VERSION_CODES.R) {
            mBinding.sensorsOffSupported =false
            return
        }
        mBinding.sensorsOffSupported =true

        if (adfree && gmhAccessInstance != null //checkAccessibilityPerm(false)
        ) {
            mBinding.swAutoSensorsOff.isChecked = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorsOff
            if (!UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorOnKey.isNullOrEmpty()) {
                sensorOnKey = UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorOnKey
            }
        } else {
            mBinding.swAutoSensorsOff.isChecked = false
            UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorsOff = false
        }
    }


    private fun getThumb(progress: Int): Drawable {
        (thumbView.findViewById<View>(R.id.tvProgress) as TextView).text = progress.toString()
        thumbView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val bitmap = Bitmap.createBitmap(
            thumbView.measuredWidth,
            thumbView.measuredHeight,
            Bitmap.Config.ARGB_8888
        )

        thumbView.layout(0, 0, thumbView.measuredWidth, thumbView.measuredHeight)

        val canvas = Canvas(bitmap)

        thumbView.draw(canvas)
        return BitmapDrawable(resources, bitmap)
    }



    @RequiresApi(VERSION_CODES.M)
    override fun onDestroy() {
        // Log.d(TAG, "onDestroy() called")
        UtilsPrefsGmhSt.instance(applicationContext).hzSharedPref.unregisterOnSharedPreferenceChangeListener(gmhPrefChngListener)
        currentRefreshRateMode.removeOnPropertyChangedCallback(mPropertyChangeCallback)
        isPowerSaveMode.removeOnPropertyChangedCallback(mPropertyChangeCallback)
        unregisterReceiver(mReceiver)
        sensorOnKey?.let{UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorOnKey = it}
        masterJob.cancel()
        super.onDestroy()
    }



    private fun showLoading(bool: Boolean) {
        launch(Dispatchers.Main) {
            if (bool) {
                mBinding.loadingFrame.visibility = View.VISIBLE
                mBinding.hzPulse.startAnimation(
                    loadAnimation(
                        this@MainActivity,
                        R.anim.pulse
                    )
                )
            } else {
                mBinding.hzPulse.clearAnimation()
                mBinding.loadingFrame.visibility = View.GONE

            }
        }
    }


    private fun showSbMsg(msg: String, length: Int?, actionStr: Int?, action: View.OnClickListener?){
        launch(Dispatchers.Main) {
            Snackbar.make(
                mBinding.root,
                msg,
                length ?: Snackbar.LENGTH_LONG
            ).apply {
                if (actionStr != null) {
                    setAction(actionStr, action)
                }
            }.show()
        }
    }


    private fun showSbMsg(msgId: Int, length: Int?, actionStr: Int?, action: View.OnClickListener?){
        showSbMsg(getString(msgId), length, actionStr, action)
    }


    @RequiresApi(VERSION_CODES.M)
    internal fun applyActivationCode(actCode: String){
        mUtilsPrefsAct.gmhPrefActivationCode = actCode
        syncLicense(silent = false, trial = false)
    }

}




