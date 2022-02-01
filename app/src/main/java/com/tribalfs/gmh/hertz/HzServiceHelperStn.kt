package com.tribalfs.gmh.hertz

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.STOPPED
import com.tribalfs.gmh.helpers.CacheSettings.hzNotifOn
import com.tribalfs.gmh.helpers.CacheSettings.hzStatus
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.SingletonMaker
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import com.tribalfs.gmh.tiles.QSTileHzMon
import kotlinx.coroutines.ExperimentalCoroutinesApi

class HzServiceHelperStn private constructor(context: Context) {

    companion object : SingletonMaker<HzServiceHelperStn, Context>(::HzServiceHelperStn){
       // private const val TAG = "HzServiceHelperStn"
    }

    private val appCtx = context.applicationContext
    private val mHzSharePref by lazy {UtilsPrefsGmhSt.instance(appCtx)}

    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    fun updateHzSize(size: Int?) {
        size?.let{
            mHzSharePref.gmhPrefHzOverlaySize = it.toFloat()
            if (mHzSharePref.gmhPrefHzIsOn) {
                if (gmhAccessInstance != null) {
                    gmhAccessInstance!!.startHz()
                }else {
                    switchHz()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    fun updateHzGravity(gravity: Int){
        mHzSharePref.gmhPrefHzPosition = gravity
        if (mHzSharePref.gmhPrefHzIsOn) {
            if (gmhAccessInstance != null) {
                gmhAccessInstance!!.startHz()
            }else {
                switchHz()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    fun switchOverlay(showOverlayHz: Boolean) {
      //  Log.d(TAG, "switchOverlay() called $showOverlayHz")
        mHzSharePref.gmhPrefHzOverlayIsOn = showOverlayHz
        if (mHzSharePref.gmhPrefHzIsOn)  {
            //Log.d(TAG, "startService called")
            if (gmhAccessInstance != null) {
                gmhAccessInstance!!.startHz()
            }else {
                startHzService()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    internal fun switchHz() {
        switchHz(null,null,null)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    internal fun switchHz(isSwOn: Boolean?, showOverlayHz: Boolean?, showNotifHz: Boolean?) {
        isSwOn?.let{mHzSharePref.gmhPrefHzIsOn = it}
        showOverlayHz?.let{mHzSharePref.gmhPrefHzOverlayIsOn = showOverlayHz}
        showNotifHz?.let{
            hzNotifOn.set(showNotifHz)
            mHzSharePref.gmhPrefHzNotifIsOn = showNotifHz
        }

        if ((isSwOn?:mHzSharePref.gmhPrefHzIsOn)
            && (mHzSharePref.gmhPrefHzOverlayIsOn || mHzSharePref.gmhPrefHzNotifIsOn) && isScreenOn
        ) {
            if (gmhAccessInstance != null) {
                gmhAccessInstance!!.startHz()
            }else{
                startHzService()
            }

        }else{
            if (gmhAccessInstance != null) {
                gmhAccessInstance!!.stopHz()
            }else{
                stopHzService()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(appCtx, ComponentName(appCtx, QSTileHzMon::class.java))
        }
    }

    private fun startHzService(){
        try {
            appCtx.startService(Intent(appCtx, HzService::class.java))
        }catch(_:Exception){}

    }

    internal fun stopHzService(){
        try {
            appCtx.stopService(Intent(appCtx, HzService::class.java))
        }catch(_:Exception){}

    }


    fun isHzServiceStopped(): Boolean {
        return hzStatus.get() == STOPPED
    }

}






