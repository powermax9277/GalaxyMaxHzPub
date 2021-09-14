package com.tribalfs.gmh

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
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
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.*
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.AccessibilityPermission.allowAccessibility
import com.tribalfs.gmh.callbacks.AccessibilityCallback
import com.tribalfs.gmh.helpers.CacheSettings
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveAccessTimeout
import com.tribalfs.gmh.helpers.CacheSettings.adaptiveDelayMillis
import com.tribalfs.gmh.helpers.CacheSettings.applyAdaptiveMod
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptiveValid
import com.tribalfs.gmh.helpers.CacheSettings.isNsNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.isSpayInstalled
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzCurMode
import com.tribalfs.gmh.helpers.CacheSettings.lrrPref
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.CheckBlacklistApiSt
import com.tribalfs.gmh.helpers.NotificationBarSt
import com.tribalfs.gmh.helpers.UtilsRefreshRate
import com.tribalfs.gmh.hertz.HzService
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.netspeed.NetSpeedServiceHelperStn
import com.tribalfs.gmh.profiles.ProfilesObj.isProfilesLoaded
import com.tribalfs.gmh.receivers.GmhBroadcastReceivers
import kotlinx.coroutines.*
import java.lang.Runnable
import kotlin.coroutines.CoroutineContext


@ExperimentalCoroutinesApi
@SuppressLint("NewApi", "WrongConstant", "PrivateApi")
class GalaxyMaxHzAccess : AccessibilityService(), CoroutineScope {

    companion object{
        private const val TAG = "GalaxyMaxHzAccess"
        // var isGMHBroadcastReceiverRegistered = false
        // private val SENSORS_OFF_DELAY = if (BuildConfig.DEBUG) 5000L else 15000L
        private const val MAX_TRY = 8
        internal const val SETUP_ADAPTIVE = "sa"
        internal const val SETUP_NETWORK_CALLBACK = "snc"
        internal const val SWITCH_AUTO_SENSORS = "cas"
        internal const val SCREEN_OFF_ONLY = "soo"
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO
     private val connectivityManager by lazy {applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager}
    private val keyguardManager by lazy {getSystemService(KEYGUARD_SERVICE) as KeyguardManager}
    private val handler by lazy {Handler(Looper.getMainLooper())}
    private val camManager by lazy {getSystemService(CAMERA_SERVICE) as CameraManager}
    private val mUtilsRefreshRate by lazy {UtilsRefreshRate(applicationContext)}
    private val wm by lazy {applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager}
    private var triesA: Int = 0
    private var triesB: Int = 0
    private var isGameOpen = false //: AtomicBoolean = AtomicBoolean(false)
    private var isVideoApp = false //: AtomicBoolean = AtomicBoolean(false)
    private var useMin60 = false //: AtomicBoolean = AtomicBoolean(false)
    private var cameraOpen: Boolean = false

    private val paramsAdaptive by lazy {
        WindowManager.LayoutParams(
            0, 0, 0, 0,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )}

    private val sensorPrivacyService: Any? by lazy {applicationContext.getSystemService("sensor_privacy")}
    private val sensorPrivacyManager by lazy{Class.forName("android.hardware.SensorPrivacyManager")}

    private val mScreenStatusReceiver by lazy{
        GmhBroadcastReceivers(
            applicationContext,
            object : AccessibilityCallback {
                override fun onChange(userPresent: Boolean, turnOffSensors: Boolean) {
                    if (userPresent) {
                        if (isFakeAdaptiveValid.get()!!) makeAdaptive()
                        if (turnOffSensors) {
                            switchSensorsOff(false)
                        }

                    }else{
                        if (turnOffSensors && !isScreenOn) {
                            switchSensorsOff(true)
                        }
                    }
                }
            },
            this
        )
    }


    private fun switchSensorsOff(on: Boolean) {
        triesA = 0
        triesB = 0
        switchSensorsOffInner(on)
    }

    private fun switchSensorsOffInner(on: Boolean) {
        if (on != isSensorsOff() && triesA < MAX_TRY) {
            launch {
                var childs: List<AccessibilityNodeInfo>? = null
                while (childs == null && triesB < MAX_TRY) {
                    performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                    try {
                        childs = rootInActiveWindow?.findAccessibilityNodeInfosByText("sensors off")
                    } catch (_: Exception) {
                        triesB += 1
                    }
                    //if(!on) delay(100) //400 works
                }
                childs?.forEach {
                    if (it.isClickable && it.contentDescription !=null) {
                        try {
                            it.performAction(ACTION_CLICK)
                            NotificationBarSt.instance(applicationContext).collapseNotificationBar()
                        } catch (_: java.lang.Exception) {
                        }
                    }
                }
                triesA += 1
                delay(200)
                switchSensorsOffInner(on)
            }
        } else {
            // isSwitchingSensorsOn.set(false)
            return
        }
    }


    private fun isSensorsOff(): Boolean {
        return try {
            sensorPrivacyManager.getDeclaredMethod("isSensorPrivacyEnabled").invoke(sensorPrivacyService) as Boolean
        }catch(_: Exception){
            if (CheckBlacklistApiSt.instance(applicationContext).setAllowed()) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Error reading device sensors state. Reboot this device and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            false
        }
    }


    private val networkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                if (isNsNotifOn.get()!!) {
                    launch { NetSpeedServiceHelperStn.instance(applicationContext).stopService(true) }
                }
            }
            override fun onAvailable(network: Network) {
                if (isNsNotifOn.get()!!) {
                    launch { NetSpeedServiceHelperStn.instance(applicationContext).startService() }
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

 /*   private val adaptiveRunnable: Runnable by lazy {
        Runnable {
            if (applyAdaptiveMod.get()!! && isScreenOn && !isGameOpen){
                mUtilsRefreshRate.setPeakRefreshRate(
                    if (useMin60 || cameraOpen) 60 else lrrPref.get()!!)
            }
        }
    }*/

    //private val defaultKeyboard by lazy{DefaultApps.getKeyboard(applicationContext)}
    private val defaultLauncherName by lazy{DefaultApps.getLauncher(applicationContext)}
    private var adaptiveView: View? = null


    private fun setupScreenStatusReceiver(){
        IntentFilter().apply {
            addAction(ACTION_SCREEN_OFF)
            addAction(ACTION_SCREEN_ON)
            addAction(ACTION_USER_PRESENT)
            addAction(ACTION_POWER_SAVE_MODE_CHANGED)
            //addAction(ACTION_PHONE_STATE)
            priority = 999
            registerReceiver(mScreenStatusReceiver, this)
        }
    }

    private fun disableNetworkCallback(){
        try{
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }catch (_: java.lang.Exception){}
    }


    private fun setupNetworkCallback(){
        disableNetworkCallback()
        if (isNsNotifOn.get()!!) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }

    private fun registerCameraCallback(){
        try{
            unregisterCameraCallback()
        }catch (_: Exception){}
        camManager.registerAvailabilityCallback(cameraCallback, handler)
    }

    private fun unregisterCameraCallback(){
        camManager.unregisterAvailabilityCallback(cameraCallback)
    }

    private fun restartOtherServices(){
        if (isNsNotifOn.get()!!) {
            launch {
                NetSpeedServiceHelperStn.instance(applicationContext).stopService(null)
                delay(1000)
                NetSpeedServiceHelperStn.instance(applicationContext).runNetSpeed(null)
            }
        }
        if (CacheSettings.hzStatus.get() == HzService.PLAYING) {
            launch {
                HzServiceHelperStn.instance(applicationContext).stopHertz()
                HzServiceHelperStn.instance(applicationContext).startHertz(null, null, null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate() {
        setupScreenStatusReceiver()
        setupNetworkCallback()
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
       // Log.d(TAG, "onStartCommand() called")
        val extras = intent?.extras
        val setupAdaptive = extras?.get(SETUP_ADAPTIVE)
        val setupNetworkCallback = extras?.get(SETUP_NETWORK_CALLBACK)
        val switchAutoSensors = extras?.get(SWITCH_AUTO_SENSORS)
        val onScreenOffOnly = extras?.get(SCREEN_OFF_ONLY)?: true

        if (setupAdaptive == true) {
            setupAdaptiveEnhancer()
        }
        if (setupNetworkCallback == true) {
            setupNetworkCallback()
        }
        if (switchAutoSensors != null) {
            checkAutoSensorsOff(switchAutoSensors as Boolean, onScreenOffOnly as Boolean)
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onServiceConnected() {
        super.onServiceConnected()
        setupAdaptiveEnhancer()
        this.serviceInfo.apply {
            notificationTimeout = adaptiveAccessTimeout
            //Set the recommended time that interactive controls need to remain on the screen to support the user.
            interactiveUiTimeoutMillis = 0
            nonInteractiveUiTimeoutMillis = 0
        }
        restartOtherServices()
    }

    private fun checkAutoSensorsOff(switchOn:Boolean, screenOffOnly: Boolean){
        if (switchOn){
            if ((screenOffOnly && !isScreenOn) || !screenOffOnly) {
                //turnOnAutoSensorsOff = false
                switchSensorsOff(true)
            }
        } else {
            if (!keyguardManager.isKeyguardLocked) {
                switchSensorsOff(false)
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        //job.cancel()
        if (hasWriteSecureSetPerm && isSpayInstalled == false) {
            allowAccessibility(applicationContext, GalaxyMaxHzAccess::class.java, true)
        }
        return false//super.onUnbind(intent)
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
        } catch (e: PackageManager.NameNotFoundException) {
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
    @RequiresApi(Build.VERSION_CODES.M)
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
                                if (keyguardManager.isDeviceLocked) {
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
    /*    mUtilsRefreshRate.setPeakRefreshRate(prrActive.get()!!)
        handler.removeCallbacks(adaptiveRunnable)
        handler.postDelayed(adaptiveRunnable, adaptiveDelayMillis)*/

        mUtilsRefreshRate.setPeakRefreshRate(prrActive.get()!!)

        if (makeAdaptiveJob != null){
            makeAdaptiveJob!!.cancel()
            makeAdaptiveJob = null
        }
        makeAdaptiveJob = launch(Dispatchers.IO) {
            delay(adaptiveDelayMillis)
            if (applyAdaptiveMod.get()!! && isScreenOn && !isGameOpen) {
                mUtilsRefreshRate.setPeakRefreshRate(
                    if (useMin60 || cameraOpen) 60 else lrrPref.get()!!
                )
            }
        }
        makeAdaptiveJob!!.start()
    }


    private fun initialAdaptive() {
        mUtilsRefreshRate.setRefreshRate(prrActive.get()!!)
        makeAdaptive()
    }


    override fun onDestroy() {
        // Log.d(TAG, "onDestroy() called")

        try {
            wm.removeView(adaptiveView)
        } catch (_: java.lang.Exception) {
        }finally{
            adaptiveView = null
        }
        unregisterReceiver(mScreenStatusReceiver)
        disableNetworkCallback()
        restartOtherServices()
        makeAdaptiveJob?.cancel()
        job.cancel()
        super.onDestroy()
    }


    override fun onInterrupt() {
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupAdaptiveEnhancer(){
        launch {
            while (!isProfilesLoaded){
                delay(150)
            }
            delay(300)
            launch(Dispatchers.Main){
                if (isFakeAdaptiveValid.get()!!) {
                    if (adaptiveView == null) {
                        adaptiveView = View(applicationContext)
                        adaptiveView!!.setOnTouchListener { _, _ ->
                            //  Log.d(TAG, "Touch")
                            makeAdaptive()
                            true
                        }
                    }
                    try {
                        wm.removeView(adaptiveView)
                    } catch (_: Exception) {
                    } finally {
                        wm.addView(adaptiveView, paramsAdaptive)
                    }
                    if (isScreenOn) {
                        initialAdaptive()//initial trigger
                    }else{
                        mUtilsRefreshRate.setMinRefreshRate(lowestHzCurMode)
                    }
                    registerCameraCallback()
                } else {
                    mUtilsRefreshRate.setRefreshRate(prrActive.get()!!)
                    try {
                        wm.removeView(adaptiveView)
                        adaptiveView = null
                    } catch (_: java.lang.Exception) {
                    }
                    unregisterCameraCallback()
                }
            }
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