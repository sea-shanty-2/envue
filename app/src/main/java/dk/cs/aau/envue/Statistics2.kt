package dk.cs.aau.envue

import android.widget.SeekBar
import android.content.pm.PackageManager
import android.Manifest.permission
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.support.v4.content.ContextCompat
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import android.support.v4.content.ContextCompat.startActivity
import android.content.Intent
import android.graphics.Color
import com.github.mikephil.charting.data.LineData
import android.graphics.drawable.Drawable
import com.github.mikephil.charting.utils.Utils.getSDKInt
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.formatter.IFillFormatter
import android.graphics.DashPathEffect
import com.github.mikephil.charting.components.Legend.LegendForm
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.XAxis
import android.view.WindowManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import android.widget.SeekBar.OnSeekBarChangeListener
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.Utils
import dk.cs.aau.envue.utility.BarChartMarker
import dk.cs.aau.envue.utility.ChartBase


class LineChartActivity1 : ChartBase(), OnChartValueSelectedListener {

    private var chart: LineChart? = null
    private var seekBarX: SeekBar? = null
    private var seekBarY: SeekBar? = null
    private var tvX: TextView? = null
    private var tvY: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_statistics)

        setTitle("LineChartActivity1")

        tvX = findViewById(R.id.tvXMax)
        tvY = findViewById(R.id.tvYMax)


        run {
            // // Chart Style // //
            chart = findViewById(R.id.chart1)

            // background color
            chart!!.setBackgroundColor(Color.WHITE)

            // disable description text
            chart!!.description.isEnabled = false

            // enable touch gestures
            chart!!.setTouchEnabled(true)

            // set listeners
            chart!!.setOnChartValueSelectedListener(this)
            chart!!.setDrawGridBackground(false)

            // create marker to display box when values are selected
            val mv = BarChartMarker(this, R.layout.marker_barchart)

            // Set the marker to the chart
            mv.setChartView(chart)
            chart!!.marker = mv

            // enable scaling and dragging
            chart!!.isDragEnabled = true
            chart!!.setScaleEnabled(true)
            // chart.setScaleXEnabled(true);
            // chart.setScaleYEnabled(true);

            // force pinch zoom along both axis
            chart!!.setPinchZoom(true)
        }

        val xAxis: XAxis
        run {
            // // X-Axis Style // //
            xAxis = chart!!.xAxis

            // vertical grid lines
            xAxis.enableGridDashedLine(10f, 10f, 0f)
        }

        val yAxis: YAxis
        run {
            // // Y-Axis Style // //
            yAxis = chart!!.axisLeft

            // disable dual axis (only use LEFT axis)
            chart!!.axisRight.isEnabled = false

            // horizontal grid lines
            yAxis.enableGridDashedLine(10f, 10f, 0f)

            // axis range
            yAxis.axisMaximum = 200f
            yAxis.axisMinimum = -50f
        }


        run {
            // // Create Limit Lines // //
            val llXAxis = LimitLine(9f, "Index 10")
            llXAxis.lineWidth = 4f
            llXAxis.enableDashedLine(10f, 10f, 0f)
            llXAxis.labelPosition = LimitLabelPosition.RIGHT_BOTTOM
            llXAxis.textSize = 10f

            val ll1 = LimitLine(150f, "Upper Limit")
            ll1.lineWidth = 4f
            ll1.enableDashedLine(10f, 10f, 0f)
            ll1.labelPosition = LimitLabelPosition.RIGHT_TOP
            ll1.textSize = 10f

            val ll2 = LimitLine(-30f, "Lower Limit")
            ll2.lineWidth = 4f
            ll2.enableDashedLine(10f, 10f, 0f)
            ll2.labelPosition = LimitLabelPosition.RIGHT_BOTTOM
            ll2.textSize = 10f

            // draw limit lines behind data instead of on top
            yAxis.setDrawLimitLinesBehindData(true)
            xAxis.setDrawLimitLinesBehindData(true)

            // add limit lines
            yAxis.addLimitLine(ll1)
            yAxis.addLimitLine(ll2)
            //xAxis.addLimitLine(llXAxis);
        }

        val keys = intent.extras?.keySet()
        val extrasFromKeys = keys?.map { k -> intent.extras?.get(k) }

        //val joined = (intent.extras?.get("joinedTimestamps") as Array<Int>).apply {sort()}
        //val left = (intent.extras?.get("leftTimestamps") as Array<Int>).apply {sort()}

        val joined = arrayOf(12340, 12345, 12350)
        val left = arrayOf(12360, 12380, 12382)

        // add data
        seekBarX!!.progress = 45
        seekBarY!!.progress = 180
        setData(joined.map { i -> i - joined.min()!! }.toTypedArray(),
            left.map { i -> i - joined.min()!! }.toTypedArray())

        // draw points over time
        chart!!.animateX(1500)

        // get the legend (only possible after setting data)
        val l = chart!!.legend

        // draw legend entries as lines
        l.form = LegendForm.LINE
    }

    private fun setData(u: Array<Int>, v: Array<Int>) {
        val values = generateChartData(u, v)

        val set1: LineDataSet

        if (chart!!.data != null && chart!!.data.dataSetCount > 0) {
            set1 = chart!!.data.getDataSetByIndex(0) as LineDataSet
            set1.values = values
            set1.notifyDataSetChanged()
            chart!!.data.notifyDataChanged()
            chart!!.notifyDataSetChanged()
        } else {
            // create a dataset and give it a type
            set1 = LineDataSet(values, "DataSet 1")

            set1.setDrawIcons(false)

            // draw dashed line
            set1.enableDashedLine(10f, 5f, 0f)

            // black lines and points
            set1.color = Color.BLACK
            set1.setCircleColor(Color.BLACK)

            // line thickness and point size
            set1.lineWidth = 1f
            set1.circleRadius = 3f

            // draw points as solid circles
            set1.setDrawCircleHole(false)

            // customize legend entry
            set1.formLineWidth = 1f
            set1.formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            set1.formSize = 15f

            // text size of values
            set1.valueTextSize = 9f

            // draw selection line as dashed
            set1.enableDashedHighlightLine(10f, 5f, 0f)

            // set the filled area
            set1.setDrawFilled(true)
            set1.fillFormatter = IFillFormatter { dataSet, dataProvider -> chart!!.axisLeft.axisMinimum }

            // set color of filled area
            if (Utils.getSDKInt() >= 18) {
                // drawables only supported on api level 18 and above
                val drawable = ContextCompat.getDrawable(this, R.drawable.fade_green)
                set1.fillDrawable = drawable
            } else {
                set1.fillColor = Color.BLACK
            }

            val dataSets = ArrayList<LineDataSet>()
            dataSets.add(set1) // add the data sets

            // create a data object with the data sets
            val data = LineData(dataSets.toList())

            // set data
            chart!!.data = data
        }
    }

    protected override fun saveToGallery() {
        saveToGallery(chart!!, "LineChartActivity1")
    }

    override fun onValueSelected(e: Entry, h: Highlight) {
        Log.i("Entry selected", e.toString())
        Log.i("LOW HIGH", "low: " + chart!!.lowestVisibleX + ", high: " + chart!!.highestVisibleX)
        Log.i(
            "MIN MAX",
            "xMin: " + chart!!.xChartMin + ", xMax: " + chart!!.xChartMax + ", yMin: " + chart!!.yChartMin + ", yMax: " + chart!!.yChartMax
        )
    }

    override fun onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.")
    }

    private fun generateChartData(u: Array<Int>, v: Array<Int>): MutableList<Entry> {
        // Generate placeholders for y

        fun merge(u: Array<Int>, v: Array<Int>): ArrayList<Pair<Int, Boolean>> {
            var i = 0
            var j = 0
            val merged = ArrayList<Pair<Int, Boolean>>()

            while (i < u.size && j < v.size) {
                if (u[i] < v[j]) {
                    merged.add(Pair(u[i], true))
                    i++
                } else if (u[i] > v[j]) {
                    merged.add(Pair(v[j], false))
                    j++
                } else {
                    merged.add(Pair(u[i], true))
                    merged.add(Pair(v[j], false))
                    i++; j++
                }
            }

            while (i < u.size) {
                merged.add(Pair(u[i], true))
                i++
            }

            while (j < v.size) {
                merged.add(Pair(v[j], false))
                j++
            }

            return merged
        }

        val merged = merge(u, v)
        val xs = (0 until Math.max(u.max()!!, v.max()!!))
        val ys = ArrayList<Int>()

        var numCurrentViewers = 0
        for (pair in merged) {
            while (ys.size < pair.first) {
                ys.add(numCurrentViewers)
            }
            numCurrentViewers += if (pair.second) 1 else -1
        }

        val barChartData = ArrayList<Entry>()
        for (pair in xs.zip(ys)) {
            barChartData.add(Entry(1f * pair.first, 1f * pair.second, resources.getDrawable(R.drawable.circle)))
        }

        return barChartData.toMutableList()
    }
}