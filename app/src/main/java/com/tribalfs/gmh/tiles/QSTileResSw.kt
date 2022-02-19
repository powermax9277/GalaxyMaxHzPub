package com.tribalfs.gmh.tiles

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.MyApplication.Companion.appScopeIO
import com.tribalfs.gmh.R
import com.tribalfs.gmh.dialogs.QSDialogs
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isMultiResolution
import com.tribalfs.gmh.helpers.UtilPermSt.Companion.CHANGE_SETTINGS
import com.tribalfs.gmh.helpers.UtilSettingsIntents.changeSystemSettingsIntent
import com.tribalfs.gmh.hertz.HzServiceHelperStn
import com.tribalfs.gmh.resochanger.ResolutionChangeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@RequiresApi(Build.VERSION_CODES.N)
class QSTileResSw : TileService() {

    private val mUtilsPermSt by lazy {UtilPermSt.instance(applicationContext)}
    private val mUtilTileIcon = UtilTileIcon()

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile() //don't move to onCreate
    }



    override fun onStartListening() {
        super.onStartListening()
        updateTile()

    }



    override fun onClick() {
        super.onClick()

        appScopeIO.launch {

            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivityAndCollapse(startMain)
            delay(500)

            val result = ResolutionChangeUtil(applicationContext).changeRes(null)

            launch(Dispatchers.Main) {

                when (result) {
                    PERMISSION_GRANTED -> {
                        // Log.d(TAG, "ChangeRes permitted")
                        delay(2000)
                        HzServiceHelperStn.instance(applicationContext).updateHzSize(null)
                    }
                    CHANGE_SETTINGS -> {
                        try {
                            startActivityAndCollapse(changeSystemSettingsIntent)
                        } catch (ignore: Exception) {
                        }
                    }
                    PERMISSION_DENIED -> {
                        try {
                            showDialog(
                                QSDialogs.getPermissionDialog(
                                    applicationContext
                                )
                            )
                        }catch(_:Exception){
                            /*Investigate why it causing
                            "android.view.WindowManager$BadTokenException: Unable to add window --
                            token android.os.BinderProxy@c53e3e1 is not valid; is your activity running?"
                             on few devices
                             */
                        }
                    }
                    else ->{
                        hasWriteSecureSetPerm = mUtilsPermSt.hasWriteSecurePerm()}
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
        val reso = UtilsDeviceInfoSt.instance(applicationContext).getDisplayResolution()//  getResoAndRefRateModeArr(currentRefreshRateMode.get())
        val resoCat = UtilsReso.getName(
            reso.height,
            reso.width
        )

        val lastChar = resoCat.takeLast(1)
        var topStr = resoCat
        var bottomStr = ""
        if (lastChar == "+"){
            bottomStr = lastChar
            topStr = topStr.dropLast(1)
        }

        val mode = when (currentRefreshRateMode.get()) {
            REFRESH_RATE_MODE_SEAMLESS -> applicationContext.getString(R.string.adp_mode)
            REFRESH_RATE_MODE_STANDARD -> applicationContext.getString(R.string.std_mode)
            REFRESH_RATE_MODE_ALWAYS -> applicationContext.getString(R.string.high_mode)
            else -> "?"
        }

        qsTile.label = "$resoCat $mode"
        qsTile.icon = mUtilTileIcon.getIcon(topStr, bottomStr)


        if (mode == applicationContext.getString(R.string.std_mode)) {
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            qsTile.state = Tile.STATE_ACTIVE
        }

        qsTile.updateTile()
    }
}