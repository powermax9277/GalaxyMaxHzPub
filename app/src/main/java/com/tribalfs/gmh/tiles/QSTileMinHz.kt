package com.tribalfs.gmh.tiles

import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.tribalfs.gmh.BuildConfig
import com.tribalfs.gmh.MyApplication
import com.tribalfs.gmh.MyApplication.Companion.applicationScope
import com.tribalfs.gmh.R
import com.tribalfs.gmh.dialogs.DialogsPermissionsQs
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isPremium
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.lrrPref
import com.tribalfs.gmh.helpers.CacheSettings.minHzListForAdp
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlin.math.max


@ExperimentalCoroutinesApi
@SuppressLint("NewApi")
class QSTileMinHz : TileService() {

    companion object{
        // private const val TAG = "QSTileMinHz"
    }

    private var prevLrr: Int? = null
    private var prevMode: String? = null
    private val mUtilsPrefsGmh by lazy{UtilsPrefsGmh(applicationContext)}

    private val propertyCallback: OnPropertyChangedCallback by lazy {
        object: OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                updateTile()
            }
        }
    }


    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }


    override fun onStartListening() {
        super.onStartListening()
        // Log.d(TAG, "onStartListening() called" )
        updateTile()
        if (minHzListForAdp?.size?:0 > 1){
            currentRefreshRateMode.addOnPropertyChangedCallback(propertyCallback)
            lrrPref.addOnPropertyChangedCallback(propertyCallback)
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        if (minHzListForAdp?.size?:0 > 1) {
            currentRefreshRateMode.removeOnPropertyChangedCallback(propertyCallback)
            lrrPref.removeOnPropertyChangedCallback(propertyCallback)
        }
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

        if (prevLrr != lrrPref.get()) {
            prevLrr = lrrPref.get()
            if (HzIcons.get(prevLrr!!) != null) {
                qsTile.icon = Icon.createWithResource(this, HzIcons.get(prevLrr!!)!!)
                qsTile.label = getString(R.string.adp_min_hz)
            } else {
                qsTile.label = "$lrrPref ${getString(R.string.adp_min_hz)}"
            }
        }

        if (isPremium.get() == true) {
            if (prevMode != currentRefreshRateMode.get()) {
                prevMode = currentRefreshRateMode.get()
                if (currentRefreshRateMode.get() == REFRESH_RATE_MODE_SEAMLESS) {
                    qsTile.state = Tile.STATE_ACTIVE
                } else {
                    qsTile.state = Tile.STATE_INACTIVE
                }
            }
        }else{
            qsTile.state = Tile.STATE_UNAVAILABLE
        }

        qsTile.updateTile()
    }


    override fun onClick() {
        super.onClick()
        if (hasWriteSecureSetPerm) {
            applicationScope.launch {
                val idx = minHzListForAdp?.indexOf(lrrPref.get())
                val nexMinHz =
                    if (idx != null && idx < (minHzListForAdp?.size ?: 1) - 1) {
                        minHzListForAdp!![idx + 1]
                    } else {
                        minHzListForAdp!![0]
                    }
                lrrPref.set(max(lowestHzForAllMode, nexMinHz))
                mUtilsPrefsGmh.gmhPrefMinHzAdapt = nexMinHz
            }
        }else{
            showDialog(
                DialogsPermissionsQs.getPermissionDialog(
                    applicationContext,
                    getString(
                        R.string.requires_ws_perm_h,
                        MyApplication.applicationName,
                        BuildConfig.APPLICATION_ID
                    )
                )
            )
        }
    }
}