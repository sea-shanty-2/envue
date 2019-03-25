package dk.cs.aau.ensight.Utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.DisplayMetrics

fun textToBitmap(text: String, dp: Int, context: Context): Bitmap {
    val size = calculateSize(dp, context)
    val bitmap: Bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_4444)
    val canvas = Canvas(bitmap)
    val paint = generateTextPaint(text, size)
    val (xpos, ypos) = calculatePosition(canvas, paint)

    canvas.drawText(text, xpos, ypos, paint)
    canvas.density = DisplayMetrics.DENSITY_MEDIUM
    bitmap.density = DisplayMetrics.DENSITY_MEDIUM

    return bitmap
}

fun calculatePosition(canvas: Canvas, paint: TextPaint): Pair<Float, Float> {
    val xpos: Float = (canvas.height / 2).toFloat()
    val ypos: Float = canvas.width / 2 - (paint.descent() + paint.ascent()) / 2
    return Pair(xpos, ypos)
}

fun generateTextPaint(text: String, size: Int): TextPaint {
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

fun calculateSize(dp: Int, context: Context): Int {
    var scale = context.resources.displayMetrics.density
    return (dp * scale).toInt()
}