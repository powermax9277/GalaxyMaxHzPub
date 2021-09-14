package com.tribalfs.gmh.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast

internal class NotificationBarSt private constructor (context: Context) {

    companion object : SingletonHolder<NotificationBarSt, Context>(::NotificationBarSt){
        private const val STATUSBAR = "statusbar"
        private const val STATUSBAR_MANAGER = "android.app.StatusBarManager"
        private const val EXPAND_PANEL = "expandSettingsPanel"
        private const val COLLAPSE_PANEL = "collapsePanels"
    }

    private val appCtx = context.applicationContext


    @SuppressLint("NewApi")
    private fun isPermitted(): Boolean{
        return (appCtx.applicationContext.checkSelfPermission(Manifest.permission.EXPAND_STATUS_BAR) == PackageManager.PERMISSION_GRANTED)
    }

    @SuppressLint("WrongConstant")
    fun expandNotificationBar() {
        if (!isPermitted()) return
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
    fun collapseNotificationBar() {
        if (!isPermitted()) return
        try {
            appCtx.getSystemService(STATUSBAR)?.let{
                Class.forName(STATUSBAR_MANAGER).getMethod(COLLAPSE_PANEL).invoke(it)
            }
        } catch (e: Exception) {
            try {
                Toast.makeText(appCtx, e.localizedMessage, Toast.LENGTH_SHORT)
                    .show()
            }catch(_: Exception){}
        }
    }

}

