package com.tribalfs.gmh.profiles

import java.io.Serializable


object ProfilesObj: Serializable {

    @Volatile
    var refreshRateModeMap = mutableMapOf<String, List<Map<String, ResolutionDetails>>>()

    //TODO test @get:Synchronized @set:Synchronized
    var adaptiveModelsObj = mutableListOf<String>()

    @Volatile
    var isProfilesLoaded: Boolean = false

    @Volatile //TODO test not necessary @get:Synchronized @set:Synchronized
    var loadComplete: Boolean = false// means all modes completely loaded
}