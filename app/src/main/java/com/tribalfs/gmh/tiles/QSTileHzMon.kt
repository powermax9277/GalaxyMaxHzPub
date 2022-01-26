package com.tribalfs.gmh.tiles

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.GalaxyMaxHzAccess.Companion.gmhAccessInstance
import com.tribalfs.gmh.dialogs.QSDialogs
import com.tribalfs.gmh.helpers.UtilsPermSt
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
        /* if (isTileExpired){
             mUtilsPrefsGmh.gmhPrefExpireDialogAllowed = true
             val i = Intent(this, Class.forName("$APPLICATION_ID.MainActivity"))
             i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
             startActivityAndCollapse(i)

         }else {*/
        if (gmhAccessInstance == null && !UtilsPermSt.instance(applicationContext).hasOverlayPerm()
            && UtilsPrefsGmhSt.instance(applicationContext).gmhPrefHzOverlayIsOn)
        {
            val mDialog = QSDialogs.getAppearOnTopDialog(this.applicationContext)
            showDialog(mDialog)
            return
        }

        val newStat = HzServiceHelperStn.instance(applicationContext).isHzServiceStopped()

        HzServiceHelperStn.instance(applicationContext).switchHz(newStat, null, null)
        // }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}