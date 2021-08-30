package com.tribalfs.gmh.netspeed

import android.content.Context
import com.tribalfs.gmh.helpers.SingletonHolder
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh.Companion.DOWNLOAD_SPEED
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh.Companion.UPLOAD_SPEED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

internal class SpeedTypes(mContext: Context) {

    companion object : SingletonHolder<SpeedTypes, Context>(::SpeedTypes)

    private var mTotalSpeed: Long = 0
    private var mDownSpeed: Long = 0
    private var mUpSpeed: Long = 0
    private var mTotalSpeedPrev: Long = 0
    private var mDownSpeedPrev: Long = 0
    private var mUpSpeedPrev: Long = 0
    private var mCalcInBits = true

    val tlSpeedData = SpeedDetails(mContext)
    val dlSpeedData = SpeedDetails(mContext)
    val upSpeedData = SpeedDetails(mContext)

    fun setIsSpeedUnitBits(inBits: Boolean) {
        mCalcInBits = inBits
    }

    suspend fun processSpeed(timeTaken: Long, downKBytes: Long, upKBytes: Long) = withContext(Dispatchers.Default) {
        if (timeTaken > 0) {

            mDownSpeed = (downKBytes / timeTaken)
            mUpSpeed = (upKBytes / timeTaken)
        } else {
            mDownSpeed = 0
            mUpSpeed = 0
        }
        mTotalSpeed = mDownSpeed + mUpSpeed

        tlSpeedData.setSpeed((mTotalSpeed*.7 + mTotalSpeedPrev*.3).roundToLong(), mCalcInBits)
        dlSpeedData.setSpeed((mDownSpeed*.7 + mDownSpeedPrev*.3).roundToLong(), mCalcInBits)
        upSpeedData.setSpeed((mUpSpeed*.7 + mUpSpeedPrev*.3).roundToLong(), mCalcInBits)
        mTotalSpeedPrev = mTotalSpeed
        mDownSpeedPrev = mDownSpeed
        mUpSpeedPrev = mUpSpeed
    }

    fun getSpeedDetails(name: String): SpeedDetails {
        return when (name) {
            UPLOAD_SPEED -> upSpeedData
            DOWNLOAD_SPEED -> dlSpeedData
            else -> tlSpeedData
        }
    }

}