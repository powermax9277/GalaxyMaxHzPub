package com.tribalfs.gmh.dialogs

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.BuildConfig
import com.tribalfs.gmh.MyApplication
import com.tribalfs.gmh.R
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.ExperimentalCoroutinesApi


object QSDialogs {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPermissionDialog(context: Context): AlertDialog {
        val appCtx = context.applicationContext
        val builder = getDialog(context)
        builder.setTitle(appCtx.getString(R.string.perm_reqd))
        builder.setMessage(appCtx.getString(
            R.string.requires_ws_perm_h,
            MyApplication.applicationName,
            BuildConfig.APPLICATION_ID
        ))
        builder.setPositiveButton(appCtx.getString(R.string.adb_setup)
        ) { dialogInterface, _ ->
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(ADB_SETUP_LINK)
            )
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appCtx.startActivity(browserIntent)
            dialogInterface.dismiss()
        }
        builder.setNegativeButton(appCtx.getString(R.string.dismiss)) { dialogInterface, _ -> dialogInterface.dismiss() }
        return builder.create()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getAppearOnTopDialog(context: Context): AlertDialog {
        val appCtx = context.applicationContext
        val builder = getDialog(context)
        builder.setTitle(appCtx.getString(R.string.allow_aot))
        builder.setMessage(appCtx.getString(R.string.aot_perm_inf))
        builder.setPositiveButton(appCtx.getString(R.string.allow)
        ) { _, _ ->
                val i = getOverlaySettingIntent(appCtx)
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            appCtx.startActivity(i)
        }

        builder.setNeutralButton(context.getString(R.string.dismiss)) { dialogInterface, _ ->
            UtilsPrefsGmhSt.instance(appCtx).gmhPrefHzOverlayIsOn = false
            dialogInterface.dismiss()
        }

        return builder.create()
    }

    fun getAllowAccessDialog(context: Context): AlertDialog {
        val appCtx = context.applicationContext
        val builder = getDialog(context)
        builder.setTitle(appCtx.getString(R.string.acces_req))
        builder.setMessage(appCtx.getString(R.string.enable_as_inf))
        builder.setPositiveButton(appCtx.getString(R.string.allow)) { _, _ ->
             val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            appCtx.startActivity(intent)

        }
        builder.setNeutralButton(appCtx.getString(R.string.dismiss)) { dialogInterface, _ ->
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