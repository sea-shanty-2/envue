package dk.cs.aau.envue.utility

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.DisplayMetrics
import java.util.concurrent.ConcurrentHashMap

private val cacheMap = ConcurrentHashMap<String, Bitmap>()

fun textToBitmap(text: String, dp: Int, context: Context): Bitmap {
    return textToBitmap(text, dp, context, false)
}

fun textToBitmap(text: String, dp: Int, context: Context, toScale: Boolean, clearCache: Boolean = false): Bitmap {
    val existing = cacheMap[text]

    if (existing == null || clearCache) {
        val size = if (toScale) calculateSize(dp, context) else dp
        val bitmap: Bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_4444)
        val canvas = Canvas(bitmap)
        val paint = generateTextPaint(text, size)
        val (xPos, yPos) = calculatePosition(canvas, paint)

        canvas.drawText(text, xPos, yPos, paint)
        canvas.density = DisplayMetrics.DENSITY_MEDIUM
        bitmap.density = DisplayMetrics.DENSITY_MEDIUM

        cacheMap[text] = bitmap

        return bitmap
    } else {
        return existing
    }
}

private fun calculatePosition(canvas: Canvas, paint: TextPaint): Pair<Float, Float> {
    val xPos: Float = (canvas.height / 2).toFloat()
    val yPos: Float = canvas.width / 2 - (paint.descent() + paint.ascent()) / 2
    return Pair(xPos, yPos)
}

private fun generateTextPaint(text: String, size: Int): TextPaint {
    val paint = TextPaint()
    val tempSize = 48f
    val bounds = Rect()

    paint.textSize = tempSize
    paint.getTextBounds(text, 0, text.length, bounds)

    val desiredSize = tempSize * size / bounds.width()

    paint.textSize = desiredSize
    paint.textAlign = Paint.Align.CENTER

    return paint
}

private fun calculateSize(dp: Int, context: Context): Int {
    val scale = context.resources.displayMetrics.density
    return (dp * scale).toInt()
}