package dk.cs.aau.envue

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import kotlin.random.Random


class StatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val chart = findViewById<View>(R.id.chart) as BarChart

        var s = 0
        val entries = ArrayList<BarEntry>()
        for (i in 0 until 100) {
            val r = Random.nextFloat()
            if (r > 0.5) {
                s++
            } else {
                if (s > 0) {
                    s--
                }
            }
            entries.add(BarEntry(1f * i, 1f * s))
        }
        val dataSet = BarDataSet(entries.toMutableList(), "The data")
        chart.setDrawGridBackground(false)
        chart.data = BarData(dataSet)
        chart.invalidate()  // Refresh
    }
}
