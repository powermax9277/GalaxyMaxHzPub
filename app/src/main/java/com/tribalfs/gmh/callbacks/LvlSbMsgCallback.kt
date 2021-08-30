package com.tribalfs.gmh.callbacks

import android.view.View

interface LvlSbMsgCallback {
  fun onResult(msg: String, actionStr: Int?, action:View.OnClickListener?, sbLen: Int?)
}