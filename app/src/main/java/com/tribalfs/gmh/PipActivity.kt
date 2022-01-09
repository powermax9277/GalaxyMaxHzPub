package com.tribalfs.gmh

import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.RequiresApi
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.app.PictureInPictureParams
import android.os.Handler
import android.os.Looper

class PipActivity : AppCompatActivity() {
    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ratio = Rational(1, 1)
        val mBuilder = PictureInPictureParams.Builder()
        mBuilder.setAspectRatio(ratio).build()
        enterPictureInPictureMode(mBuilder.build())
        Handler(Looper.getMainLooper()).postDelayed(exit(), 500)
    }

    internal inner class exit : Runnable {
        override fun run() {
            finishAfterTransition()
        }
    }
}