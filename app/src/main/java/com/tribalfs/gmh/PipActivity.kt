package com.tribalfs.gmh

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity




class PipActivity : AppCompatActivity() {
    @RequiresApi(api = Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        /* val params = window.attributes
        params.preferredRefreshRate = 120f
        window.attributes = params*/

        val ratio = Rational(1, 1)
        val mBuilder = PictureInPictureParams.Builder()
        mBuilder.setAspectRatio(ratio).build()
        enterPictureInPictureMode(mBuilder.build())

        Handler(Looper.getMainLooper()).postDelayed(Exit(), 400)
    }

    internal inner class Exit : Runnable {
        override fun run() {
            finishAfterTransition()
        }
    }
}