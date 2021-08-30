package com.tribalfs.gmh.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import com.tribalfs.gmh.MyApplication.Companion.applicationName
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh.Companion.NOT_USING
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh.Companion.USING
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class DialogUsingSpay: RoundedDialogFragment(){
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
               UtilsPrefsGmh(appCtx).hzPrefUsingSPay = USING
            }
            setNegativeButton("No, I'm not using it") { _, _ ->
                UtilsPrefsGmh(appCtx).hzPrefUsingSPay = NOT_USING
            }

        }.create()
    }
}