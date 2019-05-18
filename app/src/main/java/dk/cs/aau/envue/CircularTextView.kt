package dk.cs.aau.envue

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.text.emoji.widget.EmojiTextView
import android.support.v4.content.ContextCompat
import android.widget.TextView
import dk.cs.aau.envue.utility.EmojiIcon


class CircularTextView(context: Context,
                       private val emojiIcon: EmojiIcon) : EmojiTextView(context) {

    private var strokeWidth: Float = 0f

    init {
        // Set visual properties
        alpha = 1f
        textSize = 36f
        text = emojiIcon.char
        textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        setTextColor(Color.BLACK)  // Makes bitmap (the emoji) non-transparent


        // Mark as selected when pressed
        setOnClickListener {

            val startSize = if (isSelected) 42f else 36f
            val endSize = if (isSelected) 36f else 42f

            // Start the font-size animation
            ValueAnimator.ofFloat(startSize, endSize).apply {
                addUpdateListener { valueAnimator ->
                    textSize = valueAnimator.animatedValue as Float }
                duration = 300
                start()
            }

            apply {
                isSelected = !isSelected
            }
        }
    }


    override fun draw(canvas: Canvas) {

        val circlePaint = Paint()
        val strokePaint = Paint()
        val color = if (isSelected) ContextCompat.getColor(context, R.color.colorAccent)
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

    fun getEmoji(): EmojiIcon {
        return emojiIcon
    }
}