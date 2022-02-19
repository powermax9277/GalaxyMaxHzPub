package com.tribalfs.gmh.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.tribalfs.gmh.ACTION_CLOSE_MAIN_ACTIVITY
import com.tribalfs.gmh.KEY_JSON_LIC_TYPE
import com.tribalfs.gmh.R
import com.tribalfs.gmh.helpers.UtilRefreshRateSt
import com.tribalfs.gmh.profiles.KEY_JSON_ACTIVATION_CODE
import com.tribalfs.gmh.profiles.KEY_JSON_SIGNATURE
import com.tribalfs.gmh.profiles.KEY_JSON_TRIAL_DAYS
import com.tribalfs.gmh.profiles.KEY_JSON_TRIAL_START_DATE
import com.tribalfs.gmh.sharedprefs.UtilsPrefsAct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class DonationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "com.tribalfs.gmh.action_DONATED" -> {
                CoroutineScope(Dispatchers.IO).launch {

                    val mUtilsPrefsAct by lazy { UtilsPrefsAct(context) }

                    val activationCode = intent.getStringExtra("dc")

                    if (!activationCode.isNullOrEmpty()) {

                        val resultJson = UtilRefreshRateSt.instance(context.applicationContext).mSyncer.syncLicense(activationCode, false)

                        if (resultJson != null) {

                            context.applicationContext.sendBroadcast(Intent(ACTION_CLOSE_MAIN_ACTIVITY))
                            mUtilsPrefsAct.apply {
                                gmhPrefSignature = resultJson[KEY_JSON_SIGNATURE] as String
                                gmhPrefTrialStartDate =
                                    resultJson[KEY_JSON_TRIAL_START_DATE] as String
                                gmhPrefPremiumTrialDays = resultJson[KEY_JSON_TRIAL_DAYS] as Int
                                gmhPrefActivationCode =
                                    resultJson[KEY_JSON_ACTIVATION_CODE] as String//set to trial if trial
                                gmhPrefLicType = resultJson[KEY_JSON_LIC_TYPE] as Int
                            }

                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    context.applicationContext,
                                    "${context.applicationContext.getString(R.string.app_name)} ${context.applicationContext.getString(R.string.afa)}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

}

