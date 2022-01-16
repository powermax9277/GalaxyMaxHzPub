package com.tribalfs.gmh.tiles

import android.annotation.SuppressLint
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tribalfs.gmh.dialogs.DialogsPermissionsQs
import com.tribalfs.gmh.helpers.UtilsPermSt
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.ExperimentalCoroutinesApi

@SuppressLint("NewApi")
@ExperimentalCoroutinesApi
class QSTileHzMon : TileService() {
    /*companion object{
        private const val TAG = "QSTileHzMon"
    }*/

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }


    private fun updateTile(){
        updateTile(!HzServiceHelperStn.instance(applicationContext).isHzStop())
    }


    private fun updateTile(bool : Boolean){
        //  Log.d(TAG, "updateTile() called: $bool" )
        if (bool) qsTile.state = Tile.STATE_ACTIVE else qsTile.state = Tile.STATE_INACTIVE
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
        if (!UtilsPermSt.instance(applicationContext).hasOverlayPerm()
            && UtilsPrefsGmhSt(applicationContext).gmhPrefHzOverlayIsOn)
        {
            val mDialog = DialogsPermissionsQs.getAppearOnTopDialog(this.applicationContext)
            showDialog(mDialog)
            return
        }
        val newStat = HzServiceHelperStn.instance(applicationContext).isHzStop()
        if (newStat) qsTile.state = Tile.STATE_ACTIVE else qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
        HzServiceHelperStn.instance(applicationContext).startHertz(newStat, null, null)
        // }
    }
}