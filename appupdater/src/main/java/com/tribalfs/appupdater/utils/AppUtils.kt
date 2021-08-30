package com.tribalfs.appupdater.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.text.TextUtils
import com.tribalfs.appupdater.objects.AppDetails
import com.tribalfs.appupdater.objects.Version
import java.net.MalformedURLException
import java.net.URL

internal object AppUtils {

    fun getAppName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(
            stringId
        )
    }


    fun getAppInstalledVersion(context: Context): String {
        var version = "0.0.0.0"
        try {
            version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return version
    }


    fun getAppInstalledVersionCode(context: Context): Int {
        var versionCode = 0
        try {
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            }else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionCode
    }


    fun isUpdateAvailable(installedVersion: AppDetails, latestVersion: AppDetails): Boolean {
        return if (latestVersion.latestVersionCode != null && latestVersion.latestVersionCode!! > 0) {
            latestVersion.latestVersionCode!! > installedVersion.latestVersionCode!!
        } else {
            if (!TextUtils.equals(installedVersion.latestVersion, "0.0.0.0") && !TextUtils.equals(
                    latestVersion.latestVersion,
                    "0.0.0.0"
                )
            ) {
                try {
                    val installed = Version(installedVersion.latestVersion!!)
                    val latest = latestVersion.latestVersion?.let { Version(it) }
                    latest?.let { installed.compareTo(it) }!! < 0
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else false
        }
    }


    fun isStringAVersion(version: String): Boolean {
        return version.matches(Regex(".*\\d+.*"))
    }


    fun isStringAnUrl(s: String?): Boolean {
        var res = false
        try {
            URL(s)
            res = true
        } catch (ignored: MalformedURLException) {
        }
        return res
    }


    fun getLatestAppVersion(url: String?): AppDetails? {
        return ParserJSON(url).parse()
    }


    fun isNetworkAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }
}