package com.tribalfs.gmh.helpers

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.tribalfs.gmh.MainActivity
import com.tribalfs.gmh.R
import com.tribalfs.gmh.callbacks.LvlSbMsgCallback
import com.tribalfs.gmh.dialogs.DialogActCode
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct
import kotlinx.coroutines.ExperimentalCoroutinesApi

internal object LvlResultMsg {
   @ExperimentalCoroutinesApi
   @RequiresApi(Build.VERSION_CODES.M)
   fun getString(context: AppCompatActivity, lvlSbMsgCallback: LvlSbMsgCallback, licType: Int?){
        val mUtilsPrefsAct by lazy { UtilsPrefsAct(context.applicationContext) }
        val mUtilsDeviceInfo by lazy { UtilsDeviceInfo(context.applicationContext) }

        when (licType?:mUtilsPrefsAct.gmhPrefLicType){
            UtilsPrefsAct.LIC_TYPE_ADFREE -> {//1537
                lvlSbMsgCallback.onResult(context.getString(R.string.afa), null, null, null)
            }
            UtilsPrefsAct.LIC_TYPE_TRIAL_ACTIVE -> {//1793
                val rtDays = mUtilsPrefsAct.getFreeTrialDaysRemaining()
                lvlSbMsgCallback.onResult(context.resources.getQuantityString(R.plurals.aha, rtDays!!, rtDays), null, null, null)
            }
            UtilsPrefsAct.LIC_TYPE_TRIAL_EXPIRED -> {//769
                lvlSbMsgCallback.onResult(
                    "${context.getString(R.string.afu)}\n${context.getString(R.string.in_bd)}?",
                    android.R.string.ok,{
                        DialogActCode.newInstance(
                            mUtilsPrefsAct.gmhPrefActivationCode,
                            false
                        ).show(context.supportFragmentManager, null)
                    }, Snackbar.LENGTH_INDEFINITE)
            }
            UtilsPrefsAct.LIC_TYPE_INVALID_CODE -> {//513
                lvlSbMsgCallback.onResult(
                    context.getString(R.string.aca),
                    R.string.edit,
                    {
                        DialogActCode.newInstance(
                            mUtilsPrefsAct.gmhPrefActivationCode,
                            mUtilsPrefsAct.gmhPrefLicType != UtilsPrefsAct.LIC_TYPE_TRIAL_ACTIVE &&
                                    mUtilsPrefsAct.getFreeTrialDaysRemaining().let {
                                        it == null || it > 0
                                    }).show(context.supportFragmentManager, null)
                    }, Snackbar.LENGTH_INDEFINITE)
            }
            UtilsPrefsAct.LIC_TYPE_NONE -> {//1025
                lvlSbMsgCallback.onResult(
                    context.getString(R.string.nlh, mUtilsDeviceInfo.deviceModelVariant) +
                            "\n${context.getString(R.string.actvt_trial)}?",
                    android.R.string.ok,
                    {
                        (context as MainActivity).syncLicense(silent = false, tryTrial = true)
                    }, Snackbar.LENGTH_INDEFINITE)
            }
            UtilsPrefsAct.LIC_TYPE_NONE_EXP -> {//1281
                lvlSbMsgCallback.onResult(
                    context.getString(R.string.nlh, mUtilsDeviceInfo.deviceModelVariant) +
                            "\n${context.getString(R.string.in_bd)}?",
                    android.R.string.ok,
                    {
                        DialogActCode.newInstance(
                            mUtilsPrefsAct.gmhPrefActivationCode,
                            false
                        ).show(context.supportFragmentManager, null)
                    }, Snackbar.LENGTH_INDEFINITE)
            }
        }
    }
}