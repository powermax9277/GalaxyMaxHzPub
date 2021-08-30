package com.tribalfs.gmh.tiles

import com.tribalfs.gmh.R

internal object ResoIcons {
    fun get(resName: String): Int? {
        return when (resName) {
            "HD" -> R.drawable.ic_baseline_hd_24
            "HD+" -> R.drawable.ic_baseline_hdplus_24
            "FHD" -> R.drawable.ic_baseline_fhd_24
            "FHD+" -> R.drawable.ic_baseline_fhdplus_24
            "WQHD" -> R.drawable.ic_baseline_wqhd_24
            "WQHD+" -> R.drawable.ic_baseline_wqhdplus_24
            "CQHD+" -> R.drawable.ic_baseline_cqhdplus_24
            "UHD" -> R.drawable.ic_baseline_uhd_24
            "UHD+" -> R.drawable.ic_baseline_uhdplus_24
            "FUHD" -> R.drawable.ic_baseline_fuhd_24
            "FUHD+" -> R.drawable.ic_baseline_fuhdplus_24
            "QXGA" -> R.drawable.ic_baseline_qxga_24
            "QXGA+" -> R.drawable.ic_baseline_qxgaplus_24
            "WUXGA" -> R.drawable.ic_baseline_wuxga_24
            "WUXGA+" -> R.drawable.ic_baseline_wuxgaplus_24
            "WQXGA" -> R.drawable.ic_baseline_wqxga_24
            "WQXGA+" -> R.drawable.ic_baseline_wqxgaplus_24
            else -> null
        }
    }
}