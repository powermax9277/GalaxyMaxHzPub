package com.tribalfs.gmh.resochanger

import android.content.Context
import android.content.pm.PackageManager
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
    private val mUtilsRefreshRate by lazy { UtilsRefreshRateSt.instance(appCtx) }

    @ExperimentalCoroutinesApi
    suspend fun changeRes(resLxw: String?): Int? {
        return if (hasWriteSecureSetPerm || UtilsPermSt.instance(appCtx).hasWriteSecurePerm()) {
            if (changeResInternal(resLxw)) {
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
        val reso = resLxw?:mUtilsRefreshRate.mUtilsDeviceInfo.getDisplayResStr("x")
        mUtilsRefreshRate.getResolutionsForKey(null)?.forEach{
            it[reso]?.let{res ->
                return res.resName
            }
        }

        reso.split("x").let {
            return UtilsResoName.getName(it[0].toInt(),it[1].toInt())
        }
    }


    private fun getNextResAndDen(density : Int) : List<String>{
        val currentResString = mUtilsRefreshRate.mUtilsDeviceInfo.getDisplayResStr("x")
        val resList = getFilteredResList()

        // Log.d(TAG, "resList ${resList?.joinToString(",").toString()}")
        val idx = resList.indexOfFirst {
            it.containsKey(currentResString)
        }

        var nextResDetails: ResolutionDetails? =  null
        resList[if(idx-1>= 0) idx-1 else resList.size-1].forEach{
            nextResDetails = it.value
        }
        val nextResH = nextResDetails!!.resHeight
        val nextResW = nextResDetails!!.resWidth
        val curResH = currentResString.split("x")[0]

        val newDen = ( (density.toFloat() / curResH.toFloat()) * (nextResH.toFloat()) ).toInt()

        // Log.d(TAG, "ChangeRes / Current Resolution detected: $currentResString DPI: $newDen")
        return ("$nextResH,$nextResW,$newDen").split(",")
    }

    @ExperimentalCoroutinesApi
    private suspend fun changeResInternal(resLxw: String?): Boolean = withContext(Dispatchers.Default) {

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

        val nextRes: String?
        val nextDen: Int?

        if (resLxw == null) {
            val nextResAndDen = getNextResAndDen(currentDensity)
            nextRes = "${nextResAndDen[0]}x${nextResAndDen[1]}"
            nextDen = nextResAndDen[2].toInt()
        } else {
            if (getFilteredResList().indexOfFirst {
                    it.containsKey(resLxw)
                } != -1) {
                nextRes = resLxw
                val currentResString =mUtilsRefreshRate.mUtilsDeviceInfo.getDisplayResStr("x")
                val curResH = currentResString.split("x")[0]
                nextDen = ((currentDensity.toFloat() / curResH.toFloat()) * (resLxw.split("x")[0].toFloat())).toInt()
            } else {
                return@withContext false
            }
        }

        val nextResName =  getResName(nextRes)
        val curResName = getResName(null)

        if (isSamsung) {
            // Log.d(TAG, "setRefreshRateMode(REFRESH_RATE_MODE_STANDARD) called")
            mUtilsRefreshRate.setRefreshRateMode(REFRESH_RATE_MODE_STANDARD)
        }

        if (nextResName == "CQHD+" && listOf("WQHD+","CQHD+").indexOf(curResName) != -1){
            nextRes.split("x").let { nextResSplit ->
                ResolutionChangeApi(appCtx).setDisplaySizeDensity(
                    displayId,
                    "${(nextResSplit[0].toFloat()/nextResSplit[1].toFloat() * 1080f).toInt()}x1080",
                    null
                )
            }
        }

        val changeResResult = ResolutionChangeApi(appCtx).setDisplaySizeDensity(
            displayId,
            nextRes,
            nextDen
        )

        delay(300)
        if (changeResResult) {
            mUtilsRefreshRate.setPrefOrAdaptOrHighRefreshRateMode(nextRes)
            delay(250)//don't remove
            launch(Dispatchers.Main){
                Toast.makeText(
                    appCtx,
                    "$nextResName[$nextRes] display resolution applied.",
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