package com.tribalfs.gmh.resochanger

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Size
import android.widget.Toast
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isMultiResolution
import com.tribalfs.gmh.helpers.CacheSettings.isSamsung
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.helpers.UtilsDeviceInfoSt.Companion.SECURE
import com.tribalfs.gmh.profiles.ResolutionDetails
import kotlinx.coroutines.*


class ResolutionChangeUtil (context: Context) {

    /*companion object : Singleton<ResolutionChangeUtilSt, Context>(::ResolutionChangeUtilSt){
        // private const val TAG = "ResolutionChangeUtil"
    }*/
    private val appCtx = context.applicationContext
    internal val mUtilsRefreshRate by lazy { UtilsRefreshRateSt.instance(appCtx) }

    @ExperimentalCoroutinesApi
    suspend fun changeRes(reso: Size?): Int? {
        return if (hasWriteSecureSetPerm || UtilsPermSt.instance(appCtx).hasWriteSecurePerm()) {
            if (changeResInternal(reso)) {
                PackageManager.PERMISSION_GRANTED
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appCtx,
                        appCtx.getString(R.string.chng_res_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                null
            }
        } else {
            UtilsPermSt.instance(appCtx).getPerm(SECURE)
        }
    }

    private fun getFilteredResList(): List<Map<String, ResolutionDetails>>{
        return mUtilsRefreshRate.getResolutionsForKey(null)?.filter {
            val key = it.keys.first()
            val res = "$key(${it[key]?.resName})"
            (mUtilsRefreshRate.mUtilsPrefsGmh.gmhPrefGetSkippedRes?.contains(res) != true)
        }!!
    }

    internal fun getResName(resLxw: String?): String {
        val reso = resLxw?:mUtilsRefreshRate.mUtilsDeviceInfo.getDisplayResoStr("x")
        mUtilsRefreshRate.getResolutionsForKey(null)?.forEach{
            it[reso]?.let{res ->
                return res.resName
            }
        }

        reso.split("x").let {
            return UtilsResoName.getName(it[0].toInt(),it[1].toInt())
        }
    }


    private fun getNextResAndDen(density : Int) : SizeDensity/*List<String>*/{
        val currentResString = mUtilsRefreshRate.mUtilsDeviceInfo.getDisplayResoStr("x")
        val resList = getFilteredResList()

        // Log.d(TAG, "resList ${resList?.joinToString(",").toString()}")
        val idx = resList.indexOfFirst {
            it.containsKey(currentResString)
        }

        var nextResDetails: ResolutionDetails? =  null
        resList[if (idx-1>= 0) idx-1 else resList.size-1].forEach{
            nextResDetails = it.value
        }
        val nextResH = nextResDetails!!.resHeight
        val nextResW = nextResDetails!!.resWidth
        val curResH = currentResString.split("x")[0]

        val newDen = ( (density.toFloat() / curResH.toFloat()) * (nextResH.toFloat()) ).toInt()

        // Log.d(TAG, "ChangeRes / Current Resolution detected: $currentResString DPI: $newDen")
        return SizeDensity(nextResW, nextResH, newDen) /* ("$nextResH,$nextResW,$newDen").split(",")*/
    }

    @ExperimentalCoroutinesApi
    private suspend fun changeResInternal(reso: Size?/*String?*/): Boolean = withContext(Dispatchers.Default) {

        val currentDensity = mUtilsRefreshRate.mUtilsDeviceInfo.getDisplayDensity()

        if (!isMultiResolution) {
            launch(Dispatchers.Main) {
                Toast.makeText(
                    appCtx,
                    appCtx.getString(R.string.one_res_def),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            return@withContext false
        }

        val nextReso: Size?
        val nextDen: Int?

        if (reso == null) {
            val nextResAndDen = getNextResAndDen(currentDensity)
            nextReso = Size(nextResAndDen.w,nextResAndDen.h)
            nextDen = nextResAndDen.dpi
        } else {
            if (getFilteredResList().indexOfFirst {
                    it.containsKey("${reso.height}x${reso.width}")
                } != -1) {
                nextReso = reso
                val nexResoH = reso.height.toFloat()
                val currentReso = mUtilsRefreshRate.mUtilsDeviceInfo.getDisplayResolution()
                val curResH = currentReso.height
                nextDen = ((currentDensity.toFloat() / curResH.toFloat()) * nexResoH).toInt()
            } else {
                return@withContext false
            }
        }

        val nextResStr = "${nextReso.height}x${nextReso.width}"
        val nextResName =  getResName(nextResStr)
        val curResName = getResName(null)

        if (isSamsung) {
            // Log.d(TAG, "setRefreshRateMode(REFRESH_RATE_MODE_STANDARD) called")
            mUtilsRefreshRate.setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
        }

        if (nextResName == "CQHD+" && listOf("WQHD+","CQHD+").indexOf(curResName) != -1){
                ResolutionChangeApi(appCtx).setDisplaySizeDensity(
                    displayId,Size(1080, (nextReso.height.toFloat()/nextReso.width.toFloat()*1080f).toInt()),
                    null
                )
        }

        val changeResResult = ResolutionChangeApi(appCtx).setDisplaySizeDensity(
            displayId,
            nextReso,
            nextDen
        )

        delay(300)
        if (changeResResult) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mUtilsRefreshRate.setPrefOrAdaptOrHighRefreshRateMode(nextResStr)
            delay(250)//don't remove
            }
            launch(Dispatchers.Main){
                Toast.makeText(
                    appCtx,
                    "$nextResName[$nextReso] display resolution applied.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }else{
            if (hasWriteSecureSetPerm){
                launch(Dispatchers.Main){
                    Toast.makeText(
                        appCtx,
                        "Unable to execute change resolution command successfully. Try again after rebooting this device.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        return@withContext changeResResult
    }
}