package com.tribalfs.appupdater.interfaces

import android.content.Intent

interface DownloadCompleteCallback {
    fun complete(installIntent: Intent)
}