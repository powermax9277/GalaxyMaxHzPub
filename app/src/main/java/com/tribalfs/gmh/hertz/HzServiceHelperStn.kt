package com.tribalfs.gmh.hertz

import android.content.Context
import android.content.Intent
import com.tribalfs.gmh.helpers.CacheSettings.hzStatus
import com.tribalfs.gmh.helpers.CacheSettings.isHzNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.SingletonHolder
import com.tribalfs.gmh.hertz.HzService.Companion.STOP
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.ExperimentalCoroutinesApi

class HzServiceHelperStn private constructor(context: Context) {

    private val appCtx = context.applicationContext
    private val mHzSharePref = UtilsPrefsGmh(appCtx)

    companion object : SingletonHolder<HzServiceHelperStn, Context>(::HzServiceHelperStn){
       // private const val TAG = "HzServiceHelperStn"
    }

    @ExperimentalCoroutinesApi
    fun updateHzSize(size: Int?) {
        size?.let{
            mHzSharePref.gmhPrefHzOverlaySize = it.toFloat()
            if (mHzSharePref.gmhPrefHzIsOn) { startHertz(null, null, null) }
        }
    }

    @ExperimentalCoroutinesApi
    fun updateHzGravity(gravity: Int){
        mHzSharePref.gmhPrefHzPosition = gravity
        if (mHzSharePref.gmhPrefHzIsOn) {
            startHertz(null, null, null)
        }
    }


    @ExperimentalCoroutinesApi
    fun switchOverlay(showOverlayHz: Boolean) {
      //  Log.d(TAG, "switchOverlay() called $showOverlayHz")
        mHzSharePref.gmhPrefHzOverlayIsOn = showOverlayHz
        if (mHzSharePref.gmhPrefHzIsOn)  {
            //Log.d(TAG, "startService called")
            appCtx.startService(Intent(appCtx, HzService::class.java))
        }
    }


    @ExperimentalCoroutinesApi
   // @RequiresApi(Build.VERSION_CODES.M)
    fun startHertz(isSwOn: Boolean?, showOverlayHz: Boolean?, showNotifHz: Boolean?) {
        //Log.d(TAG, "HzServiceHelper/startHertz: showHzFpsOverlay() called")
        isSwOn?.let{mHzSharePref.gmhPrefHzIsOn = it}
        showOverlayHz?.let{mHzSharePref.gmhPrefHzOverlayIsOn = showOverlayHz}
        showNotifHz?.let{
            isHzNotifOn.set(showNotifHz)
            mHzSharePref.gmhPrefHzNotifIsOn = showNotifHz
        }
        if ((isSwOn?:mHzSharePref.gmhPrefHzIsOn)
            && (mHzSharePref.gmhPrefHzOverlayIsOn || mHzSharePref.gmhPrefHzNotifIsOn) && isScreenOn
        ) {
            appCtx.startService(Intent(appCtx, HzService::class.java))
        }else{
            stopHertz()
        }
    }

    @ExperimentalCoroutinesApi
    fun stopHertz(){
        //Log.d(TAG, "HzServiceHelper/stopHertz")
        try {
            appCtx.stopService(Intent(appCtx, HzService::class.java))
        }catch(_:Exception){}
    }

    @ExperimentalCoroutinesApi
    fun isHzStop(): Boolean {
        return hzStatus.get() == STOP
    }

}






