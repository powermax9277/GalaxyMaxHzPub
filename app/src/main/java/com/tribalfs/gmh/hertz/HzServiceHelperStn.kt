package com.tribalfs.gmh.hertz

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.tribalfs.gmh.helpers.CacheSettings.hzStatus
import com.tribalfs.gmh.helpers.CacheSettings.isHzNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.SingletonMaker
import com.tribalfs.gmh.hertz.HzService.Companion.DESTROYED
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import com.tribalfs.gmh.tiles.QSTileHzMon
import kotlinx.coroutines.ExperimentalCoroutinesApi

class HzServiceHelperStn private constructor(context: Context) {

    companion object : SingletonMaker<HzServiceHelperStn, Context>(::HzServiceHelperStn){
       // private const val TAG = "HzServiceHelperStn"
    }

    private val appCtx = context.applicationContext
    private val mHzSharePref by lazy {UtilsPrefsGmhSt(appCtx)}
    @ExperimentalCoroutinesApi
    private val qsHzMonTileComponent = ComponentName(appCtx, QSTileHzMon::class.java)

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(appCtx, qsHzMonTileComponent)
        }
    }


    private fun stopHertz(){
        //Log.d(TAG, "HzServiceHelper/stopHertz")
        try {
            appCtx.stopService(Intent(appCtx, HzService::class.java))
        }catch(_:Exception){}

    }


    fun isHzStop(): Boolean {
        return hzStatus.get() == DESTROYED
    }

}






