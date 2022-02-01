package com.tribalfs.gmh.tiles

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.MyApplication.Companion.applicationScope
import com.tribalfs.gmh.R
import com.tribalfs.gmh.UtilAccessibilityService.allowAccessibility
import com.tribalfs.gmh.dialogs.QSDialogs
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.isSpayInstalled
import com.tribalfs.gmh.helpers.CacheSettings.lrrPref
import com.tribalfs.gmh.helpers.CacheSettings.minHzListForAdp
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.REFRESH_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.helpers.STANDARD_REFRESH_RATE_HZ
import com.tribalfs.gmh.helpers.UtilRefreshRateSt
import com.tribalfs.gmh.helpers.UtilTileIcon
import com.tribalfs.gmh.sharedprefs.NOT_USING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
@RequiresApi(Build.VERSION_CODES.N)
class QSTileMinHz : TileService() {

    private val mUtilsRefreshRate by lazy{UtilRefreshRateSt.instance(applicationContext)}
    private val mUtilTileIcon = UtilTileIcon()

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }


    override fun onStartListening() {
        super.onStartListening()
        updateTile()

    }


    @Synchronized
    private fun updateTile() {
        if (minHzListForAdp?.size?:0 < 2) {
            qsTile.label = getString(R.string.feat_n_a)
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()
        } else{
            updateTileInner()
        }
    }

    @Synchronized
    private fun updateTileInner() {
        qsTile.icon = mUtilTileIcon.getIcon(lrrPref.get().toString(),"Min")
        qsTile.label = "${getString(R.string.adp_min_hz)}:${lrrPref.get()}"

        if ((isOfficialAdaptive ||isPremium.get() == true) && currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS){
            qsTile.state = Tile.STATE_ACTIVE
        }else{
            qsTile.state = Tile.STATE_UNAVAILABLE
        }

        qsTile.updateTile()
    }



    override fun onClick() {
        super.onClick()

        applicationScope.launch(Dispatchers.Main) {
            var idx = minHzListForAdp?.indexOf(lrrPref.get())
            var nexMinHzTemp = 500
            while (nexMinHzTemp >= prrActive.get()!!
                && nexMinHzTemp != minHzListForAdp?.minOrNull()
            ) {
                if (idx != null && idx < (minHzListForAdp?.size ?: 1) - 1) {
                    nexMinHzTemp = minHzListForAdp!![idx + 1]
                    idx += 1
                } else {
                    nexMinHzTemp = minHzListForAdp!![0]
                    idx = 1
                }
            }
            var nexMinHz = nexMinHzTemp

            if (isOfficialAdaptive && nexMinHz < STANDARD_REFRESH_RATE_HZ) {
                if (isPremium.get() == true) {
                    if (!checkAccessibilityPerm()) {
                        nexMinHz = STANDARD_REFRESH_RATE_HZ
                    }
                }else{
                    nexMinHz = STANDARD_REFRESH_RATE_HZ
                }
            }

            if (!isOfficialAdaptive){
                if (isPremium.get() == true ) {
                    if (!checkAccessibilityPerm()) {
                        return@launch
                    }
                }else{
                    return@launch
                }
            }

            mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefMinHzAdapt = nexMinHz

            mUtilsRefreshRate.applyMinHz()

            gmhAccessInstance?.setupAdaptiveEnhancer()

        }
    }


    private fun checkAccessibilityPerm(): Boolean{
        return if (
            gmhAccessInstance == null
        ){
            if (hasWriteSecureSetPerm && (isSpayInstalled == false ||  mUtilsRefreshRate.mUtilsPrefsGmh.hzPrefSPayUsage == NOT_USING)) {
                allowAccessibility(
                    applicationContext,
                    true
                )
                true
            }else{
                CoroutineScope(Dispatchers.Main).launch {
                    showDialog(QSDialogs.getAllowAccessDialog(applicationContext))
                }
                false
            }
        }else{
            true
        }
    }

}