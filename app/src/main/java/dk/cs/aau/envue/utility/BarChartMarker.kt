package dk.cs.aau.envue.utility

import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.data.CandleEntry
import android.content.Context
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.Utils
import dk.cs.aau.envue.R


class BarChartMarker(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvContent: TextView

    init {

        tvContent = findViewById<View>(R.id.tvContent) as TextView
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    override fun refreshContent(e: Entry, highlight: Highlight) {

        if (e is Entry) {
            tvContent.text = "" + Utils.formatNumber(e.getY(), 0, true)
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}
