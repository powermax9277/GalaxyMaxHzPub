package com.tribalfs.gmh.netspeed

import android.content.Context
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.SingletonMaker

internal class SpeedCalculator(mContext: Context) {

    companion object : SingletonMaker<SpeedCalculator, Context>(::SpeedCalculator){
        internal var mCalcInBits = true
    }

    private val mSpeed = Speed(mContext)

    class Speed(private val mContext: Context) {

        data class SpeedDetails(
            var speedUnit: String?,
            var speedValue: String?
        )

        fun get(speedByte: Long): SpeedDetails {
            val speed = if (mCalcInBits) speedByte * 8 else speedByte
            when {
                speed < 1e+3 -> {
                    val speedUnit =
                        mContext.getString(if (mCalcInBits) R.string.kbps else R.string.kBps)
                    return SpeedDetails(speedUnit, (speed ).toString().take(3).dropLastWhile {!it.isDigit()})
                }
                speed in 1e+3.toLong() until 1e+6.toLong() -> {
                    val speedUnit =
                        mContext.getString(if (mCalcInBits) R.string.Mbps else R.string.MBps)
                    return SpeedDetails(speedUnit, (speed / 1e+3).toString().take(3).dropLastWhile {!it.isDigit()})
                }
                speed in 1e+6.toLong() until 1e+9.toLong() -> {
                    val speedUnit =
                        mContext.getString(if (mCalcInBits) R.string.Gbps else R.string.GBps)
                    return SpeedDetails(speedUnit, (speed / 1e+6).toString().take(3).dropLastWhile {!it.isDigit()})
                }
                speed in 1e+9.toLong() until 1e+12.toLong()-> {
                    val speedUnit = mContext.getString(if (mCalcInBits) R.string.Tbps else R.string.TBps)
                    return SpeedDetails(speedUnit, (speed / 1e+9).toString().take(3).dropLastWhile {!it.isDigit()})

                }
                speed >= 1e+12 -> {
                    val speedUnit = mContext.getString(if (mCalcInBits) R.string.Tbps else R.string.TBps)
                    return if (speed < 1e+15) {
                        SpeedDetails(speedUnit, (speed / 1e+12).toString().take(3).dropLastWhile {!it.isDigit()})
                    } else {
                        SpeedDetails(speedUnit, mContext.getString(R.string.plus99))
                    }

                }
                else -> {
                    return SpeedDetails(null, null)
                }
            }
        }

    }

    fun getSpeed(timeTaken: Long, Bytes: Long): Speed.SpeedDetails{
        return mSpeed.get(Bytes/timeTaken)
    }

}