package com.tribalfs.gmh.tiles

import android.graphics.*
import android.graphics.drawable.Icon
import android.graphics.drawable.Icon.createWithBitmap
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.min


@RequiresApi(Build.VERSION_CODES.M)
object TileIcons{

        private const val ICON_SIZE = 100f
      //  private const val TOP_TEXT_SIZE = ICON_SIZE * 1.0f
        private const val TOP_TEXT_SIZE_LIMIT = ICON_SIZE * 0.55f//.60f
        private const val TOP_TEXT_BASE = ICON_SIZE * 0.80f
        private const val BOTTOM_TEXT_SIZE = ICON_SIZE * 0.25f
        private const val BOTTOM_TEXT_BASE = ICON_SIZE * 0.95f//92f

    //private val mTypeFace = Typeface.create("agency_fb", Typeface.BOLD)//Typeface.createFromAsset(context.applicationContext.assets,"agency-fb.ttf")

    internal fun getIcon(hz: String, type: String): Icon {
        val mIconTopTextHz = Paint()
        mIconTopTextHz.color = Color.WHITE
        mIconTopTextHz.isAntiAlias = true
        mIconTopTextHz.textAlign = Paint.Align.CENTER

        val mIconBottomTextHz = Paint()
        mIconBottomTextHz.color = Color.WHITE
        mIconBottomTextHz.isAntiAlias = true
        mIconBottomTextHz.textAlign = Paint.Align.CENTER
        val mIconBitmapHz: Bitmap = Bitmap.createBitmap(ICON_SIZE.toInt(), ICON_SIZE.toInt(), Bitmap.Config.ARGB_8888)
        val mIconCanvasHz = Canvas(mIconBitmapHz)

        mIconTopTextHz.textSize = ICON_SIZE
        mIconBottomTextHz.textSize = BOTTOM_TEXT_SIZE
        val newSize = min(ICON_SIZE * ICON_SIZE / mIconTopTextHz.measureText(hz), TOP_TEXT_SIZE_LIMIT)
        val adj = ((ICON_SIZE - newSize)/3f).coerceAtLeast(0f)
        mIconTopTextHz.textSize = newSize
        mIconBottomTextHz.textSize = min(BOTTOM_TEXT_SIZE * ICON_SIZE / mIconBottomTextHz.measureText(type), BOTTOM_TEXT_SIZE)
        mIconCanvasHz.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

     /*   val mIconCircle = Paint()
        mIconCircle.style = Paint.Style.STROKE
        mIconCircle.strokeWidth = 4f
        mIconCircle.color = Color.WHITE
        mIconCircle.isAntiAlias = true

        mIconCanvasHz.drawCircle(ICON_SIZE/2, ICON_SIZE/2, 45f, mIconCircle);*/

        mIconCanvasHz.drawText(hz, ICON_SIZE/2, TOP_TEXT_BASE - adj, mIconTopTextHz)
        mIconCanvasHz.drawText(type, ICON_SIZE/2, BOTTOM_TEXT_BASE, mIconBottomTextHz)
        return createWithBitmap(mIconBitmapHz)
    }

}