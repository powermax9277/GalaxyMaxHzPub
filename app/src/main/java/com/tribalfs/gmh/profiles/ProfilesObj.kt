package com.tribalfs.gmh.profiles

import java.io.Serializable


object ProfilesObj: Serializable {
    /*Map 1 key: displayId-refreshRateMode
    Map 2 key: resolutionString
    */
    @get:Synchronized @set:Synchronized
    @Volatile var refreshRateModeMap = mutableMapOf<String, List<Map<String, ResolutionDetails>>>()

    @get:Synchronized @set:Synchronized
    var adaptiveModelsObj = mutableListOf<String>()

    @Volatile @get:Synchronized @set:Synchronized
    var isProfilesLoaded: Boolean = false

    @Volatile @get:Synchronized @set:Synchronized
    var loadComplete: Boolean = false// means all modes completely loaded
}