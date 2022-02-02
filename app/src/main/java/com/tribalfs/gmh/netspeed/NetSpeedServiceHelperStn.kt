package com.tribalfs.gmh.netspeed

import android.content.Context
import android.content.Intent
import android.os.Build
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.helpers.SingletonMaker
import com.tribalfs.gmh.netspeed.NetSpeedService.Companion.netSpeedService
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt


internal class NetSpeedServiceHelperStn private constructor(context: Context)  {

    companion object : SingletonMaker<NetSpeedServiceHelperStn, Context>(::NetSpeedServiceHelperStn)

    private val appCtx =  context.applicationContext
    private val mUtilsPrefGmh by lazy  {UtilsPrefsGmhSt.instance(appCtx)}


    fun updateStreamType(){
        netSpeedService?.setStream(mUtilsPrefGmh.gmhPrefSpeedToShow)
    }

    fun updateSpeedUnit(){
        netSpeedService?.setSpeedUnit(mUtilsPrefGmh.gmhPrefSpeedUnit)

    }


    fun updateNetSpeed(){
        runNetSpeed(null)
    }

    fun runNetSpeed(enable: Boolean?){
        mUtilsPrefGmh.gmhPrefNetSpeedIsOn = enable ?: mUtilsPrefGmh.gmhPrefNetSpeedIsOn
        if (mUtilsPrefGmh.gmhPrefNetSpeedIsOn ) {
            startNetSpeed()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                gmhAccessInstance?.setupNetworkCallback(true)
            }
        } else {
            stopNetSpeed()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                gmhAccessInstance?.setupNetworkCallback(false)
            }
        }
    }


    //This is also called by network callback so don't register callback here
    fun startNetSpeed() {
        if (netSpeedService == null) {
            val i = Intent(appCtx, NetSpeedService::class.java)
            i.putExtra(EXTRA_STREAM, mUtilsPrefGmh.gmhPrefSpeedToShow)
            i.putExtra(EXTRA_SPEED_UNIT, mUtilsPrefGmh.gmhPrefSpeedUnit)
            appCtx.startService(i)
        }
    }

    //This is also called by network callback so don't register callback here
    fun stopNetSpeed() {
        try {
            if (netSpeedService != null) {
                appCtx.stopService(Intent(appCtx, NetSpeedService::class.java))
            }
        } catch (_: Exception) {
        }
    }
}