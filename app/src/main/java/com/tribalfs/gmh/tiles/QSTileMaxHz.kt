package com.tribalfs.gmh.tiles

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.BuildConfig
import com.tribalfs.gmh.MyApplication.Companion.applicationScope
import com.tribalfs.gmh.R
import com.tribalfs.gmh.dialogs.ADB_SETUP_LINK
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.supportedHzIntCurMod
import com.tribalfs.gmh.helpers.UtilsChangeMaxHz.Companion.CHANGE_MODE
import com.tribalfs.gmh.helpers.UtilsChangeMaxHz.Companion.CHANGE_RES
import com.tribalfs.gmh.helpers.UtilsChangeMaxHz.Companion.NO_CONFIG_LOADED
import com.tribalfs.gmh.helpers.UtilsChangeMaxHz.Companion.POWER_SAVINGS
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE_ALWAYS
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.helpers.UtilsPermSt.Companion.CHANGE_SETTINGS
import com.tribalfs.gmh.helpers.UtilsSettingsIntents.changeSystemSettingsIntent
import com.tribalfs.gmh.helpers.UtilsSettingsIntents.displaySettingsIntent
import com.tribalfs.gmh.helpers.UtilsSettingsIntents.motionSmoothnessSettingsIntent
import com.tribalfs.gmh.helpers.UtilsSettingsIntents.powerSavingModeSettingsIntent
import com.tribalfs.gmh.profiles.ProfilesObj.loadComplete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.N)
class QSTileMaxHz : TileService() {

    private val mUtilsRefreshRate: UtilsRefreshRateSt by lazy { UtilsRefreshRateSt.instance(applicationContext) }

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
            when (currentRefreshRateMode.get()) {//mUtilsDeviceInfo.getSamRefreshRateMode()){
                REFRESH_RATE_MODE_STANDARD -> {
                    qsTile.icon = TileIcons.getIcon(
                        "STD",
                        ""
                    )//Icon.createWithResource(this, R.drawable.ic_baseline_std_24)
                    qsTile.label = getString(R.string.std)
                    qsTile.state = Tile.STATE_INACTIVE
                }

                REFRESH_RATE_MODE_SEAMLESS -> {
                    val prr = mUtilsRefreshRate.getPeakRefreshRate()
                    qsTile.icon = TileIcons.getIcon("$prr", "Adp")
                    qsTile.label =
                        "${getString(R.string.adaptive)} ${getString(R.string.max_hz)}: $prr"
                    qsTile.state = Tile.STATE_ACTIVE
                }

                REFRESH_RATE_MODE_ALWAYS -> {
                    val prr = mUtilsRefreshRate.getPeakRefreshRate()
                    qsTile.state = Tile.STATE_ACTIVE
                    qsTile.icon = TileIcons.getIcon(prr.toString(), getString(R.string.high))
                    qsTile.label =
                        "${getString(R.string.max_hz)} ${getString(R.string.high)}: $prr "
                }

                else -> {
                    val prr = mUtilsRefreshRate.getPeakRefreshRate()
                    qsTile.icon = TileIcons.getIcon(prr.toString(), "?")
                    qsTile.label = "${getString(R.string.max_hz)}:${prr} ?"
                    qsTile.state = Tile.STATE_INACTIVE
                }
            }
            qsTile.updateTile()
    }


    override fun onClick() {
        super.onClick()

        //Log.d(TAG, "onClick() called")

        /* if (isTileExpired) {
                mUtilsPrefsGmh.gmhPrefExpireDialogAllowed = true
                val i = Intent(this, Class.forName("${APPLICATION_ID}.MainActivity"))
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivityAndCollapse(i)
            } else {*/


        applicationScope.launch(Dispatchers.Main) {

            when (UtilsChangeMaxHz(applicationContext).changeMaxHz(null)) {
                PERMISSION_GRANTED -> {
                    //updateTile()
                }

                CHANGE_MODE -> {
                    showDialog(getChangeRefreshRateModeDialog(false))
                }

                POWER_SAVINGS -> {
                    showDialog(getChangeRefreshRateModeDialog(true))
                    return@launch
                }

                CHANGE_RES -> {
                    if (supportedHzIntCurMod != null && supportedHzIntCurMod?.size!! > 1) {
                        showDialog(
                            getChangeResDialog(
                                        mUtilsRefreshRate.getCurrentResWithName()
                            )
                        )
                    } else {
                        Toast.makeText(
                            applicationContext,
                            if (loadComplete) "Feature not supported." else getString(R.string.rrp_not_loaded),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                CHANGE_SETTINGS -> {
                    try {
                        Toast.makeText(
                            applicationContext,
                            getString(R.string.enable_write_settings),
                            Toast.LENGTH_LONG
                        ).show()
                        startActivityAndCollapse(changeSystemSettingsIntent.apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            data = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                        })
                    } catch (_: Exception) {
                    }
                    // e.suppressed
                    return@launch
                }

                NO_CONFIG_LOADED -> {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.rrp_not_loaded),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { }
            }

        }
    }


    private fun getChangeResDialog(curReso: String): AlertDialog {
        return AlertDialog.Builder(applicationContext).apply {
            setCancelable(false)
            setTitle(getString(R.string.chng_res))
            setMessage(
                if (hasWriteSecureSetPerm) {
                    getString(
                        R.string.chng_res_qs,
                        curReso
                    )
                } else {
                    getString(
                        R.string.chng_res_stng,
                        curReso
                    )
                }
            )
            setPositiveButton(
                if (hasWriteSecureSetPerm) getString(R.string.chng_res) else getString(R.string.open_settings)
            ) { _, _ ->
                if (hasWriteSecureSetPerm) {
                    //getResolutionChoiceDialog(context).show()
                    UtilNotificationBarSt.instance(applicationContext).expandNotificationBar()
                } else {
                    val i = displaySettingsIntent
                    if (context !is Activity) {
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(i)
                }
            }
            setNegativeButton(getString(R.string.dismiss)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            if (!hasWriteSecureSetPerm) {
                setNeutralButton(
                    getString(R.string.adb_setup)
                ) { _, _ ->
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(ADB_SETUP_LINK)
                    )
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(browserIntent)
                }
            }
        }.create()
    }

    private fun getChangeRefreshRateModeDialog(powerSaveOn: Boolean): AlertDialog {
        return AlertDialog.Builder(applicationContext).apply {
            setCancelable(false)
            setTitle(
                if (powerSaveOn) {
                    getString(R.string.psm_on)
                } else {
                    getString(R.string.lbl_enable_msm)
                }
            )
            setMessage(
                getString(
                    if (powerSaveOn) {
                        R.string.psm_on_inf
                    } else {
                        R.string.rrm_std_inf
                    }
                )
            )
            setPositiveButton(
                getString(R.string.open_settings)
            ) { _, _ ->
                if (powerSaveOn) {
                    val i = powerSavingModeSettingsIntent
                    startActivity(i)
                } else {
                    try {
                        val i = motionSmoothnessSettingsIntent
                        startActivity(i)
                    }catch (_: Exception){
                        val i = displaySettingsIntent
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(i)
                    }
                }
            }
            setNegativeButton(getString(R.string.dismiss)) { dialogInterface, _ -> dialogInterface.dismiss() }
            setNeutralButton(
                getString(R.string.adb_setup)
            ) { _, _ ->
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(ADB_SETUP_LINK)
                )
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(browserIntent)
            }
        }.create()
    }

}