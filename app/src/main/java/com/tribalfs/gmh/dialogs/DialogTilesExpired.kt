
package com.tribalfs.gmh.dialogs
/*
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.tribalfs.gmh.MainActivity
import com.tribalfs.gmh.MainActivity.Companion.COUNTDOWN_START
import com.tribalfs.gmh.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.*

class DialogTilesExpired : DialogFragment()  {

    private var currentCount: Long? = null

    companion object {
        private const val CURRENT_COUNT = "current_count"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentCount?.let { outState.putLong(CURRENT_COUNT, it) }
    }

    @ExperimentalCoroutinesApi
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        currentCount = savedInstanceState?.getLong(CURRENT_COUNT, COUNTDOWN_START) ?: COUNTDOWN_START
        isCancelable = false
        return AlertDialog.Builder(context).apply {
            setTitle(getString(R.string.qst_exp))
            setIcon(R.drawable.ic_max_hz)
            setMessage(getString(R.string.info_expired, currentCount))
            setPositiveButton(getString(R.string.ad_starting_in), null)
            setNegativeButton(getString(R.string.no_thanks)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
        }.create().apply {
            setOnShowListener { dialogInterface ->
                val defaultButton: Button =
                    (dialogInterface as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                val positiveButtonText: CharSequence = defaultButton.text
                defaultButton.setOnClickListener {
                    (requireActivity() as MainActivity).showRewardedIntAd()
                    try {
                        dialogInterface.dismiss()
                    } catch (_: Exception) {
                    }
                }
                CoroutineScope(Dispatchers.Main).launch {
                    object : CountDownTimer(currentCount!! * 1000, 100) {
                        override fun onTick(millisUntilFinished: Long) {
                            val secRemaining = millisUntilFinished / 1000
                            defaultButton.text = java.lang.String.format(
                                Locale.getDefault(), "%s %d",
                                positiveButtonText,
                                secRemaining + 1 //add one so it never displays zero
                            )
                            currentCount = secRemaining
                        }

                        override fun onFinish() {
                            if (isShowing) {
                                (requireActivity() as MainActivity).showRewardedIntAd()
                                try {
                                    dialogInterface.dismiss()
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }.start()
                }
            }
        }
    }
}*/
