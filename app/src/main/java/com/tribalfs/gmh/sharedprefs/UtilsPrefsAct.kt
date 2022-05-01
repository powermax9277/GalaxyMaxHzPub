package com.tribalfs.gmh.sharedprefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val GMH_INFO = "gmh_info"
private const val TRIAL_DAYS = "0x301"
private const val AD_FREE_AUTO_CHECKED = "0x401"
private const val ACTIVATION_CODE = "0x501"
private const val TRIAL_DATE = "0x701"
private const val SIGNATURE = "0x801"
internal const val ACT_STATUS = "0x901"
internal const val LIC_TYPE_INVALID_CODE = 0x201//513
internal const val LIC_TYPE_TRIAL_EXPIRED = 0x301//769
internal const val LIC_TYPE_NONE = 0x401//1025
internal const val LIC_TYPE_NONE_EXP = 0x501//1281
internal const val LIC_TYPE_ADFREE = 0x601//1537
internal const val LIC_TYPE_TRIAL_ACTIVE = 0x701//1793


class UtilsPrefsAct(val context: Context) {

    private lateinit var actSharedPref: SharedPreferences
    private lateinit var gmhSharedPrefEditor: SharedPreferences.Editor

    init{
        createSharedPreferences()
    }

    @Synchronized
    fun createSharedPreferences() {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        actSharedPref = EncryptedSharedPreferences.create(
            context,
        GMH_INFO,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        gmhSharedPrefEditor = actSharedPref.edit()
    }



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

    private fun gmhPrefGetTrialStartDate(): LocalDate? {
        val dateStr = gmhPrefTrialStartDate
        return if (dateStr == null) {
            null
        } else {
            try {
                LocalDate.parse(dateStr.split(" ")[0], DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            } catch (_: Exception) {
                try{
                    LocalDate.parse(dateStr.split(" ")[0], DateTimeFormatter.ofPattern("MM-dd-yyyy"))
                }catch (_: Exception) {
                    null
                }
            }
        }
    }

    private fun isFreeTrialActive(): Boolean {
        val rd = getFreeTrialDaysRemaining()
        return rd != null && rd > 0
    }

    fun getFreeTrialDaysRemaining(): Int?{
        val trialDate = gmhPrefGetTrialStartDate()
        return if (trialDate == null) {
            null
        } else {
            val currentDate = LocalDate.now() //time converts it to Date object
            val days = trialDate.until(currentDate).days
            return (gmhPrefPremiumTrialDays - days)
        }
    }

}