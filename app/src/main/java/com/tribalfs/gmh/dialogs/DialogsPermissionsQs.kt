package com.tribalfs.gmh.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.UtilsPermSt.Companion.REQUEST_CODE_APPEAR_ON_TOP_PERM
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh


object DialogsPermissionsQs {
    fun getPermissionDialog(context: Context, msg: String?): AlertDialog {
        val builder = getDialog(context)
        builder.setTitle(context.getString(R.string.perm_reqd))
        builder.setMessage(msg)
        builder.setPositiveButton(
            context.getString(R.string.adb_setup)
        ) { dialogInterface, _ ->
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(ADB_SETUP_LINK)
            )
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(browserIntent)
            dialogInterface.dismiss()
        }
        builder.setNegativeButton(context.getString(R.string.dismiss)) { dialogInterface, _ -> dialogInterface.dismiss() }
        return builder.create()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getAppearOnTopDialog(context: Context): AlertDialog {
        val builder = getDialog(context)
        builder.setTitle(context.getString(R.string.allow_aot))
        builder.setMessage(context.getString(R.string.aot_perm_inf))
        builder.setPositiveButton(
            context.getString(R.string.allow)
        ) { _, _ ->
            if (context is Activity) {
                context.startActivityForResult(
                    getOverlaySettingIntent(context),
                    REQUEST_CODE_APPEAR_ON_TOP_PERM
                )
            } else {
                val i = getOverlaySettingIntent(context)
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(i)
            }
        }
        builder.setNeutralButton(context.getString(R.string.dismiss)) { dialogInterface, _ ->
            UtilsPrefsGmh(context.applicationContext).gmhPrefHzOverlayIsOn = false
            dialogInterface.dismiss()
        }

        return builder.create()
    }


    private fun getDialog(context: Context): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        return builder
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getOverlaySettingIntent(context: Context) : Intent {
        return  Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.applicationContext.packageName}")
        )
    }

}