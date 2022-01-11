package com.tribalfs.gmh.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.helpers.CacheSettings.currentRefreshRateMode
import com.tribalfs.gmh.helpers.CacheSettings.hasWriteSecureSetPerm
import com.tribalfs.gmh.helpers.CacheSettings.isFakeAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isOfficialAdaptive
import com.tribalfs.gmh.helpers.CacheSettings.isOnePlus
import com.tribalfs.gmh.helpers.CacheSettings.isPowerSaveModeOn
import com.tribalfs.gmh.helpers.CacheSettings.isXiaomi
import com.tribalfs.gmh.helpers.CacheSettings.keepModeOnPowerSaving
import com.tribalfs.gmh.helpers.CacheSettings.lowestHzForAllMode
import com.tribalfs.gmh.helpers.CacheSettings.prrActive
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.MIN_REFRESH_RATE
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.ONEPLUS_RATE_MODE_ALWAYS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.ONEPLUS_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.ONEPLUS_SCREEN_REFRESH_RATE
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.PEAK_REFRESH_RATE
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_ALWAYS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_COVER
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_SEAMLESS
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.REFRESH_RATE_MODE_STANDARD
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.STANDARD_REFRESH_RATE_HZ
import com.tribalfs.gmh.helpers.UtilsDeviceInfo.Companion.USER_REFRESH_RATE
import com.tribalfs.gmh.profiles.ModelNumbers.fordableWithHrrExternal
import com.tribalfs.gmh.profiles.ProfilesInitializer
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import kotlinx.coroutines.ExperimentalCoroutinesApi


internal class UtilsRefreshRate(context: Context) {

    /*companion object{
        private const val TAG = "UtilsRefreshRate"
    }*/

    private val appCtx = context.applicationContext
    private val mContentResolver = appCtx.contentResolver
    private val mUtilsPrefsGmh = UtilsPrefsGmh(appCtx)
    private val mUtilsDeviceInfo = UtilsDeviceInfo(appCtx)

    fun setPeakRefreshRate(refreshRate: Int){
        Settings.System.putString(mContentResolver,PEAK_REFRESH_RATE, refreshRate.toString())
    /*    val cvMaxHz = ContentValues(2);
        cvMaxHz.put("name",PEAK_REFRESH_RATE)
        cvMaxHz.put("value", valueOf(refreshRate.toString()))
        mContentResolver.insert(Uri.parse("content://settings/system"), cvMaxHz)*/
        if (isXiaomi) {
            Settings.System.putString(mContentResolver, USER_REFRESH_RATE, refreshRate.toString())
   /*         val cvMaxHz2 = ContentValues(2);
            cvMaxHz2.put("name", USER_REFRESH_RATE)
            cvMaxHz2.put("value", valueOf(refreshRate.toString()))
            mContentResolver.insert(Uri.parse("content://settings/system"), cvMaxHz2)*/
        }
    }

    internal fun setMinRefreshRate(refreshRate: Int){
        Settings.System.putString(mContentResolver,MIN_REFRESH_RATE, refreshRate.toString())
/*       val cvMinHz = ContentValues(2);
        cvMinHz.put("name",MIN_REFRESH_RATE)
        cvMinHz.put("value", valueOf(refreshRate.toString()))
        mContentResolver.insert(Uri.parse("content://settings/system"), cvMinHz)
*/
    }


    private fun deleteRefreshRate(name: String){
        try {
            mContentResolver.delete(
                Uri.parse("content://settings/system"), "name = ?", arrayOf(
                    name
                )
            )
        } catch (_: Exception) {
        }
    }


    internal fun clearRefreshRate() {
        deleteRefreshRate(PEAK_REFRESH_RATE)
        deleteRefreshRate(MIN_REFRESH_RATE)
    }

    internal fun setRefreshRate(refreshRate: Int): Boolean {
        if (refreshRate > 0) {
            setPeakRefreshRate(refreshRate)
            setMinRefreshRate(if(currentRefreshRateMode.get() == REFRESH_RATE_MODE_ALWAYS && isOfficialAdaptive) refreshRate else lowestHzForAllMode)
        }else{
            clearRefreshRate()
        }
        return true
    }


    @Synchronized
    internal fun setRefreshRateMode(mode: String) : Boolean{
        return try {
            Settings.Secure.putInt(mContentResolver, REFRESH_RATE_MODE, mode.toInt())
                    && (
                    if (fordableWithHrrExternal.indexOf(mUtilsDeviceInfo.deviceModel) != -1) {
                        Settings.Secure.putString(mContentResolver, REFRESH_RATE_MODE_COVER, mode)
                    } else {
                        true
                    })
                    && (
                    if (isOnePlus) {
                        val onePlusModeEq = if (mode == REFRESH_RATE_MODE_ALWAYS) ONEPLUS_RATE_MODE_ALWAYS else ONEPLUS_RATE_MODE_SEAMLESS
                        Settings.Global.putString(mContentResolver, ONEPLUS_SCREEN_REFRESH_RATE, onePlusModeEq)
                    } else {
                        true
                    }
                    )
        }catch(_:java.lang.Exception){false}
    }


    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun getPeakRefreshRate(): Int {
        var prr =
            if (isFakeAdaptive.get()!!) {
                (if (keepModeOnPowerSaving && isPowerSaveModeOn.get() ==true)
                    mUtilsPrefsGmh.hzPrefMaxRefreshRatePsm
                else
                    mUtilsPrefsGmh.hzPrefMaxRefreshRate
                        ).let{
                        if (it != -1){
                            it
                        }else{
                            ProfilesInitializer.instance(appCtx).getResoHighestHzForCurrentMode(null, null).toInt().let{ highestHz ->
                                mUtilsPrefsGmh.hzPrefMaxRefreshRate = highestHz
                                highestHz
                            }
                        }
                    }
            } else {
                mUtilsDeviceInfo.getPeakRefreshRateFromSettings()
            }
        if (prr == null) {
            prr = ProfilesInitializer.instance(appCtx).getResoHighestHzForCurrentMode(null, null).toInt()
        }
        //Log.d(TAG,"getPeakRefreshRate $prr")
        return prr
    }

    @ExperimentalCoroutinesApi
    @SuppressLint("NewApi")
    internal fun setPrefOrAdaptOrHighRefreshRateMode(resStrLxw: String?): Boolean{
        val rrm =
            if (mUtilsPrefsGmh.gmhPrefRefreshRateModePref != null
                && mUtilsPrefsGmh.gmhPrefRefreshRateModePref != REFRESH_RATE_MODE_STANDARD
            ){
                mUtilsPrefsGmh.gmhPrefRefreshRateModePref
            }else{
                if (isOfficialAdaptive) {
                    REFRESH_RATE_MODE_SEAMLESS
                } else {
                    REFRESH_RATE_MODE_ALWAYS
                }
            }
        return tryPrefRefreshRateMode(rrm!!,resStrLxw)
    }


    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun tryPrefRefreshRateMode(rrm: String, resStrLxw: String?) : Boolean {
        if (hasWriteSecureSetPerm) {
            return if (rrm != REFRESH_RATE_MODE_STANDARD) {
                val highest = ProfilesInitializer.instance(appCtx).getResoHighestHzForCurrentMode(resStrLxw, rrm)
                if (highest > STANDARD_REFRESH_RATE_HZ) {
                    setRefreshRate(prrActive.get()!!) && setRefreshRateMode(rrm)
                } else {
                    false
                }
            }else{
                setRefreshRateMode(rrm)
            }
        }
        return false
    }


    /* private fun getPrefOrCurrentRefreshRateMode(): String{
         return mUtilsPrefsGmh.gmhPrefRefreshRateModePref ?: mUtilsDeviceInfo.getSamRefreshRateMode()
     }*/
}