package com.tribalfs.gmh.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import com.tribalfs.gmh.MyApplication.Companion.applicationName
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt.Companion.NOT_USING
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt.Companion.USING
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class DialogUsingSpay: MyDialogFragment(){
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val appCtx = requireActivity().applicationContext
        isCancelable = false
        return AlertDialog.Builder(context).apply {
            setTitle("Samsung Pay")
            setMessage(
                "Samsung pay is installed on your device. Please confirm if you are using it or not. " +
                        "$applicationName needs to adjust some behaviors to prevent Samsung pay from crashing."
            )
            setPositiveButton("Yes, I'm using it") { _, _ ->
               UtilsPrefsGmhSt.instance(appCtx).hzPrefUsingSPay = USING
            }
            setNegativeButton("No, I'm not using it") { _, _ ->
                UtilsPrefsGmhSt.instance(appCtx).hzPrefUsingSPay = NOT_USING
            }
        }.create()
    }
}