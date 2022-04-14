package com.tribalfs.gmh.profiles

import java.io.Serializable


object ProfilesObj: Serializable {

    @Volatile
    var refreshRateModeMap = mutableMapOf<String, List<Map<String, ResolutionDetails>>>()

    var adaptiveModelsObj = mutableListOf<String>()

    @Volatile
    var isProfilesLoaded: Boolean = false

    @Volatile
    var loadComplete: Boolean = false// means all modes completely loaded
}