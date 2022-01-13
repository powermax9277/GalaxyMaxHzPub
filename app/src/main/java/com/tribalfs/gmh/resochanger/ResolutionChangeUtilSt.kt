package com.tribalfs.gmh.resochanger

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.MainActivity
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.*
import com.tribalfs.gmh.helpers.CacheSettings.displayId
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isMultiResolution
import com.tribalfs.gmh.helpers.CacheSettings.isSamsung
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.profiles.ProfilesInitializer
import com.tribalfs.gmh.profiles.ResolutionDetails
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.*


@ExperimentalCoroutinesApi
@RequiresApi(Build.VERSION_CODES.M)
class ResolutionChangeUtilSt private constructor(val context: Context) {

    companion object : SingletonHolder<ResolutionChangeUtilSt, Context>(::ResolutionChangeUtilSt){
        // private const val TAG = "ResolutionChangeUtil"
    }

    private val appCtx = context.applicationContext
    private val mUtilsDeviceInfo by lazy { UtilsDeviceInfo(appCtx) }
    private val mUtilsPrefsGmh  by lazy {UtilsPrefsGmh(appCtx)}
    private val mUtilsRefreshRate by lazy { UtilsRefreshRate(appCtx) }

    @ExperimentalCoroutinesApi
    suspend fun changeRes(resLxw: String?): Int? {
        return if (hasWriteSecureSetPerm) {
            appCtx.sendBroadcast(Intent(MainActivity.ACTION_CHANGED_RES))
            delay(550)
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
            UtilsPermSt.instance(appCtx).getPerm(UtilsSettings.SECURE)
        }
    }

    private fun getFilteredResList(): List<Map<String, ResolutionDetails>>{
        return ProfilesInitializer.instance(context).getResolutionsForKey(null)?.filter {
            val key = it.keys.first()
            val res = "$key(${it[key]?.resName})"
            (mUtilsPrefsGmh.gmhPrefGetSkippedRes?.contains(res) != true)
        }!!
    }

    internal fun getResName(resLxw: String?): String {
        val reso = resLxw?:ProfilesInitializer.instance(context).getCurrentResLxw()
        ProfilesInitializer.instance(context).getResolutionsForKey(null)?.forEach{
            it[reso]?.let{res ->
                return res.resName
            }
        }

        reso.split("x").let {
            return UtilsResoName.getName(it[0].toInt(),it[1].toInt())
        }

    }

    private fun getNextResAndDen(density : Int) : List<String>{
        val currentResString = ProfilesInitializer.instance(context).getCurrentResLxw()
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


    private suspend fun changeResInternal(resLxw: String?): Boolean = withContext(Dispatchers.Default) {

        val currentDensity = mUtilsDeviceInfo.getDensity().toInt()

        if (!isMultiResolution) {
            launch(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.one_res_def),
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
                val currentResString = ProfilesInitializer.instance(context).getCurrentResLxw()
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
                ResolutionChangeApiSt.instance(appCtx).setDisplayResolution(
                    displayId,
                    "${(nextResSplit[0].toFloat()/nextResSplit[1].toFloat() * 1080f).toInt()}x1080",
                    null
                )
            }
        }

        val changeResResult = ResolutionChangeApiSt.instance(appCtx).setDisplayResolution(
            displayId,
            nextRes,
            nextDen
        )

        delay(300)
        if (changeResResult) {
            mUtilsRefreshRate.setPrefOrAdaptOrHighRefreshRateMode(nextRes)
            launch(Dispatchers.Main){
                Toast.makeText(
                    context,
                    "$nextRes screen resolution applied.",
                    Toast.LENGTH_LONG
                ).show()
            }
            delay(250)//don't remove
        }else{
            if (hasWriteSecureSetPerm){
                launch(Dispatchers.Main){
                    Toast.makeText(
                        context,
                        "Unable to execute change resolution command successfully. Try again after rebooting this device.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        return@withContext changeResResult
    }
}