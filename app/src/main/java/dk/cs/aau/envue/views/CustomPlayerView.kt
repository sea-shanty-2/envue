package dk.cs.aau.envue.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.exoplayer2.ui.PlayerView

class CustomPlayerView(context: Context, attrs: AttributeSet?) : PlayerView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth
        // Force height to match 16:9
        val height = Math.round(width * 9/16f)
        setMeasuredDimension(width, height)
    }
}