package com.tribalfs.gmh.hertz

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.tribalfs.gmh.AccessibilityPermission
import com.tribalfs.gmh.GalaxyMaxHzAccess
import com.tribalfs.gmh.R
import com.tribalfs.gmh.callbacks.ChangedStatusCallback
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hzStatus
import com.tribalfs.gmh.helpers.CacheSettings.isHzNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.CacheSettings.offScreenRefreshRate
import com.tribalfs.gmh.helpers.UtilsDeviceInfo
import com.tribalfs.gmh.receivers.ScreenStatusReceiverBasic
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.min


@ExperimentalCoroutinesApi
@SuppressLint("InlinedApi")
internal class HzService : Service(), CoroutineScope{

    companion object {
        internal const val CHANNEL_ID_HZ = "GMH"
        private const val NOTIFICATION_ID_HZ = 5
        //private const val TAG = "HzService"
        private const val ANIMATION_DURATION = 500
        internal const val PLAYING = "playing"
        internal const val STOP = "stop"
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    private val notificationManagerCompat by lazy {NotificationManagerCompat.from(applicationContext)}
    private val mHzSharePref by lazy { UtilsPrefsGmh(applicationContext) }
    private val mUtilsDeviceInfo by lazy { UtilsDeviceInfo(applicationContext) }
    private var myJob: Job? = null
    private val mNotificationContentView by lazy {RemoteViews(
        applicationContext.packageName,
        R.layout.view_hz_notification
    )}
    private val wm by lazy {applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager}
    private val params by lazy { WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, 0, 0,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = HzGravity.TOP_LEFT
    } }
    private val dm by lazy {applicationContext.getSystemService(DISPLAY_SERVICE) as DisplayManager}

    private val displayListener by lazy {MyDisplayListener(dm.getDisplay(displayId))}

    private val stageView by lazy {(LayoutInflater.from(application).inflate(
        R.layout.hz_fps, RelativeLayout(application)
    ) as View)}
    private val hzText: TextView by lazy{stageView.findViewById(R.id.tvHzBeatMain)}
    private val mIconSpeedPaint by lazy {Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }}
    private val mIconUnitPaint by lazy {Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 50f
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }}
    private val mIconBitmap: Bitmap by lazy{Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)}
    private val mIconCanvas: Canvas by lazy {Canvas(mIconBitmap)}
    private val channelNameHz: String by lazy {getString(R.string.show_refresh_rate)}
    private val mScreenStatusReceiver by lazy{
        ScreenStatusReceiverBasic(
            object : ChangedStatusCallback {
                @RequiresApi(Build.VERSION_CODES.M)
                override fun onChange(result: Any) {
                    //  Log.d(TAG, "mScreenStatusReceiver called: $screenIsOn")
                    isScreenOn = result as Boolean
                    if (result) {
                        pauseHz()
                        restartHz()
                        notificationBuilderInstance.setVisibility(Notification.VISIBILITY_PRIVATE)
                        notificationManagerCompat.notify(
                            NOTIFICATION_ID_HZ,
                            notificationBuilderInstance.build()
                        )
                    } else {
                        pauseHz()
                    }
                }
            }
        )
    }

    private var hzOn: Boolean? = null
    private var overlayOn: Boolean? = null
    private lateinit var notificationBuilderInstance: Notification.Builder


    private fun pauseHz(){
        myJob?.cancel()
        myJob = null
        myJob =  launch(Dispatchers.Main) {
            delay(7000)
            offScreenRefreshRate = "${mUtilsDeviceInfo.getRefreshRateInt()} hz"
            delay(13000)
            notificationBuilderInstance.setVisibility(Notification.VISIBILITY_SECRET)
            notificationManagerCompat.notify(
                NOTIFICATION_ID_HZ,
                notificationBuilderInstance.build()
            )

            overlayOn?.let{if (it) showHzOverlay(false)}
            dm.unregisterDisplayListener(displayListener)
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun restartHz() {
        launch {
            showHzOverlay(overlayOn!!)
            try {
                updateRefreshRateViews(dm.getDisplay(displayId).refreshRate.toInt())
                dm.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
            } catch (e: java.lang.Exception) { }
        }
        hzStatus.set(PLAYING)
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    @SuppressLint("NewApi")
    private fun setupNotification() {
        // createNotificationChannel
        NotificationChannel(
            CHANNEL_ID_HZ,
            channelNameHz,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            lightColor = Color.BLUE
            setShowBadge(false)
            vibrationPattern = longArrayOf(0)
            enableVibration(true)
            notificationManagerCompat.createNotificationChannel(this)
        }

        notificationBuilderInstance = Notification.Builder(
            applicationContext,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) CHANNEL_ID_HZ else ""
        ).setSmallIcon(R.drawable.ic_max_hz_12)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setLocalOnly(true)
            .setCustomContentView(mNotificationContentView)
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate() {
        super.onCreate()
        setupNotification()
        setupScreenStatusReceiver()

    }


    private fun setupScreenStatusReceiver(){
        if (!AccessibilityPermission.isAccessibilityEnabled(
                applicationContext,
                GalaxyMaxHzAccess::class.java
            )) {
            IntentFilter().let {
                it.addAction(Intent.ACTION_SCREEN_OFF)
                it.addAction(Intent.ACTION_SCREEN_ON)
                it.priority = 999
                registerReceiver(mScreenStatusReceiver, it)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {//don't change nullable intent
        startForeground(NOTIFICATION_ID_HZ, notificationBuilderInstance.build())
        hzOn = mHzSharePref.gmhPrefHzIsOn
        if (hzOn == true) {
            handleConfigChange()
            restartHz()
        } else {
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        hzStatus.set(STOP)
        showHzOverlay(false)
        dm.unregisterDisplayListener(displayListener)
        notificationManagerCompat.cancel(NOTIFICATION_ID_HZ)
        try {
            unregisterReceiver(mScreenStatusReceiver)
        }catch (_: java.lang.Exception){}
        // prevHz = 0
        myJob?.cancel()
        myJob = null
        super.onDestroy()
    }



    @SuppressLint("NewApi")
    private fun updateNotif(hzStr: String) = launch(Dispatchers.Main) {
        //Log.d(TAG, "updateNotifContent called")
        if (isHzNotifOn.get()!!) {
            notificationBuilderInstance.apply {
                setSmallIcon(getIndicatorIcon(hzStr))
                setCustomContentView(
                    RemoteViews(mNotificationContentView).apply {
                        setTextViewText(R.id.tvHz, getString(R.string.cur_rr_h, hzStr))
                    }
                )
                notificationManagerCompat.notify(
                    NOTIFICATION_ID_HZ,
                    build()
                )
            }
        }

    }


    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun getIndicatorIcon(speedValue: String): Icon? = withContext(Dispatchers.Default) {
        mIconSpeedPaint.textSize = 72f
        mIconSpeedPaint.textSize = min(72 * 96 / mIconSpeedPaint.measureText(speedValue), 72f)
        mIconCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        mIconCanvas.drawText(speedValue, 48f, 50f, mIconSpeedPaint)
        mIconCanvas.drawText("Hz", 48f, 90f, mIconUnitPaint)
        return@withContext Icon.createWithBitmap(mIconBitmap)
    }


    private fun showHzOverlay(show: Boolean) = launch(Dispatchers.Main){
        if (show) {
            try{
                wm.removeView(stageView)
            }catch (_: Exception){}
            finally{
                wm.addView(stageView, params)
            }
            stageView.alpha = 0f
            stageView.visibility = View.VISIBLE
            stageView.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION.toLong())
                .setListener(null)
        } else {
            stageView.visibility = View.GONE
            try {
                wm.removeView(stageView)
            } catch (_: java.lang.Exception) {
            }
        }
    }

    private fun updateOverlay(newHz: Int){
        if (overlayOn == true) {
             launch(Dispatchers.Main) {
                 hzText.setTextColor(if (newHz <= 60.05) Color.RED else Color.GREEN)
                 hzText.text = newHz.toString()
             }
        }
    }


    private fun updateRefreshRateViews(newHz: Int){
        updateNotif(newHz.toString())
        updateOverlay(newHz)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleConfigChange(){
        hzText.textSize  = mHzSharePref.gmhPrefHzOverlaySize
        isHzNotifOn.set(mHzSharePref.gmhPrefHzNotifIsOn)
        overlayOn = mHzSharePref.gmhPrefHzOverlayIsOn
        params.gravity = mHzSharePref.gmhPrefHzPosition
        //prevHz = 0
    }


    private inner class MyDisplayListener(val display: Display) : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(arg0: Int) {
            launch(Dispatchers.Main) {
                val newHz = display.refreshRate.toInt()
                updateRefreshRateViews(newHz)
            }
        }
    }


}

