package com.tribalfs.gmh.callbacks

import android.view.View
import kotlinx.coroutines.ExperimentalCoroutinesApi

interface MyClickHandler {
    @ExperimentalCoroutinesApi
    fun onClickView(v: View)
}