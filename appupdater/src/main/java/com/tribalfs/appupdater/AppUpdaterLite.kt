package com.tribalfs.appupdater

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.tribalfs.appupdater.enums.AppUpdaterError
import com.tribalfs.appupdater.interfaces.DownloadCompleteCallback
import com.tribalfs.appupdater.interfaces.OnUpdateCheckedCallback
import com.tribalfs.appupdater.interfaces.VersionCheckListener
import com.tribalfs.appupdater.objects.AppDetails
import com.tribalfs.appupdater.utils.AppUtils.getAppInstalledVersion
import com.tribalfs.appupdater.utils.AppUtils.getAppInstalledVersionCode
import com.tribalfs.appupdater.utils.AppUtils.getAppName
import com.tribalfs.appupdater.utils.AppUtils.isUpdateAvailable
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = "AppUpdaterLite"

class AppUpdaterLite(private val context: Context) {

    private var latestAppVersion: LatestAppVersion? = null
    private var jsonUrl: String? = null
    private val updaterPrefs by lazy {UpdaterPreferences(context)}
    private var isForce = false
    private var onUpdateCheckedCallback:OnUpdateCheckedCallback? = null

    fun setUpdateJSON(jsonUrl: String): AppUpdaterLite {
        this.jsonUrl = jsonUrl
        return this
    }

    fun setUpdateForce(force: Boolean): AppUpdaterLite{
        isForce = force
        return this
    }

    fun setCallback(callback: OnUpdateCheckedCallback): AppUpdaterLite{
        onUpdateCheckedCallback = callback
        return this
    }


    @ExperimentalCoroutinesApi
    fun start() {
        Log.d(TAG, "Start check update called")
        if (!isForce && !updaterPrefs.checkAllowUpdater()) {
            return
        }

        latestAppVersion = LatestAppVersion(
            context,
            jsonUrl,
            object : VersionCheckListener {

                @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                override fun onSuccess(serverAppDetails: AppDetails) {
                    if (context is Activity && context.isFinishing) return

                    val installedAppDetails = AppDetails(
                        getAppInstalledVersion(context),
                        getAppInstalledVersionCode(context)
                    )

                    if (isUpdateAvailable(installedAppDetails, serverAppDetails)) {
                        onUpdateCheckedCallback?.onUpdateChecked(false)

                        Log.d(TAG, "New update is available")
                        if (isForce){
                            Snackbar.make((context as Activity).findViewById(android.R.id.content),
                                context.getString(R.string.appupdater_update_available_description_snackbar,
                                    serverAppDetails.latestVersion
                                ),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }

                        UpdateDownloader(
                            context,
                            serverAppDetails.latestVersionCode,
                            serverAppDetails.urlToDownload!!,
                            getAppName(context),
                            object : DownloadCompleteCallback {
                                override fun complete(installIntent: Intent) {

                                    if (context is Activity && context.isFinishing) return

                                    (context as Activity).runOnUiThread {

                                        AlertDialog.Builder(context)
                                            .setTitle(context.getString(R.string.appupdater_app_update, getAppName(context)))
                                            .setMessage(
                                                getDescriptionUpdate(
                                                    context, serverAppDetails
                                                )
                                            )
                                            .setPositiveButton(
                                                context.getString(R.string.appupdater_btn_update)
                                            ) { _, _ ->
                                               onUpdateCheckedCallback?.onUpdateChecked(true)
                                                context.startActivity(installIntent)
                                            }
                                            .setNegativeButton(
                                                context.getString(android.R.string.cancel)
                                            ){dialogInt, _ ->
                                                dialogInt.dismiss()
                                            }
                                            .setIcon(R.drawable.ic_baseline_system_update_24)
                                            .setCancelable(false)
                                            .create()
                                            .show()
                                    }
                                }

                            }
                        ).enqueueDownload()

                    }else{
                        Log.d(TAG, "No update is available")
                        if (isForce){
                            Snackbar.make((context as Activity).findViewById(android.R.id.content),
                                context.getString(R.string.appupdater_update_not_available),
                                Snackbar.LENGTH_LONG
                            ).show()}

                       onUpdateCheckedCallback?.onUpdateChecked(false)
                    }

                }

                override fun onFailed(error: AppUpdaterError) {
                    if (context is Activity && context.isFinishing) return
                    onUpdateCheckedCallback?.onUpdateChecked(false)
                    /*if (error == AppUpdaterError.UPDATE_VARIES_BY_DEVICE) {
                        Log.e(
                            TAG,
                            "UpdateFrom.GOOGLE_PLAY isn't valid: update varies by device."
                        )
                    }*/
                }
            })
        latestAppVersion!!.execute()
    }


    private fun getDescriptionUpdate(context: Context, appDetails: AppDetails): String {
        return String.format(context.resources.getString(R.string.appupdater_update_available_description_dialog,
            appDetails.latestVersion,
            appDetails.releaseNotes
            )
        )
    }



}