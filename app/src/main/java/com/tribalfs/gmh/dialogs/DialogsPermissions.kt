package com.tribalfs.gmh.dialogs
/*

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.tribalfs.gmh.R


class DialogsPermissions : RoundedDialogFragment()  {
    private var msg: String? = null

    companion object {
        fun newInstance(msg: String): DialogsPermissions {
            val f = DialogsPermissions()
            val args = Bundle()
            args.putString("msg", msg)
            f.arguments = args
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        msg = arguments?.getString("msg")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.perm_reqd))
        builder.setMessage(msg)
        builder.setPositiveButton(
            getString(R.string.adb_setup)
        ) { dialogInterface, _ ->
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(ADB_SETUP_LINK)
            )
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(browserIntent)
            dialogInterface.dismiss()
        }
        builder.setNegativeButton(getString(R.string.dismiss)) { dialogInterface, _ -> dialogInterface.dismiss() }
        isCancelable = false
        return builder.create()
    }

}*/
