package com.tribalfs.gmh.netspeed

import android.content.Context
import android.content.Intent
import com.tribalfs.gmh.helpers.CacheSettings.isNetSpeedRunning
import com.tribalfs.gmh.helpers.SingletonHolder
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.ExperimentalCoroutinesApi


class NetSpeedServiceHelperStn private constructor(context: Context)  {

    companion object : SingletonHolder<NetSpeedServiceHelperStn, Context>(::NetSpeedServiceHelperStn){
       // private const val TAG = "NetSpeedServiceHelper"
    }

    private val appCtx =  context.applicationContext
    private val mUtilsPrefGmh =  UtilsPrefsGmh(appCtx)

    @ExperimentalCoroutinesApi
    private val serviceIntent: Intent
        get() {
            return Intent(appCtx, NetSpeedService::class.java)
        }


    @ExperimentalCoroutinesApi
    fun runNetSpeed(enable: Boolean?){
        enable?.let{
            mUtilsPrefGmh.gmhPrefNetSpeedIsOn = it
            if (it) {
                startService()
            } else {
                stopService(null)
            }
        }?:run {
            if (mUtilsPrefGmh.gmhPrefNetSpeedIsOn) {
                startService()
            } else {
                stopService(null)
            }
        }

    }


    @ExperimentalCoroutinesApi
    fun startService() {
        isNetSpeedRunning.set(true)
        appCtx.startService(serviceIntent)
    }


    @ExperimentalCoroutinesApi
    fun stopService(isTemp: Boolean?) {
        isNetSpeedRunning.set(isTemp?:false)
        try {
            appCtx.stopService(serviceIntent)
        }catch (_:Exception){}
    }
}