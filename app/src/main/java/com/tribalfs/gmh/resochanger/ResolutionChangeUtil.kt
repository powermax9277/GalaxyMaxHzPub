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
import com.tribalfs.gmh.helpers.UtilsReso.getNewMatchingDpi
import com.tribalfs.gmh.profiles.ResolutionDetails
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmhSt
import kotlinx.coroutines.*


class ResolutionChangeUtil (context: Context) {


    private val appCtx = context.applicationContext
    //internal val UtilRefreshRateSt.instance(appCtx) by lazy { UtilRefreshRateSt.instance(appCtx) }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun changeRes(reso: Size?): Int? {
        return if (hasWriteSecureSetPerm || UtilPermSt.instance(appCtx).hasWriteSecurePerm()) {
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
            UtilPermSt.instance(appCtx).getPerm(SECURE)
        }
    }

    private fun getFilteredResList(): List<Map<String, ResolutionDetails>>{
        return UtilRefreshRateSt.instance(appCtx).getResolutionsForKey(null)?.filter {
            val key = it.keys.first()
            val res = "$key(${it[key]?.resName})"
            (UtilsPrefsGmhSt.instance(appCtx).gmhPrefGetSkippedRes?.contains(res) != true)
        }!!
    }

    internal fun getResName(resLxw: String?): String {
        val reso = resLxw?:UtilsDeviceInfoSt.instance(appCtx).getDisplayResoStr("x")
        UtilRefreshRateSt.instance(appCtx).getResolutionsForKey(null)?.forEach{
            it[reso]?.let{res ->
                return res.resName
            }
        }

        reso.split("x").let {
            return UtilsReso.getName(it[0].toInt(),it[1].toInt())
        }
    }


    private fun getNextResAndDen() : SizeDensity{
        val currentReso = UtilsDeviceInfoSt.instance(appCtx).getDisplaySizeDensity()
        val resList = getFilteredResList()

        val idx = resList.indexOfFirst {
            it.containsKey("${currentReso.h}x${currentReso.w}")
        }

        var nextReso: SizeDensity? = null

        resList[if (idx > 0) idx-1 else resList.size-1].forEach{
            val resoDetails =  it.value
            nextReso = SizeDensity(resoDetails.resWidth, resoDetails.resHeight, -1)
        }

        val newDen = getNewMatchingDpi(currentReso,nextReso!!)

        return SizeDensity(nextReso!!.w, nextReso!!.h, newDen)
    }


    private suspend fun changeResInternal(providedNextReso: Size?): Boolean = withContext(Dispatchers.IO) {


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

        if (providedNextReso == null) {
            val nextResAndDen = getNextResAndDen()
            nextReso = Size(nextResAndDen.w,nextResAndDen.h)
            nextDen = nextResAndDen.dpi
        } else {
            if (getFilteredResList().indexOfFirst {
                    it.containsKey("${providedNextReso.height}x${providedNextReso.width}")
                } != -1) {
                val currentDensity = UtilsDeviceInfoSt.instance(appCtx).getDisplayDensity()
                nextReso = providedNextReso
                val nexResoH = providedNextReso.height.toFloat()
                val currentReso = UtilsDeviceInfoSt.instance(appCtx).getDisplayResolution()
                val curResH = currentReso.height
                nextDen = ((currentDensity.toFloat() / curResH.toFloat()) * nexResoH).toInt()
            } else {
                return@withContext false
            }
        }

        val nextResStr = "${nextReso.height}x${nextReso.width}"
        val nextResName =  getResName(nextResStr)
        val curResName = getResName(null)


        if (nextResName == "CQHD+" && listOf("WQHD+","CQHD+").indexOf(curResName) != -1){
            ResolutionChangeApi(appCtx).setDisplaySizeDensity(
                displayId,Size(1080, (nextReso.height.toFloat()/nextReso.width.toFloat()*1080f).toInt()),
                (nextDen/nextReso.width*1080)
            )
        }

        val changeResResult = ResolutionChangeApi(appCtx).setDisplaySizeDensity(
            displayId,
            nextReso,
            nextDen
        )

        if (changeResResult) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                UtilRefreshRateSt.instance(appCtx).setPrefOrAdaptOrHighRefreshRateMode(nextResStr, true)
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