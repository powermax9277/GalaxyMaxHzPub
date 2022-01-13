package com.tribalfs.gmh.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle

class InfoDialog(): RoundedDialogFragment(){

    private var msg: Int? = null
    private var title: Int? = null

    companion object {
        fun newInstance(title: Int, msg: Int): InfoDialog {
            val f = InfoDialog()
            val args = Bundle()
            args.putInt("msg", msg)
            args.putInt("title", title)
            f.arguments = args
            return f
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        msg = arguments?.getInt("msg")
        title = arguments?.getInt("title")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return AlertDialog.Builder(context).apply {
            setTitle(title as Int)
            setMessage(msg as Int)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
               dialog.dismiss()
            }
        }.create()
    }

}