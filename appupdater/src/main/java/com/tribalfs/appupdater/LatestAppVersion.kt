package com.tribalfs.appupdater

import android.content.Context
import com.tribalfs.appupdater.enums.AppUpdaterError
import com.tribalfs.appupdater.interfaces.VersionCheckListener
import com.tribalfs.appupdater.objects.AppDetails
import com.tribalfs.appupdater.utils.AppUtils.getLatestAppVersion
import com.tribalfs.appupdater.utils.AppUtils.isNetworkAvailable
import com.tribalfs.appupdater.utils.AppUtils.isStringAVersion
import com.tribalfs.appupdater.utils.AppUtils.isStringAnUrl
import kotlinx.coroutines.*


class LatestAppVersion(
    private val context: Context,
    private val jsonUrl: String?,
    private val listener: VersionCheckListener?
) {

    private var isCancelled: Boolean = true


    @ExperimentalCoroutinesApi
    private val job: Job = CoroutineScope(Dispatchers.IO).launch {
        cancel(false)
        val appDetailsDeferred: Deferred<AppDetails?> = async(Dispatchers.IO) {
            return@async  getLatestAppVersion(jsonUrl)

        }
        appDetailsDeferred.await()

        val update = appDetailsDeferred.getCompleted()

        if (update == null) {
            val error =  AppUpdaterError.JSON_ERROR
            listener?.onFailed(error)
            cancel(true)
        } else {
            if (isStringAVersion(update.latestVersion!!)) {
                listener?.onSuccess(update)
                cancel(true)
            } else {
                listener?.onFailed(AppUpdaterError.UPDATE_VARIES_BY_DEVICE)
                cancel(true)
            }
        }
    }


    @ExperimentalCoroutinesApi
    fun execute() {
        if (listener == null) {
            return
        }

        if (isNetworkAvailable(context)) {
            if (jsonUrl == null || !isStringAnUrl(jsonUrl)) {
                listener.onFailed(AppUpdaterError.JSON_URL_MALFORMED)
                return
            }
        } else {
            listener.onFailed(AppUpdaterError.NETWORK_NOT_AVAILABLE)
            return
        }

        job.start()
    }


    @ExperimentalCoroutinesApi
    private fun cancel(isCancelled: Boolean){
        if (this.isCancelled != isCancelled) {
            if (isCancelled){
                job.cancel()
            }
            this.isCancelled = isCancelled
        }
    }
}
