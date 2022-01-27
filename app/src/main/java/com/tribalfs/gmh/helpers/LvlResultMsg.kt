package com.tribalfs.gmh.helpers

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.tribalfs.gmh.MainActivity
import com.tribalfs.gmh.R
import com.tribalfs.gmh.callbacks.LvlSbMsgCallback
import com.tribalfs.gmh.dialogs.DialogActCode
import com.tribalfs.gmh.sharedprefs.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

internal class LvlResultMsg(
    private val appCompatActivity: AppCompatActivity,
    private val mUtilsPrefsAct: UtilsPrefsAct,
    private val modelVariant: String) {

    
   @ExperimentalCoroutinesApi
   fun showMsg(lvlSbMsgCallback: LvlSbMsgCallback, licType: Int?){

        when (licType?:mUtilsPrefsAct.gmhPrefLicType){
            LIC_TYPE_ADFREE -> {//1537
                lvlSbMsgCallback.onResult(appCompatActivity.getString(R.string.afa), null, null, null)
            }

            LIC_TYPE_TRIAL_ACTIVE -> {//1793
                val rtDays = mUtilsPrefsAct.getFreeTrialDaysRemaining()
                lvlSbMsgCallback.onResult(appCompatActivity.resources.getQuantityString(R.plurals.aha, rtDays!!, rtDays), null, null, null)
            }

            LIC_TYPE_TRIAL_EXPIRED -> {//769
                lvlSbMsgCallback.onResult(
                    "${appCompatActivity.getString(R.string.afu)}\n${appCompatActivity.getString(R.string.in_bd)}?",
                    android.R.string.ok,{
                        DialogActCode.newInstance(
                            mUtilsPrefsAct.gmhPrefActivationCode,
                            false
                        ).show(appCompatActivity.supportFragmentManager, null)
                    }, Snackbar.LENGTH_INDEFINITE)
            }

            LIC_TYPE_INVALID_CODE -> {//513
                lvlSbMsgCallback.onResult(
                    appCompatActivity.getString(R.string.aca),
                    R.string.edit,
                    {
                        DialogActCode.newInstance(
                            mUtilsPrefsAct.gmhPrefActivationCode,
                            mUtilsPrefsAct.gmhPrefLicType != LIC_TYPE_TRIAL_ACTIVE &&
                                    mUtilsPrefsAct.getFreeTrialDaysRemaining().let {
                                        it == null || it > 0
                                    }).show(appCompatActivity.supportFragmentManager, null)
                    }, Snackbar.LENGTH_INDEFINITE)
            }

            LIC_TYPE_NONE -> {//1025
                lvlSbMsgCallback.onResult(
                    appCompatActivity.getString(R.string.nlh, modelVariant) +
                            "\n${appCompatActivity.getString(R.string.actvt_trial)}?",
                    android.R.string.ok,
                    {
                        (appCompatActivity as MainActivity).syncLicense(silent = false, trial = true)
                    }, Snackbar.LENGTH_INDEFINITE)
            }

            LIC_TYPE_NONE_EXP -> {//1281
                lvlSbMsgCallback.onResult(
                    appCompatActivity.getString(R.string.nlh, modelVariant) +
                            "\n${appCompatActivity.getString(R.string.in_bd)}?",
                    android.R.string.ok,
                    {
                        DialogActCode.newInstance(
                            mUtilsPrefsAct.gmhPrefActivationCode,
                            false
                        ).show(appCompatActivity.supportFragmentManager, null)
                    }, Snackbar.LENGTH_INDEFINITE)
            }

        }
    }
}