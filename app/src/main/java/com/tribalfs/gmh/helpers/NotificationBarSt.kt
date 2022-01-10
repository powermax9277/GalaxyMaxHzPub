package com.tribalfs.gmh.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class NotificationBarSt private constructor (context: Context) {

    companion object : SingletonHolder<NotificationBarSt, Context>(::NotificationBarSt){
        private const val STATUSBAR = "statusbar"
        private const val STATUSBAR_MANAGER = "android.app.StatusBarManager"
        private const val EXPAND_PANEL = "expandSettingsPanel"
        private const val COLLAPSE_PANEL = "collapsePanels"
    }

    private val appCtx = context.applicationContext

    private val isPermitted: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        (appCtx.applicationContext.checkSelfPermission(Manifest.permission.EXPAND_STATUS_BAR) == PackageManager.PERMISSION_GRANTED)
    } else {
        true
    }

    @SuppressLint("WrongConstant")
    fun expandNotificationBar() {
        if (!isPermitted) return
        try {
            appCtx.getSystemService(STATUSBAR)?.let{
                Class.forName(STATUSBAR_MANAGER).getMethod(EXPAND_PANEL).invoke(it)
            }
        } catch (e: Exception) {
            try {
            Toast.makeText(appCtx, e.localizedMessage, Toast.LENGTH_SHORT)
                .show()
            }catch(_: Exception){}
        }
    }

    @SuppressLint("WrongConstant")
    fun collapseNotificationBar(): Boolean {
        if (!isPermitted) return false
        return try {
            Class.forName(STATUSBAR_MANAGER).getMethod(COLLAPSE_PANEL).invoke(appCtx.getSystemService(STATUSBAR))
            true
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(appCtx, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

}

