package com.tribalfs.gmh.tiles

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.dialogs.QSDialogs
import com.tribalfs.gmh.helpers.UtilPermSt
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.ExperimentalCoroutinesApi

@RequiresApi(Build.VERSION_CODES.N)
@ExperimentalCoroutinesApi
class QSTileHzMon : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }



    private fun updateTile(){
        val isOn = !HzServiceHelperStn.instance(applicationContext).isHzServiceStopped()
        if (isOn) qsTile.state = Tile.STATE_ACTIVE else qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }


    override fun onClick() {
        super.onClick()
        if (gmhAccessInstance == null && !UtilPermSt.instance(applicationContext).hasOverlayPerm()
            && UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHzOverlayIsOn
        ) {
            val mDialog = QSDialogs.getAppearOnTopDialog(this.applicationContext)
            showDialog(mDialog)
            return
        }

        val newStat = HzServiceHelperStn.instance(applicationContext).isHzServiceStopped()

        HzServiceHelperStn.instance(applicationContext).switchHz(newStat, null, null)

    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}