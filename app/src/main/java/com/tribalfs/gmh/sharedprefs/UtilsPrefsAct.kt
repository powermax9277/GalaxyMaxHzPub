package com.tribalfs.gmh.sharedprefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class UtilsPrefsAct(context: Context) {

    companion object{
        private const val GMH_INFO = "gmh_info"
        //private const val TILE_EXPIRY_DAYS = "0x201"
        private const val TRIAL_DAYS = "0x301"
        private const val AD_FREE_AUTO_CHECKED = "0x401"
        private const val ACTIVATION_CODE = "0x501"
        //private const val INS_DATE = "0x601"
        private const val TRIAL_DATE = "0x701"
        private const val SIGNATURE = "0x801"
        internal const val ACT_STATUS = "0x901"
        internal const val LIC_TYPE_INVALID_CODE = 0x201//513
        internal const val LIC_TYPE_TRIAL_EXPIRED = 0x301//769
        internal const val LIC_TYPE_NONE = 0x401//1025
        internal const val LIC_TYPE_NONE_EXP = 0x501//1281
        internal const val LIC_TYPE_ADFREE = 0x601//1537
        internal const val LIC_TYPE_TRIAL_ACTIVE = 0x701//1793
    }

    private val appCtx = context.applicationContext
    private val masterKey = MasterKey.Builder(appCtx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val actSharedPref = EncryptedSharedPreferences.create(
        appCtx,
        GMH_INFO,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val gmhSharedPrefEditor = actSharedPref.edit()

/*    var gmhPrefTileExpiryDays: Int
        get() { return actSharedPref.getInt(TILE_EXPIRY_DAYS, 0xA) }
        set(days){gmhSharedPrefEditor.putInt(TILE_EXPIRY_DAYS, days).apply()}*/

    var gmhPrefPremiumTrialDays: Int
        get() { return (actSharedPref.getInt(TRIAL_DAYS, 0x7)) }
        set(days) {gmhSharedPrefEditor.putInt(TRIAL_DAYS, days).apply()}

    var gmhPrefAdFreeAutoChecked: Boolean
        get() { return actSharedPref.getBoolean(AD_FREE_AUTO_CHECKED, false) }
        set(checked){ gmhSharedPrefEditor.putBoolean(AD_FREE_AUTO_CHECKED, checked).apply() }

    var gmhPrefLicType: Int
        get() {
            actSharedPref.getInt(ACT_STATUS, LIC_TYPE_NONE).apply{
                return if (this == LIC_TYPE_TRIAL_ACTIVE){
                    if (isFreeTrialActive()) {
                        this
                    }else {
                        LIC_TYPE_TRIAL_EXPIRED
                    }
                }else{
                    this
                }
            }
        }
        set(licType) {gmhSharedPrefEditor.putInt(ACT_STATUS, licType).apply()}

    var gmhPrefSignature: String?
        get() { return actSharedPref.getString(SIGNATURE, null) }
        set(devId) {gmhSharedPrefEditor.putString(SIGNATURE, devId).apply()}

    var gmhPrefActivationCode: String?
        get() { return actSharedPref.getString(ACTIVATION_CODE, null) }
        set(code) {gmhSharedPrefEditor.putString(ACTIVATION_CODE, code).apply()}

    var gmhPrefTrialStartDate: String?
        get(){return actSharedPref.getString(TRIAL_DATE, null)}
        set(formattedDateStr){ gmhSharedPrefEditor.putString(TRIAL_DATE, formattedDateStr).apply() }

    private fun gmhPrefGetTrialStartDate(): Date? {
        val dateStr = gmhPrefTrialStartDate
        return if (dateStr == null) {
            null
        } else {
            try {
                stringToDate(dateStr)
            } catch (_: ParseException) {
                null
            }
        }
    }

    /*var gmhPrefInstallDateStr: String?
        get() { return actSharedPref.getString(INS_DATE, null) }
        set(dateStr) {gmhSharedPrefEditor.putString(INS_DATE, dateStr).apply()}


    val gmhPrefGetInstallDate: Date?
        get() {
            val dateStr = gmhPrefInstallDateStr
            return if (dateStr == null) {
                null
            } else {
                try {
                    stringToDate(dateStr)
                } catch (_: ParseException) {
                    null
                }
            }
        }*/

    private var sdf = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())

    private fun stringToDate(dateStr: String): Date {
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(dateStr)!!
        return cal.time
    }

/*
    fun getTileIsExpired(insertNewDate: Boolean): Boolean {
        return if (gmhPrefLicType == LIC_TYPE_ADFREE || gmhPrefLicType == LIC_TYPE_TRIAL_ACTIVE || gmhPrefGetInstallDate == null) {
            if (insertNewDate) {
                gmhPrefInstallDateStr = sdf.format(Calendar.getInstance().time */
/*time converts it to Date object*//*
)
            }
            false
        } else {
            TimeUnit.DAYS.convert(
                Calendar.getInstance().time.time
                    - gmhPrefGetInstallDate!!.time, TimeUnit.MILLISECONDS
            ) > gmhPrefTileExpiryDays
        }
    }
*/

    private fun isFreeTrialActive(): Boolean {
        val rd = getFreeTrialDaysRemaining()
        return rd != null && rd > 0
    }

    fun getFreeTrialDaysRemaining(): Int?{
        val trialDate = gmhPrefGetTrialStartDate()
        return if (trialDate == null) {
            null
        } else {
            val currentTime = Calendar.getInstance().time //time converts it to Date object
            val diff: Long = currentTime.time - trialDate.time
            val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
            return (gmhPrefPremiumTrialDays - days)
        }
    }

}