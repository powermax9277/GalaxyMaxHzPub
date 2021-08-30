package com.tribalfs.gmh.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tribalfs.gmh.callbacks.ChangedStatusCallback

class ScreenStatusReceiverBasic(private val callback: ChangedStatusCallback) : BroadcastReceiver() {
    override fun onReceive(p0: Context, p1: Intent) {
        when (p1.action){
            Intent.ACTION_SCREEN_OFF -> callback.onChange(false)
            Intent.ACTION_SCREEN_ON -> callback.onChange(true)
        }
    }
}