package com.tribalfs.gmh.hertz

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.tribalfs.gmh.PAUSE
import com.tribalfs.gmh.PLAYING
import com.tribalfs.gmh.R
import com.tribalfs.gmh.STOPPED
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hzNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.hzStatus
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.UtilNotifIcon
import com.tribalfs.gmh.hertz.HzNotifGlobal.CHANNEL_ID_HZ
import com.tribalfs.gmh.hertz.HzNotifGlobal.NOTIFICATION_ID_HZ
import com.tribalfs.gmh.hertz.HzNotifGlobal.hznotificationBuilder
import com.tribalfs.gmh.hertz.HzNotifGlobal.hznotificationChannel
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


internal class HzService : Service(), CoroutineScope{

    companion object {
        private const val ANIMATION_DURATION = 700L
    }

    private val masterJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = masterJob + Dispatchers.IO

    private val notificationManagerCompat by lazy {NotificationManagerCompat.from(applicationContext)}
    private val mHzSharePref by lazy { UtilsPrefsGmhSt.instance(applicationContext) }
    private var mPauseHzJob: Job? = null
    private var mTransfyHzJob: Job? = null
    private val mNotificationContentView by lazy {RemoteViews(
        applicationContext.packageName,
        R.layout.hz_notification
    )}

    private val wm by lazy {applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager}
    private val params by lazy { WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, 0, 0,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }else{
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSPARENT
    ).apply {gravity = HzGravity.TOP_LEFT } }

    private val dm by lazy { applicationContext.getSystemService(DISPLAY_SERVICE) as DisplayManager}
    private val mDisplay by lazy { dm.getDisplay(displayId) }
    private var prevHz = 0

    private val displayListener by lazy{ object: DisplayManager.DisplayListener  {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            launch(Dispatchers.Main) {
                val curHz = mDisplay.refreshRate.toInt()
                if (prevHz != curHz) {
                    prevHz = curHz
                    updateRefreshRateViews(curHz)
                }
            }
        }
    }}

    // private val displayListener by lazy { MyDisplayListener(mDisplayChangeCallback)}

    private val stageView by lazy {LayoutInflater.from(application).inflate(
        R.layout.hz_overlay, RelativeLayout(application)
    ) as View}

    private val hzText: TextView by lazy{stageView.findViewById(R.id.tvHzBeatMain)}
    private val mNotifIcon by lazy{ UtilNotifIcon() }

    private val mScreenStatusReceiver by lazy{
        object : BroadcastReceiver(){
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceive(context: Context?, intent: Intent?) {
                when(intent?.action){
                    ACTION_SCREEN_ON ->{
                        mPauseHzJob?.cancel()
                        startHz()
                        hznotificationBuilder?.setVisibility(Notification.VISIBILITY_PRIVATE)
                        notificationManagerCompat.notify(
                            NOTIFICATION_ID_HZ,
                            hznotificationBuilder!!.build()
                        )
                    }
                    ACTION_SCREEN_OFF ->{
                        mPauseHzJob?.cancel()
                        mPauseHzJob = null
                        mPauseHzJob = launch(Dispatchers.Main){
                            delay(10000)
                            if (!isScreenOn.get()) {
                                pauseHz()
                            }
                            mPauseHzJob = null
                        }

                    }
                }
            }

        }
    }

    private var hzOn: Boolean? = null
    private var overlayOn: Boolean? = null


    @RequiresApi(Build.VERSION_CODES.M)
    private fun pauseHz(){
        dm.unregisterDisplayListener(displayListener)
        hznotificationBuilder!!.setVisibility(Notification.VISIBILITY_SECRET)
        notificationManagerCompat.notify(
            NOTIFICATION_ID_HZ,
            hznotificationBuilder!!.build()
        )
        showHzOverlayInt(false)
        hzStatus.set(PAUSE)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun startHz() {
        overlayOn = mHzSharePref.gmhPrefHzOverlayIsOn
        hzText.textSize  = mHzSharePref.gmhPrefHzOverlaySize
        params.gravity = mHzSharePref.gmhPrefHzPosition
        showHzOverlayInt(overlayOn!!)

        updateRefreshRateViews(mDisplay.refreshRate.toInt())
        dm.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        hzStatus.set(PLAYING)
    }



    override fun onBind(intent: Intent): IBinder? {
        return null
    }


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
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .setLocalOnly(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                hznotificationBuilder!!.setCustomContentView(mNotificationContentView)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate() {
        super.onCreate()
        setupNotification()
        registerScreenStatusReceiver()
    }


    private fun registerScreenStatusReceiver(){
        IntentFilter().let {
            it.addAction(ACTION_SCREEN_OFF)
            it.addAction(ACTION_SCREEN_ON)
            it.priority = 999
            registerReceiver(mScreenStatusReceiver, it)
        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {//don't change nullable intent
        startForeground(NOTIFICATION_ID_HZ, hznotificationBuilder!!.build())
        hzOn = mHzSharePref.gmhPrefHzIsOn
        if (hzOn == true) {
            hzNotifOn.set(mHzSharePref.gmhPrefHzNotifIsOn)
            startHz()
        } else {
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onDestroy() {
        hzStatus.set(STOPPED)
        showHzOverlayInt(false)
        dm.unregisterDisplayListener(displayListener)
        notificationManagerCompat.cancel(NOTIFICATION_ID_HZ)
        try {
            unregisterReceiver(mScreenStatusReceiver)
        }catch (_: java.lang.Exception){}
        try {
            wm.removeView(stageView)
        }catch (_: java.lang.Exception){}
        mPauseHzJob?.cancel()
        mPauseHzJob = null
        masterJob.cancel()
        super.onDestroy()
    }



    private fun updateNotif(hzStr: String) = launch(Dispatchers.Main) {
        //Log.d(TAG, "updateNotifContent called")
        if (hzNotifOn.get()!!) {
            hznotificationBuilder?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setSmallIcon(mNotifIcon.getIcon(hzStr, "Hz"))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setCustomContentView(RemoteViews(mNotificationContentView).apply {
                        setTextViewText(R.id.tvHz, getString(R.string.cur_rr_h, hzStr))
                    })
                }
                notificationManagerCompat.notify(
                    NOTIFICATION_ID_HZ,
                    build()
                )
            }
        }
    }



    private fun showHzOverlayInt(show: Boolean) = launch(Dispatchers.Main){
        if (show) {
            try {
                wm.removeView(stageView)
            } catch (_: Exception) {
            } finally {
                wm.addView(stageView, params)
            }
            stageView.alpha = 0f
            stageView.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION)
                .setListener(null)
            stageView.visibility = View.VISIBLE
        }else {
            try {
                stageView.visibility = View.GONE
                wm.removeView(stageView)
            } catch (_: java.lang.Exception) {
            }
        }
    }


    private fun updateOverlay(newHz: Int){
        if (overlayOn == true) {
            launch(Dispatchers.Main) {
                mTransfyHzJob?.cancel()
                hzText.setTextColor(if (newHz <= 60.05) Color.RED else Color.rgb(44,255,29))
                hzText.alpha = 0.85f
                hzText.text = newHz.toString()
                mTransfyHzJob = launch(Dispatchers.Main) {
                    delay(4000)
                    hzText.alpha = 0.25f
                    mTransfyHzJob = null
                }

            }
        }
    }


    private fun updateRefreshRateViews(newHz: Int){
        updateNotif(newHz.toString())
        updateOverlay(newHz)
    }

}

