package com.tribalfs.gmh.helpers

import android.graphics.*
import android.graphics.drawable.Icon
import android.graphics.drawable.Icon.createWithBitmap
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.min


@RequiresApi(Build.VERSION_CODES.M)
class UtilTileIcon{

    companion object {
        private const val ICON_SIZE = 100f
        private const val TOP_TEXT_SIZE_LIMIT = ICON_SIZE * 0.55f//.60f
        private const val TOP_TEXT_BASE = ICON_SIZE * 0.80f
        private const val BOTTOM_TEXT_SIZE = ICON_SIZE * 0.25f
        private const val BOTTOM_TEXT_BASE = ICON_SIZE * 0.95f//92f
    }

    private val mIconTopText = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val mIconBottomText = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val mIconBitmap: Bitmap = Bitmap.createBitmap(ICON_SIZE.toInt(), ICON_SIZE.toInt(), Bitmap.Config.ARGB_8888)
    private val mIconCanvas = Canvas(mIconBitmap)

    internal fun getIcon(topStr: String, botStr: String): Icon {

        mIconTopText.textSize = ICON_SIZE
        mIconBottomText.textSize = BOTTOM_TEXT_SIZE

        val newSize = min(ICON_SIZE * ICON_SIZE / mIconTopText.measureText(topStr), TOP_TEXT_SIZE_LIMIT)
        val adj = ((ICON_SIZE - newSize)/3f).coerceAtLeast(0f)

        mIconTopText.textSize = newSize
        mIconBottomText.textSize = min(BOTTOM_TEXT_SIZE * ICON_SIZE / mIconBottomText.measureText(botStr), BOTTOM_TEXT_SIZE)
        mIconCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        mIconCanvas.drawText(topStr, ICON_SIZE /2, TOP_TEXT_BASE - adj, mIconTopText)
        mIconCanvas.drawText(botStr, ICON_SIZE /2, BOTTOM_TEXT_BASE, mIconBottomText)
        return createWithBitmap(mIconBitmap)
    }

}