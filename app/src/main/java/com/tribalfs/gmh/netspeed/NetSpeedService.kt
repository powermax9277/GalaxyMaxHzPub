package com.tribalfs.gmh.netspeed

import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.CacheSettings.isOnePlus
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.UtilNotifIcon
import com.tribalfs.gmh.helpers.UtilSettingsIntents.dataUsageSettingsIntent
import com.tribalfs.gmh.netspeed.SpeedCalculator.Companion.mCalcInBits
import com.tribalfs.gmh.sharedprefs.BIT_PER_SEC
import com.tribalfs.gmh.sharedprefs.DOWNLOAD_SPEED
import com.tribalfs.gmh.sharedprefs.TOTAL_SPEED
import com.tribalfs.gmh.sharedprefs.UPLOAD_SPEED
import kotlinx.coroutines.*
import java.lang.Runnable
import java.lang.String.format
import java.util.*
import kotlin.coroutines.CoroutineContext

internal const val CHANNEL_ID_NET_SPEED = "NSI"
private const val NOTIFICATION_ID_NET_SPEED = 7
private const val UPDATE_INTERVAL = 1200L
internal const val EXTRA_STREAM = "es"
internal const val EXTRA_SPEED_UNIT = "eu"


class NetSpeedService : Service(), CoroutineScope {

    companion object{
        //even with volatile this does not reflects real time
        internal var netSpeedService: NetSpeedService? = null
    }

    private val mNotificationContentView: RemoteViews by lazy {RemoteViews(applicationContext.packageName, R.layout.ns_notification) }
    private val notificationManagerCompat by lazy{NotificationManagerCompat.from(applicationContext)}
    private val mNotifIcon by lazy{ UtilNotifIcon() }
    private val mHandler by lazy {Handler(Looper.getMainLooper())}
    private lateinit var notificationBuilderInstance: Notification.Builder

    private var mSpeedToShow = TOTAL_SPEED
    private var mLastRxBytes: Long = 0
    private var mLastTxBytes: Long = 0
    private var mLastTime: Long = 0


    private val masterJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = masterJob + Dispatchers.IO


    private var continueMeasureNetStat: Boolean = false

    private val measureNetStat
        get() = launch {
            while (continueMeasureNetStat) {
                val currentRxBytes: Long = TrafficStats.getTotalRxBytes()
                val currentTxBytes: Long = TrafficStats.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()

                val usedRxdHBytes = currentRxBytes - mLastRxBytes
                val usedTxdHBytes = currentTxBytes - mLastTxBytes
                val usedTime = currentTime - mLastTime

                if ((usedRxdHBytes > 10 || usedRxdHBytes > 10) && usedTime > 0) {
                    SpeedCalculator.instance(applicationContext).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            updateNotification(
                                getSpeed(usedTime,usedTxdHBytes)/*up*/,
                                getSpeed(usedTime,usedRxdHBytes)/*down*/,
                                getSpeed(usedTime,usedRxdHBytes + usedTxdHBytes)
                            )
                        }
                    }
                    mLastRxBytes = currentRxBytes
                    mLastTxBytes = currentTxBytes
                    mLastTime = currentTime
                }

                delay(UPDATE_INTERVAL)
            }
        }


    private fun startNetStatInternal(){
        continueMeasureNetStat = true
        measureNetStat.start()
    }


    private fun stopNetStatInternal(){
        continueMeasureNetStat = false
        measureNetStat.cancel()
    }


    private val pauseNetStatRunnable = Runnable {
        if (!isScreenOn.get()) {
            stopNetStatInternal()
            notificationBuilderInstance.setVisibility(Notification.VISIBILITY_SECRET)
            notificationManagerCompat.notify(
                NOTIFICATION_ID_NET_SPEED,
                notificationBuilderInstance.build()
            )
        }
    }

    private val mScreenStatusReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        mHandler.removeCallbacks(pauseNetStatRunnable)
                        startNetStatInternal()
                        notificationBuilderInstance.setVisibility(Notification.VISIBILITY_PRIVATE)
                        notificationManagerCompat.notify(
                            NOTIFICATION_ID_NET_SPEED,
                            notificationBuilderInstance.build()
                        )
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        mHandler.postDelayed(pauseNetStatRunnable, 7000)
                    }
                }

            }
        }
    }

    
    private fun setupScreenStatusReceiver(){
         IntentFilter().let {
            it.addAction(Intent.ACTION_SCREEN_OFF)
            it.addAction(Intent.ACTION_SCREEN_ON)
            it.priority = 999
            registerReceiver(mScreenStatusReceiver, it)
        }
    }


    private fun setupNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (NotificationChannel(
                CHANNEL_ID_NET_SPEED,
                getString(R.string.show_ns),
                NotificationManager.IMPORTANCE_LOW
            )).apply {
                lightColor = Color.BLUE
                lockscreenVisibility = Notification.VISIBILITY_SECRET
                setShowBadge(false)
                vibrationPattern = longArrayOf(0)
                enableVibration(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
                notificationManagerCompat.createNotificationChannel(this)
            }
        }

        val pendingIntent: PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(applicationContext, 0, dataUsageSettingsIntent, FLAG_IMMUTABLE)
        } else {
            null
        }


        notificationBuilderInstance = (if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Notification.Builder(applicationContext, CHANNEL_ID_NET_SPEED)
        }else{
            @Suppress("DEPRECATION")
            Notification.Builder(applicationContext)
        })
        notificationBuilderInstance.apply {
            setSmallIcon(R.drawable.ic_speed_12)
            setOngoing(true)
            setOnlyAlertOnce(true)
            setCategory(Notification.CATEGORY_STATUS)
            setVisibility(Notification.VISIBILITY_PRIVATE)
            setLocalOnly(true)
            pendingIntent?.let{if (!isOnePlus) {setContentIntent(it)}}
            setAutoCancel(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setCustomContentView(mNotificationContentView)
            }
        }

    }


    override fun onCreate() {
        super.onCreate()
        netSpeedService = this
        setupNotification()
        setupScreenStatusReceiver()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {//keep intent nullable for system restart
        intent?.getIntExtra(EXTRA_STREAM, TOTAL_SPEED)?.let{setStream(it)}
        intent?.getIntExtra(EXTRA_SPEED_UNIT, BIT_PER_SEC)?.let{setSpeedUnit(it)}
        startForeground(NOTIFICATION_ID_NET_SPEED, notificationBuilderInstance.build())
        startNetStatInternal()
        return START_STICKY
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        netSpeedService = null
        stopNetStatInternal()
        notificationManagerCompat.cancel(NOTIFICATION_ID_NET_SPEED)
        try {
            unregisterReceiver(mScreenStatusReceiver)
        }catch(_: java.lang.Exception){}
        masterJob.cancel()
        stopForeground(true)
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun updateNotification(
        up: SpeedCalculator.Speed.SpeedDetails,
        down: SpeedCalculator.Speed.SpeedDetails,
        total: SpeedCalculator.Speed.SpeedDetails
    ) = withContext(Dispatchers.Main) {
        notificationBuilderInstance.apply {

            when (mSpeedToShow) {
                TOTAL_SPEED ->{
                    setSmallIcon(
                        mNotifIcon.getIcon(
                            total.speedValue!!,
                            total.speedUnit!!
                        )
                    )
                }
                DOWNLOAD_SPEED ->{
                    setSmallIcon(
                        mNotifIcon.getIcon(
                            down.speedValue!!,
                            down.speedUnit!!
                        )
                    )
                }
                UPLOAD_SPEED ->{
                    setSmallIcon(
                        mNotifIcon.getIcon(
                            up.speedValue!!,
                            up.speedUnit!!
                        )
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setCustomContentView(RemoteViews(mNotificationContentView).apply {
                    setTextViewText(
                        R.id.notificationTextDl,
                        format(
                            Locale.ENGLISH,
                            applicationContext.getString(R.string.notif_sp_dl),
                            down.speedValue,
                            down.speedUnit
                        )
                    )
                    setTextViewText(
                        R.id.notificationTextUl,
                        format(
                            Locale.ENGLISH,
                            applicationContext.getString(R.string.notif_sp_ul),
                            up.speedValue,
                            up.speedUnit
                        )
                    )
                    setTextViewText(
                        R.id.notificationTextTot,
                        format(
                            Locale.ENGLISH,
                            applicationContext.getString(R.string.notif_sp_cb),
                            total.speedValue,
                            total.speedUnit
                        )
                    )
                }
                )
            }

            notificationManagerCompat.notify(
                NOTIFICATION_ID_NET_SPEED, build())
        }
    }


    fun setStream(stream: Int){
        mSpeedToShow = stream

    }

    fun setSpeedUnit(speedUnit: Int){
        mCalcInBits = speedUnit == BIT_PER_SEC
    }


}