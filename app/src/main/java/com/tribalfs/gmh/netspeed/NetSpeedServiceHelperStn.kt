package com.tribalfs.gmh.netspeed

import android.content.Context
import android.content.Intent
import com.tribalfs.gmh.helpers.CacheSettings.isNetSpeedRunning
import com.tribalfs.gmh.helpers.SingletonMaker
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt


class NetSpeedServiceHelperStn private constructor(context: Context)  {

    companion object : SingletonMaker<NetSpeedServiceHelperStn, Context>(::NetSpeedServiceHelperStn){
       // private const val TAG = "NetSpeedServiceHelper"
    }

    private val appCtx =  context.applicationContext
    private val mUtilsPrefGmh by lazy  {UtilsPrefsGmhSt.instance(appCtx)}

    private val serviceIntent: Intent
        get() {
            return Intent(appCtx, NetSpeedService::class.java)
        }


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


    fun startService() {
        isNetSpeedRunning.set(true)
        appCtx.startService(serviceIntent)
    }


    
    fun stopService(isTemp: Boolean?) {
        isNetSpeedRunning.set(isTemp?:false)
        try {
            appCtx.stopService(serviceIntent)
        }catch (_:Exception){}
    }
}