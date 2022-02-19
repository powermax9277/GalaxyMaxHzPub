package com.tribalfs.gmh.helpers

import android.graphics.*
import android.graphics.drawable.Icon
import android.graphics.drawable.Icon.createWithBitmap
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.min

private const val ICON_SIZE = 96f
private const val TOP_TEXT_SIZE = ICON_SIZE * 0.70f
private const val TOP_TEXT_BASE = ICON_SIZE * 0.52f
private const val BOTTOM_TEXT_SIZE = ICON_SIZE * 0.52f
private const val BOTTOM_TEXT_BASE = ICON_SIZE * 0.9375f


class UtilNotifIcon{

    private val mIconTopText = Paint().apply{
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private val mIconBottomText = Paint().apply{
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = BOTTOM_TEXT_SIZE
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private val mIconBitmapHz: Bitmap = Bitmap.createBitmap(ICON_SIZE.toInt(), ICON_SIZE.toInt(), Bitmap.Config.ARGB_8888)
    private val mIconCanvasHz = Canvas(mIconBitmapHz)

    @RequiresApi(Build.VERSION_CODES.M)
    internal fun getIcon(topStr: String, botStr: String): Icon {
        mIconTopText.textSize = TOP_TEXT_SIZE
        mIconBottomText.textSize = BOTTOM_TEXT_SIZE
        mIconTopText.textSize = min(TOP_TEXT_SIZE * ICON_SIZE / mIconTopText.measureText(topStr),TOP_TEXT_SIZE)
        mIconBottomText.textSize = min(BOTTOM_TEXT_SIZE * ICON_SIZE / mIconBottomText.measureText(botStr),BOTTOM_TEXT_SIZE)
        mIconCanvasHz.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        mIconCanvasHz.drawText(topStr, ICON_SIZE /2, TOP_TEXT_BASE, mIconTopText)
        mIconCanvasHz.drawText(botStr, ICON_SIZE /2, BOTTOM_TEXT_BASE, mIconBottomText)
        return createWithBitmap(mIconBitmapHz)
    }

}