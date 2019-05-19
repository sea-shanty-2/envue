package dk.cs.aau.envue

import android.support.v4.content.ContextCompat
import com.github.mikephil.charting.data.LineDataSet
import android.graphics.Color
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.IFillFormatter
import android.graphics.DashPathEffect
import com.github.mikephil.charting.components.Legend.LegendForm
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


class ViewershipFragment : ChartBase(), OnChartValueSelectedListener {

    private var chart: LineChart? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_viewership, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        // // Chart Style // //
        chart = view?.findViewById(R.id.chart1)!!

        chart!!.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setOnChartValueSelectedListener(this@ViewershipFragment)
            setDrawGridBackground(false)

            // create marker to display box when values are selected
            val mv = BarChartMarker(context!!, R.layout.marker_barchart)

            // Set the marker to the chart
            mv.setChartView(chart)
            marker = mv

            // enable scaling and dragging
            isDragEnabled = true
            setScaleEnabled(true)

            // force pinch zoom along both axis
            setPinchZoom(true)
        }

        // Load data from Bundle
        val joined = (activity?.intent?.extras?.get("joinedTimestamps") as Array<Int>).apply {sort()}
        val left = (activity?.intent?.extras?.get("leftTimestamps") as Array<Int>).apply {sort()}

        //val joined = arrayOf(12340, 12345, 12350)
        //val left = arrayOf(12360, 12380, 12382)

        val maxY = setData(
            joined.map { i -> i - joined.min()!! }.toTypedArray(),
            left.map { i -> i - joined.min()!! }.toTypedArray())


        chart!!.xAxis.apply {
            isEnabled = false
        }

        chart!!.axisLeft.apply {
            // Horizontal grid lines
            enableAxisLineDashedLine(10f, 10f, 0f)

            // Axis range
            axisMaximum = maxY!! + 1
            axisMinimum = 0f
            labelCount = maxY.toInt()
        }

        chart!!.axisRight.apply {
            isEnabled = false
        }

        // Draw points over time
        chart!!.animateX(1500)

        // Get the legend (only possible after setting data)
        val legend = chart!!.legend

        // Draw legend entries as lines
        legend.form = LegendForm.LINE

        super.onActivityCreated(savedInstanceState)
    }

    private fun setData(u: Array<Int>, v: Array<Int>): Float? {
        val values = generateChartData(u, v)
        val maxY = values.map { i -> i.y }.max() ?: 0f

        val dataSet: LineDataSet

        if (chart!!.data != null && chart!!.data.dataSetCount > 0) {
            dataSet = chart!!.data.getDataSetByIndex(0) as LineDataSet
            dataSet.values = values
            dataSet.notifyDataSetChanged()
            chart!!.data.notifyDataChanged()
            chart!!.notifyDataSetChanged()
        } else {
            // Create a dataset and give it a type
            dataSet = LineDataSet(values, "Total number of viewers")

            dataSet.setDrawIcons(false)

            // Draw dashed line
            dataSet.enableDashedLine(10f, 5f, 0f)

            // Black lines and points
            dataSet.color = Color.BLACK
            dataSet.setCircleColor(Color.BLACK)

            // Line thickness and point size
            dataSet.lineWidth = 1f
            dataSet.circleRadius = 3f

            // Draw points as solid circles
            dataSet.setDrawCircleHole(false)

            // Customize legend entry
            dataSet.formLineWidth = 1f
            dataSet.formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            dataSet.formSize = 15f

            // Text size of values
            dataSet.valueTextSize = 9f

            // Draw selection line as dashed
            dataSet.enableDashedHighlightLine(10f, 5f, 0f)

            // Set the filled area
            dataSet.setDrawFilled(true)
            dataSet.fillFormatter = IFillFormatter { _, _ -> chart!!.axisLeft.axisMinimum }
            dataSet.setDrawCircles(false)
            dataSet.setDrawCircleHole(false)

            // Set color of filled area
            if (Utils.getSDKInt() >= 18) {
                // Drawables only supported on api level 18 and above
                val drawable = ContextCompat.getDrawable(context!!, R.drawable.fade_green)
                dataSet.fillDrawable = drawable
            } else {
                dataSet.fillColor = Color.BLACK
            }

            val dataSets = ArrayList<LineDataSet>()
            dataSets.add(dataSet) // Add the data set(s)

            // Create a data object with the data sets
            val data = LineData(dataSets.toList()).apply { setDrawValues(false) }

            // Set data
            chart?.apply {
                this.data = data
                this.background = resources.getDrawable(android.R.color.transparent)
            }
        }

        return maxY
    }

    override fun saveToGallery() {
        chart?.run { saveToGallery(this, "LineChartActivityI") }
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

    /** Takes two sorted lists of integers (joined- and left timestamps, respectively)
     * and generates a line chart data set of viewer numbers. */
    private fun generateChartData(u: Array<Int>, v: Array<Int>): MutableList<Entry> {

        // Helper function for merging the sorted lists into a single labelled
        // list that can be traversed linearly while incrementing/decrementing
        // viewer numbers.
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
        val xs = (0 until Math.max(u.max() ?: 0, v.max() ?: 0))
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