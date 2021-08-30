package com.tribalfs.appupdater.utils

import com.tribalfs.appupdater.objects.AppDetails
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset

internal class ParserJSON(url: String?) {
    private var jsonUrl: URL? = null
    fun parse(): AppDetails? {
        try {
            val json = readJsonFromUrl()
            val update = AppDetails()
            update.latestVersion = json.getString(KEY_LATEST_VERSION).trim { it <= ' ' }
            update.latestVersionCode = json.optInt(KEY_LATEST_VERSION_CODE)
            val releaseArr = json.optJSONArray(KEY_RELEASE_NOTES)
            if (releaseArr != null) {
                val builder = StringBuilder()
                for (i in 0 until releaseArr.length()) {
                    builder.append(releaseArr.getString(i).trim { it <= ' ' })
                    if (i != releaseArr.length() - 1) builder.append(System.getProperty("line.separator"))
                }
                update.releaseNotes = builder.toString()
            }
            val url = URL(json.getString(KEY_URL).trim { it <= ' ' })
            update.urlToDownload = url
            return update
        } catch (_: Exception) {
            /*Log.e(
                "AppUpdater",
                "The server is down or there isn't an active Internet connection.",
                e
            )
        } catch (_: JSONException) {
            Log.e(
                "AppUpdater",
                "The JSON updater file is mal-formatted. AppUpdate can't check for updates."
            )*/
        }
        return null
    }

    @Throws(IOException::class)
    private fun readAll(rd: Reader): String {
        val sb = StringBuilder()
        var cp: Int
        while (rd.read().also { cp = it } != -1) {
            sb.append(cp.toChar())
        }
        return sb.toString()
    }

    @Throws(IOException::class, JSONException::class)
    private fun readJsonFromUrl(): JSONObject {
        val `is` = jsonUrl!!.openStream()
        return `is`.use {
            val rd = BufferedReader(
                InputStreamReader(
                    it,
                    Charset.forName("UTF-8")
                )
            )
            val jsonText = readAll(rd)
            JSONObject(jsonText)
        }
    }

    companion object {
        private const val KEY_LATEST_VERSION = "latestVersion"
        private const val KEY_LATEST_VERSION_CODE = "latestVersionCode"
        private const val KEY_RELEASE_NOTES = "releaseNotes"
        private const val KEY_URL = "url"
    }

    init {
        try {
            jsonUrl = URL(url)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }
    }
}