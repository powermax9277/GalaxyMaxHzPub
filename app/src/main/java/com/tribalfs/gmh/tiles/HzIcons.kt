package com.tribalfs.gmh.tiles

import com.tribalfs.gmh.R

internal object HzIcons{
    fun get(num: Int): Int?{
        return when(num){
            30 -> R.drawable.ic_baseline_30hz_24
            48 -> R.drawable.ic_baseline_48hz_24
            60 -> R.drawable.ic_baseline_60hz_24
            90 -> R.drawable.ic_baseline_90hz_24
            96 -> R.drawable.ic_baseline_96hz_24
            100 -> R.drawable.ic_baseline_100hz_24
            105 -> R.drawable.ic_baseline_105hz_24
            120 -> R.drawable.ic_baseline_120hz_24
            142 -> R.drawable.ic_baseline_142hz_24
            144 -> R.drawable.ic_baseline_144hz_24
            160 -> R.drawable.ic_baseline_160hz_24
            else -> null
        }
    }
}