package com.tribalfs.gmh.helpers

import com.tribalfs.gmh.resochanger.SizeDensity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object UtilsReso {
    private const val N60_VGA = 480
    private const val QrHD = 540
    private const val HD = 720
    private const val FHD_2K = 1080
    private const val WUXGA = 1200
    private const val QHD_M = 1400
    private const val QHD = 1440
    private const val QXGA = 1536
    private const val QSXGA = 2048
    private const val UHD_4K = 2160
    private const val QUXGA = 2400
    private const val FVK = 2880
    private const val FUHD = 4320


    fun getName(x: Int, y: Int): String {
        val w = min(x, y)
        val h = max(x, y)
        val ar = h.toFloat() / w.toFloat()

        var resName = when {
            (w >= FUHD) -> {
                when {
                    (h >= 17280) -> "STK"
                    (h >= 7680) -> "8KUHD"
                    else -> "FUHD"
                }
            }

            (w >= FVK) -> {
                when {
                    (h >= 5200) -> "W5K"
                    (h >= 5120) -> "5K"
                    else -> "UHD+"
                }
            }

            (w >= QUXGA) -> {
                if (h >= 3840) "WQUXGA" else "QUXGA"
            }

            (w >= UHD_4K) -> {
                when {
                    (h >= 4632) -> "WUHD"
                    (h >= 4096) -> "DCI4K"
                    (h >= 3840) -> "4KUHD"
                    else -> "UHD"
                }
            }

            (w >= QSXGA) -> {
                if (h >= 3200) "WQSXGA" else "QSXGA"
            }

            (w >= QXGA) -> {
                when {
                    (h > 2048) -> if (w > 1536) "WQXGA+" else "QXGA+"
                    else -> if (w > 1536) "WQXGA" else "QXGA"
                }
            }

            (w >= QHD) -> if (ar >= 16f / 9f) "WQHD" else "QHD"

            (w >= QHD_M) -> "CQHD"

            (w >= WUXGA) -> "WUXGA"

            (w >= FHD_2K) -> "FHD"

            (w >= HD) -> "HD"

            (w >= QrHD) -> "qHD"

            (w >= N60_VGA) -> {
                when (h) {
                    960 -> "960H"
                    854 -> "FWVGA"
                    720 -> "SD"
                    else -> "480p"
                }
            }
            else -> "?Name"
        }

        if (ar >= 18.5f / 9f) resName = "$resName+"

        return resName
    }


    internal fun getNewMatchingDpi(curReso: SizeDensity, newReso: SizeDensity): Int {
        val curDiagPixels = sqrt(curReso.h.toFloat().pow(2) + curReso.w.toFloat().pow(2))
        val factor = curDiagPixels / curReso.dpi.toFloat()
        val newDiagPixels = sqrt(newReso.h.toFloat().pow(2) + newReso.w.toFloat().pow(2))
        return (newDiagPixels / factor).toInt()
    }
}
