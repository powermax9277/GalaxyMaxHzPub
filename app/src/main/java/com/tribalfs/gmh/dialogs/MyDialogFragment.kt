package com.tribalfs.gmh.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.DialogFragment
import com.tribalfs.gmh.R

open class MyDialogFragment : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Set transparent background and no title
        if (dialog != null && dialog!!.window != null) {
            dialog!!.window!!.setBackgroundDrawable(
                getDrawable(
                    requireContext(),
                    R.drawable.rounded_edge
                )
            )
            dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        }
        return inflater.inflate(R.layout.dialog_rounded, container, false)
    }
}