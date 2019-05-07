package dk.cs.aau.envue

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import kotlin.random.Random


class StatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val keys = intent.extras?.keySet()
        val extrasFromKeys = keys?.map { k -> intent.extras?.get(k) }

        val joined = intent.extras?.get("joinedTimestamps") as Array<Int>
        val left = intent.extras?.get("leftTimestamps") as Array<Int>


        val chart = findViewById<View>(R.id.chart) as BarChart

        val dataSet = BarDataSet(generateChartData(joined, left), "The data")
        chart.setDrawGridBackground(false)
        chart.data = BarData(dataSet)
        chart.invalidate()  // Refresh

    }

    private fun generateChartData(u: Array<Int>, v: Array<Int>): MutableList<BarEntry> {
        val chartData = ArrayList<Int>()
        var i=0; var j=0; var numCurrentViewers=0

        // Increment the num viewers when finding a smaller joined
        // timestamp (from vector u) and decrement in left time stamps
        // (vector v).
        while (i < u.size && j < v.size) {
            when {
                u[i] < v[j] -> {
                    numCurrentViewers++; i++
                    chartData.add(numCurrentViewers)
                }
                u[i] > v[j] -> {
                    numCurrentViewers--; j++
                    chartData.add(numCurrentViewers)
                }
                else -> {
                    i++; j++
                    chartData.add(numCurrentViewers)
                }
            }
        }

        // Now either u or v is done, so process the rest of the lists
        while (i < u.size) {
            numCurrentViewers++; i++
            chartData.add(numCurrentViewers)
        }

        while (j < v.size) {
            numCurrentViewers--; j++
            chartData.add(numCurrentViewers)
        }

        Log.d("BROADIDTIMES", "Merged: $chartData")

        // Convert to BarChart entries
        val barChartEntries = (0 until chartData.size).map { i -> BarEntry(i * 1f, chartData[i] * 1f) }

        return barChartEntries.toMutableList()
    }
}
