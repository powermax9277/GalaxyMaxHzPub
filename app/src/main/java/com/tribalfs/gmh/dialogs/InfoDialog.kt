package com.tribalfs.gmh.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle

class InfoDialog(private val tit: Int, private val msg: Int): RoundedDialogFragment(){
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return AlertDialog.Builder(context).apply {
            setTitle(tit)
            setMessage(msg)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
               dialog.dismiss()
            }
        }.create()
    }

}