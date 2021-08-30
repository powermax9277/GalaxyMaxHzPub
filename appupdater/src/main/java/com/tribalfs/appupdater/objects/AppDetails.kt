package com.tribalfs.appupdater.objects

import java.net.URL

class AppDetails {
    var latestVersion: String? = null
    var latestVersionCode: Int? = null
    var releaseNotes: String? = null
    var urlToDownload: URL? = null

    constructor()
    constructor(latestVersion: String, latestVersionCode: Int) {
        this.latestVersion = latestVersion
        this.latestVersionCode = latestVersionCode
    }
}