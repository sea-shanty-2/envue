package dk.cs.aau.envue

import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.utils.Utils
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.utility.BarChartMarker
import dk.cs.aau.envue.utility.calculateScoreFromViewTime
import java.text.SimpleDateFormat
import java.util.*

class LeaderboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_leaderboard, container, false)

        val query: LeaderboardQuery = LeaderboardQuery.builder().build()

        GatewayClient.query(query).enqueue(object: ApolloCall.Callback<LeaderboardQuery.Data>() {
            override fun onResponse(response: Response<LeaderboardQuery.Data>) {
                Log.d("LEADERBOARD", "Response ")
                val me = response.data()?.accounts()?.me() ?: return

                val rank = me.rank() ?: 0
                val total = me.score()
                val percentile = me.percentile() ?: 0.0
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

                val scores: List<Pair<Date, Int>>? = me.broadcasts()?.items()?.map {

                    val join = it.joinedTimeStamps()?.map { x -> Pair(x.id(), x.time())  }?.toMutableList()
                        ?: mutableListOf()
                    val left = it.leftTimeStamps()?.map { x -> Pair(x.id(), x.time())  }?.toMutableList()
                        ?: mutableListOf()

                    val date = dateFormat.parse(it.activity() as String)
                    Pair(date, calculateScoreFromViewTime(join, left))
                }

                activity?.runOnUiThread {
                    setFields(rank, total, percentile, scores)
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.d("LEADERBOARD", "Something went wrong while fetching leaderboard: $e")
            }
        })

        return v
    }

    fun setFields(rank: Int?, total_score: Int?, percentile: Double?, scores: List<Pair<Date, Int>>?) {
        val rankView = view?.findViewById<TextView>(R.id.rank)
        val scoreView = view?.findViewById<TextView>(R.id.total_score)
        val percentileView = view?.findViewById<TextView>(R.id.percentile)

        rankView?.text = rank?.toString() ?: "Data not found"
        scoreView?.text = total_score?.toString() ?: "Data not found"
        percentileView?.text = percentile?.toString() ?: "Data not found"

        // If null or empty keep chart unchanged
        if (scores?.isEmpty() != false) return

        var index = 0f
        val chart = view?.findViewById<View>(R.id.leaderboard_chart) as LineChart

        val entries = scores.sortedBy { it.first }
                                        .map {
                                            val e = Entry(index, it.second.toFloat())
                                            index += 1
                                            e
                                        }

        val dataSet = LineDataSet(entries, "Score for the last 30 broadcasts")

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
        dataSet.fillFormatter = IFillFormatter { dataSet, dataProvider -> chart.axisLeft.axisMinimum }
        dataSet.setDrawCircles(false)
        dataSet.setDrawCircleHole(false)

        // Set color of filled area
        if (Utils.getSDKInt() >= 18) {
            // Drawables only supported on api level 18 and above
            val drawable = ContextCompat.getDrawable(context!!, R.drawable.fade_blue)
            dataSet.fillDrawable = drawable
        } else {
            dataSet.fillColor = Color.BLACK
        }

        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
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

            axisLeft.axisMinimum = 0f
            axisRight.setDrawGridLines(false)
            axisRight.setDrawLabels(false)
        }

        chart.data = LineData(dataSet)

        chart.background = resources.getDrawable(android.R.color.transparent)


        chart.invalidate()

    }



}