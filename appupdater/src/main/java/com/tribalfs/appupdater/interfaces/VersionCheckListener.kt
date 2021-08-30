package com.tribalfs.appupdater.interfaces

import com.tribalfs.appupdater.enums.AppUpdaterError
import com.tribalfs.appupdater.objects.AppDetails

interface VersionCheckListener {
    fun onSuccess(appDetails: AppDetails)
    fun onFailed(error: AppUpdaterError)
}