package dk.cs.aau.envue

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.activity_statistics.*
import kotlin.random.Random
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.model.GradientColor
import dk.cs.aau.envue.utility.BarChartMarker


class StatisticsActivity : AppCompatActivity(), OnChartValueSelectedListener {
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        Log.i("Entry selected", e.toString());
        Log.i("LOW HIGH", "low: " + chart.getLowestVisibleX() + ", high: " + chart.getHighestVisibleX());
        Log.i("MIN MAX", "xMin: " + chart.getXChartMin() + ", xMax: " + chart.getXChartMax() + ", yMin: " + chart.getYChartMin() + ", yMax: " + chart.getYChartMax());
    }

    override fun onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val keys = intent.extras?.keySet()
        val extrasFromKeys = keys?.map { k -> intent.extras?.get(k) }

        //val joined = (intent.extras?.get("joinedTimestamps") as Array<Int>).apply {sort()}
        //val left = (intent.extras?.get("leftTimestamps") as Array<Int>).apply {sort()}

        val joined = arrayOf(12340, 12345, 12350)
        val left = arrayOf(12360, 12380, 12382)


        if (!joined.isEmpty() || !left.isEmpty()) {
            val dataSet = BarDataSet(generateChartData(
                joined.map { i -> i - joined.min()!! }.toTypedArray(),
                left.map { i -> i - joined.min()!! }.toTypedArray()),
                "Viewer numbers").apply {
                setDrawValues(false)
            }

            runOnUiThread {
                val chart = (findViewById<View>(R.id.chart) as BarChart).apply {
                    // set listeners
                    setOnChartValueSelectedListener(this@StatisticsActivity)

                    setDrawGridBackground(false)
                    setDrawBorders(false)
                    setDrawMarkers(true)
                    xAxis.isEnabled = false
                    axisRight.isEnabled = false
                    axisLeft.setDrawZeroLine(true)

                    marker = BarChartMarker(
                        this@StatisticsActivity, R.layout.marker_barchart).apply {
                        chartView = chart
                    }


                    // Force y-labels to be integers
                    axisLeft.apply {
//                        valueFormatter = object : ValueFormatter() {
//                            override fun getFormattedValue(value: Float): String {
//                                return Math.floor(value.toDouble()).toInt().toString()
//                            }
//                        }
                        labelCount = 3
                    }
                }


                chart.data = BarData(dataSet.apply {
                    setGradientColor(android.R.color.transparent, R.color.envue_neutral)
                })
                chart.invalidate()  // Refresh
            }
        }
    }

    private fun generateChartData(u: Array<Int>, v: Array<Int>): MutableList<BarEntry> {
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

        val barChartData = ArrayList<BarEntry>()
        for (pair in xs.zip(ys)) {
            barChartData.add(BarEntry(1f * pair.first, 1f * pair.second))
        }

        return barChartData.toMutableList()
    }
}
