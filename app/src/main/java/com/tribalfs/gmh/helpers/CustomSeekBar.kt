package com.tribalfs.gmh.helpers

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.AppCompatSeekBar

class CustomSeekBar : AppCompatSeekBar, OnSeekBarChangeListener {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {}

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context) : super(context) {}

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
}