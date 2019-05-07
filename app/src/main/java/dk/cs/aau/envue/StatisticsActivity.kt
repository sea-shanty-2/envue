package dk.cs.aau.envue

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import kotlinx.android.synthetic.main.activity_statistics.*
import kotlin.random.Random


class StatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val keys = intent.extras?.keySet()
        val extrasFromKeys = keys?.map { k -> intent.extras?.get(k) }

        val joined = (intent.extras?.get("joinedTimestamps") as Array<Int>).apply {sort()}
        val left = (intent.extras?.get("leftTimestamps") as Array<Int>).apply {sort()}

        val chart = findViewById<View>(R.id.chart) as LineChart

        if (!joined.isEmpty() || !left.isEmpty()) {
            val dataSet = LineDataSet(generateChartData(
                joined.map { i -> i - joined.min()!! }.toTypedArray(),
                left.map { i -> i - joined.min()!! }.toTypedArray()),
                "Viewer numbers")

            chart.setDrawGridBackground(false)
            chart.data = LineData(dataSet.apply {
                setDrawFilled(true)
                setDrawCircleHole(false)
                setDrawCircles(false)
                lineWidth = 0f
                fillDrawable = ContextCompat.getDrawable(this@StatisticsActivity, R.drawable.fade_green)
            })
            chart.invalidate()  // Refresh
        }
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
            barChartData.add(Entry(1f * pair.first, 1f * pair.second))
        }

        return barChartData.toMutableList()
    }
}
