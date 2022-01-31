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
import android.content.pm.ApplicationInfo.CATEGORY_SOCIAL
import android.content.pm.ApplicationInfo.CATEGORY_VIDEO
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
import android.view.*
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
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptiveValid
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.lrrPref
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CacheSettings.restoreSync
import com.tribalfs.gmh.helpers.CacheSettings.screenOffRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.sensorOnKey
import com.tribalfs.gmh.helpers.CacheSettings.turnOffAutoSensorsOff
import com.tribalfs.gmh.hertz.*
import com.tribalfs.gmh.hertz.HzNotifGlobal.CHANNEL_ID_HZ
import com.tribalfs.gmh.hertz.HzNotifGlobal.NOTIFICATION_ID_HZ
import com.tribalfs.gmh.hertz.HzNotifGlobal.hznotificationBuilder
import com.tribalfs.gmh.netspeed.NetSpeedService.Companion.netSpeedService
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.profiles.ProfilesObj.isProfilesLoaded
import com.tribalfs.gmh.receivers.GmhBroadcastReceivers
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.*
import java.lang.Integer.max
import java.lang.Runnable
import java.util.*
import kotlin.coroutines.CoroutineContext

internal const val PLAYING = 1
internal const val STOPPED = -1
internal const val PAUSE = 0
private const val MAX_TRY = 8
//private const val TAG = "GalaxyMaxHzAccess"
// var isGMHBroadcastReceiverRegistered = false
// private val SENSORS_OFF_DELAY = if (BuildConfig.DEBUG) 5000L else 15000L

@ExperimentalCoroutinesApi
@RequiresApi(Build.VERSION_CODES.M)
class GalaxyMaxHzAccess : AccessibilityService(), CoroutineScope {

    companion object{
        internal var gmhAccessInstance: GalaxyMaxHzAccess? = null
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    private val mConnectivityManager by lazy {applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager}
    private val mKeyguardManager by lazy {applicationContext.getSystemService(KEYGUARD_SERVICE) as KeyguardManager}
    private val handler by lazy {Handler(Looper.getMainLooper())}
    private val mCameraManager by lazy {getSystemService(CAMERA_SERVICE) as CameraManager}
    private val mUtilsRefreshRate by lazy {UtilRefreshRateSt.instance(applicationContext)}
    private val mNotifBar by lazy {UtilNotifBarSt.instance(applicationContext)}
    private var triesA: Int = 0
    private var triesB: Int = 0
    private var isGameOpen = false
    private var isVideoApp = false
    private var useMin60 = false
    private var cameraOpen: Boolean = false
    private val mUtilsPrefGmh by lazy { UtilsPrefsGmhSt.instance(applicationContext) }
    private val notificationManagerCompat by lazy { NotificationManagerCompat.from(applicationContext)}
    private val mNotificationContentView by lazy { RemoteViews(applicationContext.packageName, R.layout.view_hz_notification) }
    private val mNotifIcon by lazy{ UtilNotifIcon() }
    private var mTransfyHzJob: Job? = null
    private var mPauseHzJob: Job? = null

    private val autoSensorsOffRunnable: Runnable by lazy {
        Runnable {
            if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefSensorsOff) {
                    switchSensorsOff(true)
                    //Workaround sensors off sometimes trigger action_SCREEN_ON
                    handler.postDelayed(forceLowestRunnable,1000)
            }
        }
    }

    private val forceLowestRunnable: Runnable by lazy {
       Runnable {
            if (screenOffRefreshRateMode != currentRefreshRateMode.get()) {
                CacheSettings.ignoreRrmChange = true
                if (mUtilsRefreshRate.setRefreshRateMode(screenOffRefreshRateMode!!)) {
                    mUtilsRefreshRate.setRefreshRate(lowestHzForAllMode, null)
                } else {
                    mUtilsRefreshRate.setRefreshRate(lowestHzCurMode, null)
                    CacheSettings.ignoreRrmChange = false
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
                    isScreenOn = false
                    restoreSync = false
                    disablePsm = false

                    // Workaround for AOD Bug on some device????
                    mUtilsRefreshRate.clearPeakAndMinRefreshRate()

                    if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefForceLowestSoIsOn) { handler.postDelayed(forceLowestRunnable,2000) }

                    handler.postDelayed(autoSensorsOffRunnable, 20000)

                    if (hzStatus.get() == PLAYING) {
                        mPauseHzJob?.cancel()
                        mPauseHzJob = null
                        mPauseHzJob = launch(Dispatchers.Main) {
                            delay(10000)
                            if (!isScreenOn) {
                                stopHz()
                                hzStatus.set(PAUSE)
                            }
                        }
                        mPauseHzJob?.start()
                    }
                }

                ACTION_SCREEN_ON ->{
                    isScreenOn = true

                    //handler.removeCallbacks { autoSensorsOffRunnable }
                    handler.removeCallbacksAndMessages(null)

                    launch {
                        mUtilsRefreshRate.setRefreshRate(prrActive.get()!!, mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefMinHzAdapt)
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
                    if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefSensorsOff || turnOffAutoSensorsOff) {
                        switchSensorsOff(false)
                    }
                    if (turnOffAutoSensorsOff){
                        mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefSensorsOff = false
                        turnOffAutoSensorsOff = false
                    }
                }

            }
        }
    }

    private val mScreenStatusReceiver by lazy{
        GmhBroadcastReceivers(applicationContext, mGmhBroadcastCallback, this)
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
    internal fun setupNetworkCallback(){
            disableNetworkCallback()
            if (netSpeedService != null) {
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
        mCameraManager.registerAvailabilityCallback(cameraCallback, handler)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && HzNotifGlobal.hznotificationChannel == null) {
            HzNotifGlobal.hznotificationChannel = NotificationChannel(
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
       // Log.d("TESTEST","onStartCommand called")
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
        stageView = LayoutInflater.from(this).inflate(R.layout.hz_fps, mLayout)
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
        //Log.d("TESTEST","startHz called")
        hzNotifOn.set(mUtilsPrefGmh.gmhPrefHzNotifIsOn)
        if (hzNotifOn.get() == true) {
            hznotificationBuilder?.setVisibility(VISIBILITY_PRIVATE)
            notificationManagerCompat.notify(
                NOTIFICATION_ID_HZ,
                hznotificationBuilder!!.build()
            )
        }

        hzOverlayOn = mUtilsPrefGmh.gmhPrefHzOverlayIsOn
        if (hzOverlayOn == true) {
            if (params?.gravity != mUtilsPrefGmh.gmhPrefHzPosition) {
                params?.gravity = mUtilsPrefGmh.gmhPrefHzPosition
                try {
                    mWindowsManager?.updateViewLayout(mLayout, params)
                } catch (_: Exception) {
                }
            }
            hzText?.visibility = View.VISIBLE
            hzText?.textSize  = mUtilsPrefGmh.gmhPrefHzOverlaySize
        }else{
            hzText?.visibility = View.GONE
        }

        updateRefreshRateViews(mDisplay.refreshRate.toInt())
        //updateOverlay(mDisplay.refreshRate.toInt())
        dm.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
    }

    internal fun stopHz() {
        //Log.d("TESTEST","stopHz called")
        hzStatus.set(STOPPED)
        hznotificationBuilder!!.setVisibility(Notification.VISIBILITY_SECRET)
        notificationManagerCompat.notify(
            NOTIFICATION_ID_HZ,
            hznotificationBuilder!!.build()
        )
        mTransfyHzJob?.cancel()
        hzText?.visibility = View.GONE
        notificationManagerCompat.cancel(NOTIFICATION_ID_HZ)
        dm.unregisterDisplayListener(displayListener)

    }


    private fun updateRefreshRateViews(newHz: Int){
        updateNotif(newHz.toString())
        updateOverlay(newHz)
    }



    private fun updateNotif(hzStr: String) = launch(Dispatchers.Main) {
        //Log.d(TAG, "updateNotifContent called")
        if (hzNotifOn.get()!!) {
            hznotificationBuilder?.apply {
                setSmallIcon(mNotifIcon.getIcon(hzStr, "Hz"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setCustomContentView(
                        RemoteViews(mNotificationContentView).apply {
                            setTextViewText(R.id.tvHz, getString(R.string.cur_rr_h, hzStr))
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
            if ((screenOffOnly && !isScreenOn) || !screenOffOnly) {
                //turnOnAutoSensorsOff = false
                switchSensorsOff(true)
            }
        } else {
            if (!mKeyguardManager.isKeyguardLocked) {
                switchSensorsOff(false)
            }
        }
    }



    override fun onUnbind(intent: Intent?): Boolean {
        //Log.d("TESTEST","OnUnbind called")
        gmhAccessInstance = null
        HzServiceHelperStn.instance(applicationContext).switchHz()
        return super.onUnbind(intent)
    }



    private fun isPartOf(list: List<String>, cn: ComponentName): Boolean {
        list.forEach {item ->
            item.split(":").let{
                if (cn.packageName.contains(it[0]) && (if (it.size == 2) it[1] == cn.className else true)){
                    return true
                }
            }
        }
        return false
    }


    private val manualVideoAppList by lazy {
        listOf(
            "com.amazon.avod.thirdpartyclient",
            "com.vanced.android.youtube",
            "com.cisco.webex.meetings",
            "com.sec.android.gallery3d",
            "com.google.android.apps.youtube"//Youtube Music
        )

    }

    private val manualGameList by lazy{
        listOf(
            "com.google.stadia",
            "com.valvesoftware.steamlink",
            "com.microsoft.xcloud",
            "com.microsoft.xboxone",
            "com.gameloft.android",
            "com.sec.android.app.samsungapps:com.sec.android.app.samsungapps.instantplays.InstantPlaysGameActivity"
        )
    }


    private fun tryGetActivity(componentName: ComponentName): ActivityInfo? {
        return try {
            packageManager.getActivityInfo(componentName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private var ignoreNextTWSC = false
    private val ignoreRunnable = Runnable {
        ignoreNextTWSC = false
    }

    private fun setTempIgnoreTwsc(){
        handler.removeCallbacks(ignoreRunnable)
        ignoreNextTWSC = true
        handler.postDelayed(ignoreRunnable, 2000L)
    }

    @SuppressLint("SwitchIntDef")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        /*Log.i(
            TAG,
            "EVENT_TYPE ${event?.eventType} CHANGE_TYPE ${event?.contentChangeTypes} $ ${event?.packageName} Classname: ${event?.className}"
        )*/

        if (!isScreenOn || !applyAdaptiveMod.get()!!) return

        when (event?.eventType) {

            TYPE_WINDOW_STATE_CHANGED -> {//32
                if (ignoreNextTWSC) return

                if (event.packageName != null && event.className != null) {
                    val componentName = ComponentName(
                        event.packageName.toString(),
                        event.className.toString()
                    )
                    val activityInfo = tryGetActivity(componentName)
                    if (activityInfo != null){
                        //Log.i("CurrentActivity", componentName.flattenToShortString())
                        val ai = packageManager.getApplicationInfo(componentName.packageName, 0)
                        isGameOpen = false
                        useMin60 = false
                        isVideoApp = false
                        when {
                            (ai.category == ApplicationInfo.CATEGORY_GAME || isPartOf(manualGameList, componentName)) -> {
                                isGameOpen = true
                                setTempIgnoreTwsc()
                            }

                            (ai.category == CATEGORY_VIDEO || isPartOf(manualVideoAppList, componentName)) ->{
                                useMin60 = true
                                isVideoApp = true
                                setTempIgnoreTwsc()
                            }

                            (ai.category == CATEGORY_SOCIAL && !isOfficialAdaptive) ->{
                                useMin60 = true
                                setTempIgnoreTwsc()
                            }

                        }
                        //Log.i(TAG, "TYPE_WINDOW_STATE_CHANGED: ${event.packageName} isGame:$isGameOpen isVideoApp:$isVideoApp useMin60:$useMin60")
                        makeAdaptive()
                    }
                }
            }

            TYPE_VIEW_SCROLLED/*4096 */ -> {
                if (!(!isOfficialAdaptive && isVideoApp)){
                    makeAdaptive()
                }
            }


            TYPE_WINDOW_CONTENT_CHANGED -> {//2048
                when (event.contentChangeTypes) {

                    CONTENT_CHANGE_TYPE_SUBTREE -> {//1
                        when (event.packageName?.toString()) {
                            defaultLauncherName -> {
                                return
                            }

                            "com.android.systemui" -> {
                                if (mKeyguardManager.isDeviceLocked) {
                                    if (isGameOpen) {
                                        isGameOpen = false
                                    }
                                } else {
                                    rootInActiveWindow?.findAccessibilityNodeInfosByText("open settings")?.let{
                                        // Log.i(TAG, "Notif is expanded")
                                        makeAdaptive()
                                    }
                                }
                            }

                            "com.android.settings", "com.samsung.android.app.notes" -> {
                                makeAdaptive()
                            }

                            else -> {
                                if (isOfficialAdaptive) {
                                    if (useMin60) makeAdaptive()
                                }
                            }
                        }
                    }

                    else -> { }
                }
            }
        }
    }


    private var makeAdaptiveJob: Job? = null


    @RequiresApi(Build.VERSION_CODES.M)
    //@Synchronized
    private fun makeAdaptive() {
        //Log.i(TAG, "makeAdaptive called")
        //if(resources.configuration.keyboardHidden ==  KEYBOARDHIDDEN_NO) return

        mUtilsRefreshRate.setPeakRefreshRate(prrActive.get()!!)

        if (makeAdaptiveJob != null){
            makeAdaptiveJob!!.cancel()
            makeAdaptiveJob = null
        }
        makeAdaptiveJob = launch(Dispatchers.IO) {
            delay(adaptiveDelayMillis)
            if (applyAdaptiveMod.get()!! && isScreenOn && !isGameOpen) {
                mUtilsRefreshRate.setPeakRefreshRate(
                    if (useMin60 || cameraOpen) max(60,lrrPref.get()!!) else lrrPref.get()!!
                )
            }
        }
        makeAdaptiveJob!!.start()
    }


    private fun initialAdaptive() {
        mUtilsRefreshRate.setRefreshRate(prrActive.get()!!, mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefMinHzAdapt)
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
        job.cancel()
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
                    if (isScreenOn) {
                        initialAdaptive()//initial trigger
                    }else{
                        if (UtilPermSt.instance(applicationContext).hasWriteSystemPerm()) {
                            mUtilsRefreshRate.setMinRefreshRate(lowestHzCurMode)
                        }
                    }
                    registerCameraCallback()
                } else {
                    if (UtilPermSt.instance(applicationContext).hasWriteSystemPerm()) {
                        if (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefMinHzAdapt > STANDARD_REFRESH_RATE_HZ) {
                            mUtilsRefreshRate.setRefreshRate(prrActive.get()!!,mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefMinHzAdapt)
                        }else{
                            mUtilsRefreshRate.setRefreshRate(prrActive.get()!!,lowestHzForAllMode)
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
            hzText?.setTextColor(if (newHz <= 60.05) Color.RED else Color.rgb(44,255,29))
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


    /*   private val ignoredPackages by lazy {
           listOf(
               "com.samsung.android.game",
               "com.samsung.android.plugin.dailylimits",
               "com.samsung.accessibility",
               "com.android.systemui",
               "com.samsung.android.app.cocktailbarservice",
               "com.google.android.gms",
               "com.samsung.android.spay",
               defaultKeyboard,
               defaultLauncherName,
               "com.samsung.android.sidegesturepad",
               "com.samsung.android.app.edgetouch",
               "com.xiaomi.gameboosterglobal"
           )
       }*/
}