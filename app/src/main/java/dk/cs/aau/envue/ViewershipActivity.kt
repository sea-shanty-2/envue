package dk.cs.aau.envue

import android.support.v4.content.ContextCompat
import com.github.mikephil.charting.data.LineDataSet
import android.graphics.Color
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.IFillFormatter
import android.graphics.DashPathEffect
import com.github.mikephil.charting.components.Legend.LegendForm
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.XAxis
import android.os.Bundle
import android.util.Log
import android.view.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.Utils
import dk.cs.aau.envue.utility.BarChartMarker
import dk.cs.aau.envue.utility.ChartBase


class ViewershipActivity : ChartBase(), OnChartValueSelectedListener {

    private var chart: LineChart? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_viewership, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        // // Chart Style // //
        chart = view?.findViewById(R.id.chart1)

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
        val mv = BarChartMarker(context!!, R.layout.marker_barchart)

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

        val keys = activity?.intent?.extras?.keySet()

        //val joined = (intent.extras?.get("joinedTimestamps") as Array<Int>).apply {sort()}
        //val left = (intent.extras?.get("leftTimestamps") as Array<Int>).apply {sort()}

        val joined = arrayOf(12340, 12345, 12350)
        val left = arrayOf(12360, 12380, 12382)

        // add data

        val maxY = setData(joined.map { i -> i - joined.min()!! }.toTypedArray(),
            left.map { i -> i - joined.min()!! }.toTypedArray())


        val xAxis: XAxis
        run {
            // // X-Axis Style // //
            xAxis = chart!!.xAxis

            // vertical grid lines
            //xAxis.enableGridDashedLine(10f, 10f, 0f)
            xAxis.isEnabled = false
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
            yAxis.axisMaximum = maxY!! + 1
            yAxis.axisMinimum = 0f
            yAxis.labelCount = maxY.toInt()
        }

        chart!!.axisRight.isEnabled = false


        // draw points over time
        chart!!.animateX(1500)

        // get the legend (only possible after setting data)
        val l = chart!!.legend

        // draw legend entries as lines
        l.form = LegendForm.LINE

        super.onActivityCreated(savedInstanceState)
    }


    private fun setData(u: Array<Int>, v: Array<Int>): Float? {
        val values = generateChartData(u, v)
        val maxY = values.map { i -> i.y }.max()

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
                val drawable = ContextCompat.getDrawable(context!!, R.drawable.fade_green)
                set1.fillDrawable = drawable
            } else {
                set1.fillColor = Color.BLACK
            }

            val dataSets = ArrayList<LineDataSet>()
            dataSets.add(set1) // add the data sets

            // create a data object with the data sets
            val data = LineData(dataSets.toList()).apply { setDrawValues(false) }

            // set data
            chart!!.data = data
            chart!!.background = resources.getDrawable(android.R.color.transparent)
        }

        return maxY
    }

    override fun saveToGallery() {
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