package com.tribalfs.gmh.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.tribalfs.gmh.BuildConfig.APPLICATION_ID
import com.tribalfs.gmh.MyApplication.Companion.applicationName
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.UtilNotifBarSt
import com.tribalfs.gmh.helpers.UtilDeviceInfoSt
import com.tribalfs.gmh.helpers.UtilSettingsIntents

const val ADB_SETUP_LINK = "https://github.com/tribalfs/GalaxyMaxHzPub/blob/main/README.md"

class InfoDialog : MyDialogFragment(){

    private var actionIdx: Int? = null
    private var msg: String? = null
    private var title: String? = null
    private var plusStr: String? = null
    private var negStr: String? = null

    companion object {
        const val SENSORS_OFF_INFO = 1
        const val QDM_INFO = 2
        const val CHANGE_RES_INFO = 3
        const val ADB_PERM_INFO = 4
        const val ENABLE_ACCESS = 5
        fun newInstance(idx: Int): InfoDialog {
            val f = InfoDialog()
            val args = Bundle()
            args.putInt("info_idx", idx)
            f.arguments = args
            return f
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionIdx = arguments?.getInt("info_idx")
        val msgTitle = actionIdx?.let { getTitleMsg(it) }
        msg = msgTitle?.get(1)
        title = msgTitle?.get(0)
        plusStr = msgTitle?.get(2)
        negStr = msgTitle?.get(3)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return AlertDialog.Builder(context).apply {
            setTitle(title)
            setMessage(msg)
            setPositiveButton(plusStr) { _, _ ->
                performAction(actionIdx)
            }
            negStr?.let{
                setNegativeButton(it) { dialog, _ ->
                    dialog?.dismiss()
                }
            }
        }.create()
    }


    private fun getTitleMsg(msgIdx: Int): List<String?>?{
        return when(msgIdx){
            SENSORS_OFF_INFO ->{
                listOf(getString(R.string.auto_sensors_off_exp),getString(R.string.sensors_off_note), getString(android.R.string.ok),null)
            }
            QDM_INFO ->{
                listOf(getString(R.string.quick_doz_mod), getString(R.string.quick_doz_mod_inf2), getString(android.R.string.ok),null)
            }
            CHANGE_RES_INFO ->{
                listOf(getString(R.string.chng_res), if (hasWriteSecureSetPerm) {
                    getString(R.string.chng_res_qs, UtilDeviceInfoSt.instance(requireContext().applicationContext).getDisplayResoStr("x"))
                } else {
                    getString(R.string.chng_res_stng, UtilDeviceInfoSt.instance(requireContext().applicationContext).getDisplayResoStr("x"))
                },getString(android.R.string.ok), null)
            }
            ADB_PERM_INFO ->{
                listOf(getString(R.string.perm_reqd),"${
                    getString(
                        R.string.requires_ws_perm_h,
                        applicationName, APPLICATION_ID
                    )
                }${getString(R.string.perm_appx)}",getString(R.string.adb_setup), getString(R.string.dismiss))
            }
            ENABLE_ACCESS ->{
                listOf(getString(R.string.acces_req),getString(R.string.enable_as_inf),getString(R.string.allow), getString(R.string.dismiss))
            }
            else -> {
                null
            }
        }
    }



    private fun performAction(actionIdx: Int?){
        when(actionIdx){
            CHANGE_RES_INFO -> {
                if (hasWriteSecureSetPerm) {
                    //getResolutionChoiceDialog(context).show()
                    UtilNotifBarSt.instance(requireContext().applicationContext).expandNotificationBar()
                } else {
                    val i = UtilSettingsIntents.displaySettingsIntent
                    if (context !is Activity) {
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(i)
                }
            }
            ADB_PERM_INFO -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ADB_SETUP_LINK))
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(browserIntent)
                dismiss()
            }
            ENABLE_ACCESS ->{
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            else -> {
                dismiss()
            }
        }
    }

}