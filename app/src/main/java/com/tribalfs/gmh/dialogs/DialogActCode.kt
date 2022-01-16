package com.tribalfs.gmh.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.tribalfs.gmh.MainActivity
import com.tribalfs.gmh.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class DialogActCode : RoundedDialogFragment()  {

    companion object {
        internal const val KEY_AC = "0X1"
        internal const val KEY_ST = "0X2"
        internal fun newInstance(ac: String?, showTrial: Boolean): DialogActCode {
            val f = DialogActCode()
            val args = Bundle()
            args.putString(KEY_AC, ac)
            args.putBoolean(KEY_ST, showTrial)
            f.arguments = args
            return f
        }
    }

    private lateinit var input: EditText
    private var actCode: String? = null
    private var showTrial: Boolean? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_AC, input.text.toString())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let{
            actCode = it.getString(KEY_AC)
            showTrial = it.getBoolean(KEY_ST)
        }?:run {
            actCode = savedInstanceState?.getString(KEY_AC, "")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        // val actCode = savedInstanceState?.getString(KEY_AC, "") ?:tag
        input = EditText(context).apply{
            layoutParams =  LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT).apply {
                setMargins(0, 8, 0, 8)
            }
            setText(actCode?:"")
        }

        return AlertDialog.Builder(context).apply {
            setTitle(getString(R.string.in_bd))
            setMessage(getString(R.string.input_ac_inf))
            setView(input)
            setPositiveButton(getString(R.string.apply)) { _, _ -> }
            setNegativeButton(getString(android.R.string.cancel)) { dialogInterface, _ -> dialogInterface.dismiss() }
            if (showTrial == true) setNeutralButton(getString(R.string.actvt_trial)) { _, _ -> }
        }.create().apply {
            setOnShowListener { dialogInterface ->
                (dialogInterface as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).apply {
                    setOnClickListener {
                        input.text.toString().let {
                            if (it.isNotEmpty()) {
                                (requireActivity() as MainActivity).applyActivationCode(it.trim())
                                dialogInterface.dismiss()
                            } else {
                                Toast.makeText(
                                    context,
                                    getString(R.string.ac_none),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                if (showTrial == true) {
                    (dialogInterface).getButton(AlertDialog.BUTTON_NEUTRAL).apply {
                        setOnClickListener {
                            (requireActivity() as MainActivity).syncLicense(
                                silent = false,
                                trial = true
                            )
                            dialogInterface.dismiss()
                        }
                    }
                }

            }
        }
    }

}