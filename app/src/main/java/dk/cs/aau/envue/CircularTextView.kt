package dk.cs.aau.envue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.text.emoji.widget.EmojiTextView
import android.support.v4.content.ContextCompat
import android.util.AttributeSet


class CircularTextView : EmojiTextView {
    private var strokeWidth: Float = 0f
    internal var strokeColor: Int = R.color.white
    internal var solidColor: Int = R.color.white

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}


    override fun draw(canvas: Canvas) {

        val circlePaint = Paint()
        val strokePaint = Paint()
        val color = if (isSelected) ContextCompat.getColor(context, R.color.envue_light)
                    else Color.TRANSPARENT

        circlePaint.apply {
            setColor(color)
            flags = Paint.ANTI_ALIAS_FLAG
        }
        strokePaint.apply {
            setColor(color)
            flags = Paint.ANTI_ALIAS_FLAG
        }

        val h = this.height / 5
        val w = this.width / 5

        val diameter = if (h > w) h else w
        val radius = diameter / 2

        this.height = diameter
        this.width = diameter

        canvas.drawCircle(diameter / 2f, diameter / 2f, radius.toFloat(), strokePaint)

        canvas.drawCircle(diameter / 2f, diameter / 2f, radius.toFloat() - strokeWidth, circlePaint)

        super.draw(canvas)
    }

    fun setStrokeWidth(dp: Int) {
        val scale = context.resources.displayMetrics.density
        strokeWidth = dp * scale

    }

    fun setSelectionMarkerColor(resourceId: Int) {
        strokeColor = resourceId
        solidColor = resourceId
    }
}