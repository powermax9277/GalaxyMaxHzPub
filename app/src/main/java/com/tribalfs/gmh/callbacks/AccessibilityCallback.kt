package com.tribalfs.gmh.callbacks

interface AccessibilityCallback {
    fun onChange(userPresent: Boolean, turnOffSensors: Boolean)
}
