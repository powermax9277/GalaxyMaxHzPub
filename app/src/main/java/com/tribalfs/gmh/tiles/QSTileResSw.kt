package com.tribalfs.gmh.tiles

import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.tribalfs.gmh.BuildConfig.APPLICATION_ID
import com.tribalfs.gmh.MyApplication.Companion.applicationName
import com.tribalfs.gmh.MyApplication.Companion.applicationScope
import com.tribalfs.gmh.R
import com.tribalfs.gmh.dialogs.DialogsPermissionsQs
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.isMultiResolution
import com.tribalfs.gmh.helpers.UtilsDeviceInfo
import com.tribalfs.gmh.helpers.UtilsPermSt.Companion.CHANGE_SETTINGS
import com.tribalfs.gmh.helpers.UtilsSettingsIntents.changeSystemSettingsIntent
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.resochanger.ResolutionChangeUtilSt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@SuppressLint("NewApi")
class QSTileResSw : TileService() {

    companion object{
        private const val TAG = "QSTileResSw"
    }

    private var prevResCat: String? = null
    private var prevMode: String? = null
    private var ignoreCallback = false


    private val propertyCallback: OnPropertyChangedCallback by lazy {
        object: OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                if (!ignoreCallback)
                    updateTile()
                else
                    ignoreCallback = false
            }
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile() //don't move to onCreate
    }



    override fun onStartListening() {
        super.onStartListening()
        updateTile()
        currentRefreshRateMode.addOnPropertyChangedCallback(propertyCallback)
    }


    override fun onStopListening() {
        super.onStopListening()
        currentRefreshRateMode.removeOnPropertyChangedCallback(propertyCallback)
    }


    override fun onClick() {
        super.onClick()
        /*    if (isTileExpired) {
                mUtilsPrefsGmh.gmhPrefExpireDialogAllowed = true
                val i = Intent(this, Class.forName("$APPLICATION_ID.MainActivity"))
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityAndCollapse(i)
            } else {*/
        applicationScope.launch {
            ignoreCallback = true
            val result = ResolutionChangeUtilSt.instance(applicationContext).changeRes(null)
            launch(Dispatchers.Main) {
                when (result) {
                    PERMISSION_GRANTED -> {
                        Log.d(TAG, "ChangeRes permitted")
                        delay(800)
                        updateTile()
                        HzServiceHelperStn.instance(applicationContext).updateHzSize(null)
                    }
                    CHANGE_SETTINGS -> {
                        try {
                            startActivityAndCollapse(changeSystemSettingsIntent)
                        } catch (ignore: Exception) {
                        }
                    }
                    PERMISSION_DENIED -> {
                        showDialog(
                            DialogsPermissionsQs.getPermissionDialog(
                                applicationContext,
                                getString(
                                    R.string.requires_ws_perm_h,
                                    applicationName,
                                    APPLICATION_ID
                                )
                            )
                        )
                    }
                }

            }
        }
    }



    @Synchronized
    private fun updateTile() {
        if (!isMultiResolution) {
            qsTile.label = getString(R.string.feat_n_a)
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()
        } else{
            updateTileInner()
        }
    }

    private fun updateTileInner() {
        Log.d(TAG, "updateTile() called")
        val resMode = UtilsDeviceInfo(applicationContext).getResoAndRefRateModeArr(currentRefreshRateMode.get())
        if (prevResCat != resMode[0]) {
            prevResCat = resMode[0]
            // if (ResoIcons.get(resMode[0]) != null) {
            ResoIcons.get(resMode[0])?.let {
                qsTile.icon = Icon.createWithResource(this, it)
                qsTile.label = resMode[1]
            }?: run {
                qsTile.icon = Icon.createWithResource(this, R.drawable.ic_res_switch_24)
                qsTile.label = "${resMode[0]} ${resMode[1]}"
            }

        }

        if (prevMode != resMode[1]) {
            prevMode = resMode[1]
            if (resMode[1] == applicationContext.getString(R.string.std_mode)) {
                qsTile.state = Tile.STATE_INACTIVE
            } else {
                qsTile.state = Tile.STATE_ACTIVE
            }
        }
        qsTile.updateTile()
    }
}