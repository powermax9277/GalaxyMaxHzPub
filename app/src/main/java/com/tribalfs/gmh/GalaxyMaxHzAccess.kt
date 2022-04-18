package com.tribalfs.gmh

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.Notification.VISIBILITY_PRIVATE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfo.CATEGORY_GAME
import android.content.pm.ApplicationInfo.CATEGORY_VIDEO
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_VOLUME_DOWN
import android.view.KeyEvent.KEYCODE_VOLUME_UP
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.WRAP_CONTENT
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.*
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.tribalfs.gmh.callbacks.DisplayChangedCallback
import com.tribalfs.gmh.callbacks.GmhBroadcastCallback
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveAccessTimeout
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveDelayMillis
import com.tribalfs.gmh.helpers.CacheSettings.applyAdaptiveMod
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.disablePsm
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hzNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.hzStatus
import com.tribalfs.gmh.helpers.CacheSettings.ignoreRrmChange
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptiveValid
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isSamsung
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.lrrPref
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.restoreSync
import com.tribalfs.gmh.helpers.CacheSettings.screenOffRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.sensorOnKey
import com.tribalfs.gmh.helpers.CacheSettings.turnOffAutoSensorsOff
import com.tribalfs.gmh.hertz.HzGravity
import com.tribalfs.gmh.hertz.HzNotifGlobal.CHANNEL_ID_HZ
import com.tribalfs.gmh.hertz.HzNotifGlobal.NOTIFICATION_ID_HZ
import com.tribalfs.gmh.hertz.HzNotifGlobal.hznotificationBuilder
import com.tribalfs.gmh.hertz.HzNotifGlobal.hznotificationChannel
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.hertz.MyDisplayListener
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.profiles.ProfilesObj.isProfilesLoaded
import com.tribalfs.gmh.receivers.GmhBroadcastReceivers
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.*
import java.lang.Integer.max
import java.lang.Runnable
import kotlin.coroutines.CoroutineContext


internal const val PLAYING = 1
internal const val STOPPED = -1
internal const val PAUSE = 0
private const val MAX_TRY = 8

private val manualVideoAppList = listOf(
    "amazon.avod",
    "youtube",
    "newpipe",
    "webex.meetings",
    "gallery",
    "tvplus",
    "netflix",
    "disneyplus",
    "plexapp",
    "hbo.hbogo",
    "iqiyi",
    "bilibili",
    "vuclip",
    "skytube",
    "libretube",
    "twitch",
    "video",
    "iflix.play",
    "viki",
    "player",
    "koushikdutta.cast",
    "xbmc.kodi",
    "com.teamseries.lotus"
)

private val useStockAdaptiveList = listOf(
    "browser",
    "samsung.android.app.appsedge",
    "com.opera",
    "com.uc.",
    "mozilla.firefox",
    "duckduckgo",
    "android.chrome",
    "microsoft.emmx",
    "microsoft.bing",
    "android.apps.photos",
    "com.nbaimd",
    "com.twitter",
    "com.instagram",
    "ss.android.ugc." //tiktok
)

private val manualGameList = listOf(
    "com.google.stadia",
    "steamlink",
    "android.steam",
    "com.microsoft.xcloud",
    "com.microsoft.xboxone",
    "com.gameloft",
    "android.app.samsungapps:InstantPlaysGameActivity",
    "com.distractionware",
    "supercell",
    "com.google.android.gms",
    "com.samsung.android.app.notes",
    "com.epicgames"
)


@ExperimentalCoroutinesApi
@RequiresApi(Build.VERSION_CODES.M)
class GalaxyMaxHzAccess : AccessibilityService(), CoroutineScope {

    companion object{
        internal var gmhAccessInstance: GalaxyMaxHzAccess? = null
    }

    private val masterJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = masterJob + Dispatchers.IO

    private val mConnectivityManager by lazy {applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager}
    private val mKeyguardManager by lazy {applicationContext.getSystemService(KEYGUARD_SERVICE) as KeyguardManager}
    private val mHandler by lazy {Handler(Looper.getMainLooper())}
    private val mCameraManager by lazy {getSystemService(CAMERA_SERVICE) as CameraManager}
    private val mUtilsRefreshRate by lazy {UtilRefreshRateSt.instance(applicationContext)}
    private val mNotifBar by lazy {UtilNotifBarSt.instance(applicationContext)}
    private var triesA: Int = 0
    private var triesB: Int = 0
    private var useStockAdaptive = false
    private var ignoreScrollForNonNative = false
    private var useMin60 = false
    private var volumePressed = false
    private var cameraOpen: Boolean = false
    private val mUtilsPrefGmh by lazy { UtilsPrefsGmhSt.instance(applicationContext) }
    private val notificationManagerCompat by lazy { NotificationManagerCompat.from(applicationContext)}
    private val mNotificationContentView by lazy { RemoteViews(applicationContext.packageName, R.layout.hz_notification) }
    private val mNotifIcon by lazy{ UtilNotifIcon() }
    private var mTransfyHzJob: Job? = null
    private var mPauseHzJob: Job? = null
    private val refreshRateModeChangeCallback by lazy{
        object : OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                launch {
                    updateNotif(mDisplay.refreshRate.toInt().toString())
                }
            }
        }
    }
    private val myBatteryManager by lazy {applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager}

    private fun isCharging(): Boolean {
        return myBatteryManager.isCharging
    }

    private val autoSensorsOffRunnable: Runnable by lazy {
        Runnable {
            if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorsOff) {
                if (isCharging()) return@Runnable
                switchSensorsOff(true)
                //Workaround sensors off sometimes trigger action_SCREEN_ON
                mHandler.postDelayed(forceLowestRunnable,1000)
            }
        }
    }

    private val forceLowestRunnable: Runnable by lazy {
        Runnable {
            if (screenOffRefreshRateMode != currentRefreshRateMode.get()) {
                ignoreRrmChange.set(true)
                if (mUtilsRefreshRate.setRefreshRateMode(screenOffRefreshRateMode!!)) {
                    mUtilsRefreshRate.setRefreshRate(lowestHzForAllMode, null)
                } else {
                    mUtilsRefreshRate.setRefreshRate(lowestHzCurMode, null)
                    ignoreRrmChange.set(false)
                }
            } else {
                mUtilsRefreshRate.setRefreshRate(lowestHzCurMode, null)
            }
        }
    }



    private val mGmhBroadcastCallback = object: GmhBroadcastCallback {
        override fun onIntentReceived(intent: String) {
            when (intent) {
                ACTION_SCREEN_OFF -> {
                    isScreenOn.set(false)
                    restoreSync.set(false)
                    disablePsm.set(false)

                    // Workaround for AOD Bug on some device????
                    mUtilsRefreshRate.clearPeakAndMinRefreshRate()

                    if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefForceLowestSoIsOn) { mHandler.postDelayed(forceLowestRunnable,2000) }

                    mHandler.postDelayed(autoSensorsOffRunnable, 20000)

                    if (hzStatus.get() == PLAYING) {
                        mPauseHzJob?.cancel()
                        mPauseHzJob = null
                        mPauseHzJob = launch(Dispatchers.Main) {
                            delay(10000)
                            if (!isScreenOn.get()) {
                                pauseHz()
                            }
                        }
                        mPauseHzJob?.start()
                    }
                }

                ACTION_SCREEN_ON ->{
                    isScreenOn.set(true)

                    //handler.removeCallbacks { autoSensorsOffRunnable }
                    mHandler.removeCallbacksAndMessages(null)

                    launch {
                        val mHz = if (isSamsung){
                            if (UtilsDeviceInfoSt.instance(applicationContext).regularMinHz < UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt){
                                UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt
                            }else{
                                0
                            }
                        }else{
                            UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt
                        }

                        mUtilsRefreshRate.setRefreshRate(prrActive.get()!!, mHz)
                        currentRefreshRateMode.get()?.let {
                            if (screenOffRefreshRateMode != it) {
                                mUtilsRefreshRate.setRefreshRateMode(it)
                            }
                        }
                    }

                    mPauseHzJob?.cancel()
                    if(hzStatus.get() == PAUSE) {
                        startHz()
                    }

                }

                ACTION_USER_PRESENT -> {
                    if (isFakeAdaptiveValid.get()!!) makeAdaptive()
                    if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorsOff || turnOffAutoSensorsOff) {
                        switchSensorsOff(false)
                    }
                    if (turnOffAutoSensorsOff){
                        UtilsPrefsGmhSt.instance(applicationContext).gmhPrefSensorsOff = false
                        turnOffAutoSensorsOff = false
                    }
                }

            }
        }
    }

    private val mScreenStatusReceiver by lazy{
        GmhBroadcastReceivers(applicationContext, mGmhBroadcastCallback, this, mHandler)
    }

    private var mWindowsManager: WindowManager? = null
    private var params: WindowManager.LayoutParams? = null
    private var mLayout: FrameLayout? = null
    private var stageView: View? = null
    private val dm by lazy { applicationContext.getSystemService(DISPLAY_SERVICE) as DisplayManager}
    private val mDisplay by lazy { dm.getDisplay(displayId) }
    private var hzText: TextView? = null
    private var hzOverlayOn: Boolean? = null

    private val mDisplayChangeCallback by lazy{ object: DisplayChangedCallback {
        override fun onDisplayChanged() {
            launch(Dispatchers.Main) {
                val newHz = mDisplay.refreshRate.toInt()
                updateRefreshRateViews(newHz)
            }
        }

    }}

    private val displayListener by lazy { MyDisplayListener(mDisplayChangeCallback) }

    private val networkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                if (mUtilsPrefGmh.gmhPrefNetSpeedIsOn) {
                    launch { NetSpeedServiceHelperStn.instance(applicationContext).stopNetSpeed() }
                }
            }
            override fun onAvailable(network: Network) {
                if (mUtilsPrefGmh.gmhPrefNetSpeedIsOn) {
                    launch { NetSpeedServiceHelperStn.instance(applicationContext).startNetSpeed() }
                }
            }
        }
    }

    private val cameraCallback by lazy{
        object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                super.onCameraAvailable(cameraId)
                cameraOpen = false
                makeAdaptive()
            }

            override fun onCameraUnavailable(cameraId: String) {
                super.onCameraUnavailable(cameraId)
                cameraOpen = true
            }
        }
    }

    private val defaultLauncherName by lazy{DefaultApps.getLauncher(applicationContext)}

    private fun switchSensorsOff(on: Boolean) {
        triesA = 0
        triesB = 0
        switchSensorsOffInner(on)
    }


    private fun switchSensorsOffInner(targetState: Boolean) {

        if ( triesA < MAX_TRY) {
            launch {
                var childs: List<AccessibilityNodeInfo>? = null

                val sensorState = mNotifBar.isSensorsOff()

                if (sensorState != null){
                    if (sensorState != targetState) {
                        while (childs == null && triesB < MAX_TRY) {
                            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                            try {
                                childs = rootInActiveWindow?.findAccessibilityNodeInfosByText("sensors off")
                            } catch (_: Exception) {
                                triesB += 1
                                delay(200)
                                switchSensorsOffInner(targetState)
                            }
                        }
                        childs?.forEach {
                            val contDesc = it.contentDescription
                            if (contDesc != null && it.isClickable) {
                                try {
                                    it.performAction(ACTION_CLICK)
                                    UtilNotifBarSt.instance(applicationContext)
                                        .collapseNotificationBar()
                                    return@launch
                                } catch (_: java.lang.Exception) { }
                            }
                        }
                    }else{
                        return@launch
                    }
                    triesA += 1
                    delay(200)
                    switchSensorsOffInner(targetState)
                }else {
                    //Workaround if sensor state can't be read from isSensorsOff())
                    while (childs == null && triesB < MAX_TRY) {
                        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                        try {
                            childs = rootInActiveWindow?.findAccessibilityNodeInfosByText("sensors off")
                        } catch (_: Exception) {
                            triesB += 1
                            delay(200)
                            switchSensorsOffInner(targetState)
                        }
                    }

                    childs?.forEach {
                        val initDesc = it.contentDescription
                        if (initDesc != null && it.isClickable) {

                            if (mKeyguardManager.isKeyguardLocked) {
                                try {
                                    it.apply{
                                        performAction(ACTION_CLICK)
                                        delay(600)
                                        refresh()
                                        sensorOnKey = if (initDesc != contentDescription) contentDescription else initDesc
                                    }
                                }catch (_: java.lang.Exception) { }
                            }

                            if (sensorOnKey != null){
                                if ((sensorOnKey == initDesc) != targetState) {
                                    it.performAction(ACTION_CLICK)
                                }
                                if (UtilNotifBarSt.instance(applicationContext).collapseNotificationBar()) {
                                    return@launch
                                }
                            }else{
                                when(initDesc.split(",")[1]){
                                    "On.","已开启。","Til.","פועל.","Включено","ON","Activé","Activado" -> {
                                        sensorOnKey = initDesc
                                    }

                                    else ->{
                                        UtilNotifBarSt.instance(applicationContext)
                                            .collapseNotificationBar()
                                        return@launch
                                    }
                                }
                            }
                            triesA += 1
                            delay(200)
                            switchSensorsOffInner(targetState)
                        }
                    }
                    triesA += 1
                    delay(200)
                    switchSensorsOffInner(targetState)
                }
            }
        }else {
            UtilNotifBarSt.instance(applicationContext)
                .collapseNotificationBar()
            return
        }
    }

    private fun setupScreenStatusReceiver(){
        IntentFilter().apply {
            addAction(ACTION_SCREEN_OFF)
            addAction(ACTION_SCREEN_ON)
            addAction(ACTION_USER_PRESENT)
            addAction(ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(ACTION_LOCALE_CHANGED)
            priority = 999
            registerReceiver(mScreenStatusReceiver, this)
        }
    }

    private fun disableNetworkCallback(){
        try{
            mConnectivityManager.unregisterNetworkCallback(networkCallback)
        }catch (_: java.lang.Exception){}
    }

    @RequiresApi(Build.VERSION_CODES.N)
    internal fun setupNetworkCallback(enable: Boolean){
        disableNetworkCallback()
        if (enable){
            try {
                mConnectivityManager.registerDefaultNetworkCallback(networkCallback)
            } catch (_: Exception) {
            }
        }
    }

    private fun registerCameraCallback(){
        try{
            unregisterCameraCallback()
        }catch (_: Exception){}
        mCameraManager.registerAvailabilityCallback(cameraCallback, mHandler)
    }

    private fun unregisterCameraCallback(){
        mCameraManager.unregisterAvailabilityCallback(cameraCallback)
    }

    override fun onCreate() {
        setupScreenStatusReceiver()
        setupNotification()
    }

    @SuppressLint("NewApi")
    private fun setupNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hznotificationChannel == null) {
            hznotificationChannel = NotificationChannel(
                CHANNEL_ID_HZ,
                getString(R.string.refresh_rate_mon),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                lightColor = Color.BLUE
                setShowBadge(false)
                vibrationPattern = longArrayOf(0)
                enableVibration(true)
                notificationManagerCompat.createNotificationChannel(this)
            }
        }

        if (hznotificationBuilder == null) {
            hznotificationBuilder =
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Notification.Builder(applicationContext, CHANNEL_ID_HZ)
                } else {
                    @Suppress("DEPRECATION")
                    Notification.Builder(applicationContext)
                })
                    .setSmallIcon(R.drawable.ic_max_hz_12)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setCategory(Notification.CATEGORY_STATUS)
                    .setVisibility(VISIBILITY_PRIVATE)
                    .setLocalOnly(true)
                    .setCustomContentView(mNotificationContentView)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onServiceConnected() {
        gmhAccessInstance = this
        HzServiceHelperStn.instance(applicationContext).stopHzService()
        setupOverlay()
        setupAdaptiveEnhancer()
        this.serviceInfo.apply {
            notificationTimeout = adaptiveAccessTimeout
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                /*Set the recommended time that interactive controls
                need to remain on the screen to support the user.*/
                interactiveUiTimeoutMillis = 0
                nonInteractiveUiTimeoutMillis = 0
            }
        }

        if(mUtilsPrefGmh.gmhPrefHzIsOn){
            startHz()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        gmhAccessInstance = null
        HzServiceHelperStn.instance(applicationContext).switchHz()
        return super.onUnbind(intent)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun setupOverlay(){
        mWindowsManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mLayout =  FrameLayout(this)
        params =    WindowManager.LayoutParams()
        params!!.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }else{
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        params!!.format = PixelFormat.TRANSLUCENT
        params!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        params!!.width = WRAP_CONTENT
        params!!.height = WRAP_CONTENT
        params!!.x = 0
        params!!.y = 0
        params!!.gravity = HzGravity.TOP_LEFT
        stageView = LayoutInflater.from(this).inflate(R.layout.hz_overlay, mLayout)
        hzText = stageView!!.findViewById(R.id.tvHzBeatMain)

        try {
            mWindowsManager!!.removeView(mLayout)
        }catch(_:Exception){ }

        try {
            mWindowsManager!!.addView(mLayout, params)
        }catch(_:Exception){ }
    }

    internal fun startHz() {
        hzStatus.set(PLAYING)
        hzNotifOn.set(mUtilsPrefGmh.gmhPrefHzNotifIsOn)

        if (hzNotifOn.get() == true) {
            hznotificationBuilder?.setVisibility(VISIBILITY_PRIVATE)
            notificationManagerCompat.notify(
                NOTIFICATION_ID_HZ,
                hznotificationBuilder!!.build()
            )
        }

        hzOverlayOn = mUtilsPrefGmh.gmhPrefHzOverlayIsOn

        launch(Dispatchers.Main) {
            if (hzOverlayOn == true) {
                if (params?.gravity != mUtilsPrefGmh.gmhPrefHzPosition) {
                    params?.gravity = mUtilsPrefGmh.gmhPrefHzPosition
                    try {
                        mWindowsManager?.updateViewLayout(mLayout, params)
                    } catch (_: Exception) {
                    }
                }
                hzText?.visibility = View.VISIBLE
                hzText?.textSize = mUtilsPrefGmh.gmhPrefHzOverlaySize
            } else {
                hzText?.visibility = View.GONE
            }
        }

        updateRefreshRateViews(mDisplay.refreshRate.toInt())

        dm.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        currentRefreshRateMode.addOnPropertyChangedCallback(refreshRateModeChangeCallback)
        lrrPref.addOnPropertyChangedCallback(refreshRateModeChangeCallback)
    }

    private fun pauseHz() {
        stopHz()
        hzStatus.set(PAUSE)
    }

    internal fun stopHz() {
        hzStatus.set(STOPPED)
        hznotificationBuilder!!.setVisibility(Notification.VISIBILITY_SECRET)
        notificationManagerCompat.notify(
            NOTIFICATION_ID_HZ,
            hznotificationBuilder!!.build()
        )
        mTransfyHzJob?.cancel()
        launch(Dispatchers.Main) {
            hzText?.visibility = View.GONE
        }
        notificationManagerCompat.cancel(NOTIFICATION_ID_HZ)
        dm.unregisterDisplayListener(displayListener)
        currentRefreshRateMode.removeOnPropertyChangedCallback(refreshRateModeChangeCallback)
        lrrPref.removeOnPropertyChangedCallback(refreshRateModeChangeCallback)
    }

    private fun updateRefreshRateViews(newHz: Int){
        updateNotif(newHz.toString())
        updateOverlay(newHz)
    }

    private fun updateNotif(hzStr: String) = launch(Dispatchers.Main) {
        if (hzNotifOn.get()!!) {
            hznotificationBuilder?.apply {
                setSmallIcon(mNotifIcon.getIcon(hzStr, "Hz"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setCustomContentView(
                        RemoteViews(mNotificationContentView).apply {
                            setTextViewText(R.id.tvHz,
                                "${
                                    when (currentRefreshRateMode.get()){
                                        REFRESH_RATE_MODE_SEAMLESS ->{
                                            getString(R.string.adp_mode)+ ": " + lrrPref.get()+"-"+prrActive.get()
                                        }
                                        REFRESH_RATE_MODE_ALWAYS ->{
                                            getString(R.string.high_mode)
                                        }
                                        else ->{
                                            getString(R.string.std_mode)
                                        } }
                                }  |  ${
                                    getString(R.string.cur_rr_h, hzStr)
                                }"
                            )
                        }
                    )
                }
                notificationManagerCompat.notify(
                    NOTIFICATION_ID_HZ,
                    build()
                )
            }
        }
    }

    //if triggered by tasker
    internal fun checkAutoSensorsOff(switchOn:Boolean, screenOffOnly: Boolean){
        if (switchOn){
            if ((screenOffOnly && !isScreenOn.get()) || !screenOffOnly) {
                //turnOnAutoSensorsOff = false
                switchSensorsOff(true)
            }
        } else {
            if (!mKeyguardManager.isKeyguardLocked) {
                switchSensorsOff(false)
            }
        }
    }



    private fun isPartOf(list: List<String>, cn: ComponentName): Boolean {
        list.forEach {item ->
            item.split(":").let{
                if (cn.packageName.contains(it[0]) && (if (it.size != 2) true else cn.className.contains(it[1]) ) ){
                    return true
                }
            }
        }
        return false
    }



    private fun tryGetActivity(componentName: ComponentName): ActivityInfo? {
        return try {
            packageManager.getActivityInfo(componentName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }


    private var ignoreNextWinChange = true
    private val ignoreAhhRunnable = Runnable {
        ignoreNextWinChange = true
    }

    private fun setTempIgnoreWinChange(){
        mHandler.removeCallbacks(ignoreAhhRunnable)
        ignoreNextWinChange = false
        mHandler.postDelayed(ignoreAhhRunnable, 1000L)
    }

    private var ignoreLauncher = true
    private val ignoreLauncherRunnable = Runnable {
        ignoreLauncher = true
    }

    private fun setNoIgnoreLauncher(){
        mHandler.removeCallbacks(ignoreLauncherRunnable)
        ignoreLauncher = false
        mHandler.postDelayed(ignoreLauncherRunnable, 1000L)
    }

    // private val mediaSessionManager by lazy {(getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager)}
    private var volumeJob: Job? = null

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        volumeJob?.cancel()
        volumeJob = launch {
            if (event?.keyCode == KEYCODE_VOLUME_UP || event?.keyCode == KEYCODE_VOLUME_DOWN) {
                volumePressed = true
                makeAdaptive()
                delay(5000)
                volumePressed = false
                makeAdaptive()
            }
        }
        volumeJob?.start()
        return super.onKeyEvent(event)
    }


    @SuppressLint("SwitchIntDef")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (!isScreenOn.get() || !applyAdaptiveMod.get()!!) return

         /*Log.d(
             "TESTEST",
             "EVENT_TYPE ${event?.eventType} CHANGE_TYPE ${event?.contentChangeTypes} ${event?.packageName} Classname: ${event?.className}"
         )*/
        when (event?.eventType) {

            TYPE_WINDOW_STATE_CHANGED -> {//32

                if (event.contentChangeTypes != 0) return

                if (event.packageName != null && event.className != null) {

                    if (event.packageName == defaultLauncherName){
                        if (ignoreLauncher){
                            setNoIgnoreLauncher()
                            return
                        }
                    }

                    val componentName = ComponentName(
                        event.packageName.toString(),
                        event.className.toString()
                    )
                    val activityInfo = tryGetActivity(componentName)

                    val ai = packageManager.getApplicationInfo(componentName.packageName, 0)

                    if (activityInfo != null){

                        if(ai.category == CATEGORY_GAME || isPartOf(manualGameList, componentName)) {
                            useStockAdaptive = true
                            useMin60 = false
                            ignoreScrollForNonNative = false
                            makeAdaptive()
                            return
                        }

                        if(ai.category == CATEGORY_VIDEO || isPartOf(manualVideoAppList, componentName)) {
                            useMin60 = true
                            ignoreScrollForNonNative = true
                            useStockAdaptive = false
                            makeAdaptive()
                            return
                        }


                        if (isPartOf(useStockAdaptiveList, componentName))  {
                            if (UtilsDeviceInfoSt.instance(applicationContext).isLowRefreshDevice){
                                useStockAdaptive = true
                                useMin60 = false
                                ignoreScrollForNonNative = false
                                makeAdaptive()
                                return
                            }
                        }

                        if(ai.category == ApplicationInfo.CATEGORY_SOCIAL || ai.category == ApplicationInfo.CATEGORY_MAPS){
                            if (!isOfficialAdaptive) {
                                useMin60 = true
                                useStockAdaptive = false
                                ignoreScrollForNonNative = false
                                makeAdaptive()
                                return
                            }else{
                                if (UtilsDeviceInfoSt.instance(applicationContext).isLowRefreshDevice){
                                    useStockAdaptive = true
                                    useMin60 = false
                                    ignoreScrollForNonNative = false
                                    makeAdaptive()
                                    return
                                }
                            }
                        }

                        for (window in windows) {
                            if (window.isInPictureInPictureMode ||(window.type == -1 && window.root.packageName != "com.android.systemui")
                            ) {
                                if (!isOfficialAdaptive) {
                                    useMin60 = true
                                    ignoreScrollForNonNative = false
                                    useStockAdaptive = false
                                    makeAdaptive()
                                    return
                                } else {
                                    useStockAdaptive = true
                                    ignoreScrollForNonNative = false
                                    useMin60 = false
                                    makeAdaptive()
                                    return
                                }
                            }
                        }

                        ignoreScrollForNonNative = false
                        useStockAdaptive = false
                        useMin60 = false
                        makeAdaptive()//don't remove
                        return

                    }else{
                        if  (ai.category == CATEGORY_VIDEO || isPartOf(manualVideoAppList, componentName)) {
                            useMin60 = true
                            ignoreScrollForNonNative = true
                            useStockAdaptive = false
                            makeAdaptive()
                            return
                        }

                    }
                }
            }

            TYPE_VIEW_SCROLLED/*4096 */ -> {
                if (isOfficialAdaptive || !ignoreScrollForNonNative){
                    makeAdaptive()
                }
                return
            }


            TYPE_WINDOW_CONTENT_CHANGED -> {//2048
                when (event.contentChangeTypes) {

                    CONTENT_CHANGE_TYPE_SUBTREE -> {//1
                        when (event.packageName?.toString()) {

                            BuildConfig.APPLICATION_ID ->{
                                return
                            }

                            "com.android.systemui" -> {
                                if (mKeyguardManager.isDeviceLocked) {
                                    if (useStockAdaptive) {
                                        useStockAdaptive = false
                                    }
                                    return
                                }else{
                                    //Notification Panel
                                    if (isOfficialAdaptive/*UtilsDeviceInfoSt.instance(applicationContext).isLowRefreshDevice*/){
                                        makeAdaptive()
                                    }
                                    return
                                }
                            }

                            else -> {
                                if ((isOfficialAdaptive && useMin60) || UtilsDeviceInfoSt.instance(applicationContext).isLowRefreshDevice){
                                    makeAdaptive()
                                    return
                                }
                            }
                        }
                    }

                    CONTENT_CHANGE_TYPE_SUBTREE + CONTENT_CHANGE_TYPE_TEXT -> {//3
                        //When expanding notification in some cases
                        if (isOfficialAdaptive && event.packageName?.toString() == "com.android.systemui") {
                            makeAdaptive()
                            return
                        }
                    }
                }
            }

            TYPE_WINDOWS_CHANGED ->{ // 4194304
                if (ignoreNextWinChange) {
                    setTempIgnoreWinChange()
                    return
                }
                if (event.contentChangeTypes == 0){
                    makeAdaptive()
                    setTempIgnoreWinChange()
                }
            }
        }
    }


    private var makeAdaptiveJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.M)
    private fun makeAdaptive() {
        mUtilsRefreshRate.setPeakRefreshRate(prrActive.get()!!)
        if (makeAdaptiveJob != null){
            makeAdaptiveJob!!.cancel()
            makeAdaptiveJob = null
        }
        makeAdaptiveJob = launch(Dispatchers.IO) {
            delay(adaptiveDelayMillis)
            if (applyAdaptiveMod.get()!! && isScreenOn.get() && !useStockAdaptive && !cameraOpen) {
                mUtilsRefreshRate.setPeakRefreshRate(
                    if (useMin60 || volumePressed) max(60, lrrPref.get()!!) else lrrPref.get()!!
                )
            }
        }
        makeAdaptiveJob!!.start()
    }


    private fun initialAdaptive() {
        mUtilsRefreshRate.setRefreshRate(prrActive.get()!!, UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt)
        makeAdaptive()
    }


    override fun onDestroy() {
        stopHz()
        hzStatus.set(STOPPED)
        try {
            mWindowsManager!!.removeView(mLayout)
        } catch (_: java.lang.Exception) {
        }
        unregisterReceiver(mScreenStatusReceiver)
        disableNetworkCallback()
        makeAdaptiveJob?.cancel()
        masterJob.cancel()
        super.onDestroy()
    }


    override fun onInterrupt() {
    }

    @SuppressLint("ClickableViewAccessibility")
    private val adaptiveEnhancer = View.OnTouchListener { _, _ ->
        makeAdaptive()
        true
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun setupAdaptiveEnhancer(){
        launch {
            while (!isProfilesLoaded){
                delay(150)
            }
            delay(300)
            launch(Dispatchers.Main){
                if (isFakeAdaptiveValid.get()!!) {
                    mLayout?.setOnTouchListener(adaptiveEnhancer)
                    if (isScreenOn.get()) {
                        initialAdaptive()//initial trigger
                    }else{
                        if (UtilPermSt.instance(applicationContext).hasWriteSystemPerm()) {
                            mUtilsRefreshRate.setMinRefreshRate(lowestHzCurMode)
                        }
                    }
                    registerCameraCallback()
                } else {
                    if (UtilPermSt.instance(applicationContext).hasWriteSystemPerm()) {
                        if (UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt > UtilsDeviceInfoSt.instance(applicationContext).regularMinHz) {
                            mUtilsRefreshRate.setRefreshRate(prrActive.get()!!, UtilsPrefsGmhSt.instance(applicationContext).gmhPrefMinHzAdapt)
                        }else{
                            mUtilsRefreshRate.setRefreshRate(prrActive.get()!!, lowestHzForAllMode)
                        }
                    }
                    mLayout?.setOnTouchListener(null)
                    unregisterCameraCallback()
                }
            }
        }
    }

    private fun updateOverlay(newHz: Int){
        launch(Dispatchers.Main) {
            mTransfyHzJob?.cancel()
            hzText?.setTextColor(if (newHz <= 60.05) Color.RED else getColor(R.color.refresh_rate))
            hzText?.alpha = 0.85f
            hzText?.text = newHz.toString()

            mTransfyHzJob = null
            mTransfyHzJob = launch{
                delay(4000)
                hzText?.alpha = 0.25f
            }
            mTransfyHzJob?.start()
        }
    }


}