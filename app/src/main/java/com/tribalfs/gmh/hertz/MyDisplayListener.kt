package com.tribalfs.gmh.hertz

import android.hardware.display.DisplayManager
import com.tribalfs.gmh.callbacks.DisplayChangedCallback

internal class MyDisplayListener(private val refreshRateCallback: DisplayChangedCallback) : DisplayManager.DisplayListener {
    override fun onDisplayAdded(displayId: Int) {}
    override fun onDisplayRemoved(displayId: Int) {}
    override fun onDisplayChanged(arg0: Int) {
        refreshRateCallback.onDisplayChanged()
    }
}