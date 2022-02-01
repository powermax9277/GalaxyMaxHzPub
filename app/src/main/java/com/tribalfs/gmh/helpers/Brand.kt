package com.tribalfs.gmh.helpers

import com.tribalfs.gmh.helpers.CacheSettings.isOnePlus
import com.tribalfs.gmh.helpers.CacheSettings.isSamsung
import com.tribalfs.gmh.helpers.CacheSettings.isXiaomi

object Brand {
    internal fun set(manufacturer: String){
        when (manufacturer) {
            "SAMSUNG" ->{
                isSamsung = true
                isXiaomi = false
                isOnePlus = false
            }
            "XIAOMI","POCO" ->{
                isSamsung = false
                isXiaomi = true
                isOnePlus = false
            }
            "ONEPLUS" ->{
                isSamsung = false
                isXiaomi = false
                isOnePlus = true
            }
        }
    }
}