package com.tribalfs.gmh.netspeed

import android.content.Context
import com.tribalfs.gmh.R
import java.util.*
import kotlin.math.roundToInt

internal class SpeedDetails(private val mContext: Context) {

    lateinit var speedValue: String
    lateinit var speedUnit: String

    fun setSpeed(speedKByte: Long, calcInBits: Boolean) {
        val speed = if (calcInBits) speedKByte*8/10 else speedKByte/10
        when {
            speed < 1e+3  -> {
                speedUnit = mContext.getString(if (calcInBits) R.string.kbps else R.string.kBps)
                speedValue = speed.toInt().toString()
            }
            speed in 1e+3.toLong() until 1e+6.toLong() -> {
                speedUnit = mContext.getString(if (calcInBits) R.string.Mbps else R.string.MBps)
                speedValue = if (speed < 1e+4) {
                    String.format(Locale.ENGLISH, "%.1f", speed / 1e+3)
                } else {
                    (speed / 1e+3).roundToInt().toString()
                }
            }
            speed in 1e+6.toLong() until 1e+9.toLong() -> {
                speedUnit = mContext.getString(if (calcInBits) R.string.Gbps else R.string.GBps)
                speedValue = if (speed < 1e+7) {
                    String.format(Locale.ENGLISH, "%.1f", speed / 1e+7)
                } else {
                    (speed / 1e+6).roundToInt().toString()
                }
            }
            speed >= 1e+9 -> {
                speedUnit = mContext.getString(if (calcInBits) R.string.Tbps else R.string.TBps)
                speedValue = if (speed < 1e+12 ) {
                    when {
                        speed < 1e+9 -> {
                            String.format(
                                Locale.ENGLISH,
                                "%.1f",
                                speed / 1e+9)
                        }
                        else -> (speed / 1e+9).roundToInt().toString()
                    }
                } else {
                    mContext.getString(R.string.plus99)
                }
            }

            else -> {
                speedValue = mContext.getString(R.string.dash)
                speedUnit = mContext.getString(R.string.dash)
            }
        }
    }
}