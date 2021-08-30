package com.tribalfs.appupdater

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.tribalfs.appupdater.interfaces.DownloadCompleteCallback
import java.io.File
import java.net.URL


class UpdateDownloader(
    private val context: Context,
    private val versionCode: Int?,
    private val url: URL,
    private val appName: String,
    private val downloadCompleteCallback: DownloadCompleteCallback
) {

    companion object {
        private const val TAG = "UpdateDownloader"
        private const val FILE_NAME = "update.apk"
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
        private const val APP_INSTALL_PATH = "\"application/vnd.android.package-archive\""
        @JvmField var isDownloading = false
    }

    @SuppressLint("NewApi")
    fun enqueueDownload() {
        if (isDownloading) return
        var destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"
        destination += FILE_NAME
        val uri = Uri.parse("$FILE_BASE_PATH$destination")
        val file = File(destination)
        if (file.isFile) {
            //check the version code of the file
            val fileVersionCode = context.packageManager.getPackageArchiveInfo(destination, 0)?.longVersionCode?.toInt()
            Log.d(TAG, "Apk exists - version: $fileVersionCode Server version: $versionCode")
            if (versionCode == fileVersionCode){
                downloadCompleteCallback.complete(getInstallIntent(destination, uri))
                return
            } else {
                file.delete()
                Log.d(TAG, "Apk from old version deleted")
            }
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url.toString())
        val request = DownloadManager.Request(downloadUri)
        request.setMimeType(MIME_TYPE)
        request.setTitle("$appName update")
        request.setDescription("Downloading...")
        request.setDestinationUri(uri)
        setDownloadListener(destination, uri, downloadCompleteCallback)
        // Enqueue a new download and same the referenceId
        downloadManager.enqueue(request)
        isDownloading = true
    }


    private fun setDownloadListener(destination: String, uri: Uri, downloadCompleteCallback: DownloadCompleteCallback) {
        // set BroadcastReceiver to install app when .apk is downloaded
        val onDownloadCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isDownloading = false
                context.unregisterReceiver(this)
                downloadCompleteCallback.complete(getInstallIntent(destination, uri))
            }
        }
        context.registerReceiver(onDownloadCompleteReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }


    private fun getInstallIntent(destination: String, uri: Uri): Intent{
        val install = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val contentUri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + PROVIDER_PATH,
                File(destination)
            )
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            install.data = contentUri
        } else {
            install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            install.setDataAndType(
                uri,
                APP_INSTALL_PATH
            )
        }
        return install
    }
}