package dk.cs.aau.envue

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.utility.calculateScoreFromViewTime
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LeaderboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_leaderboard, container, false)

        val leaderboardQuery: LeaderboardQuery = LeaderboardQuery.builder().build()
        val a = GatewayClient.query(leaderboardQuery).enqueue(object: ApolloCall.Callback<LeaderboardQuery.Data>() {
            override fun onResponse(response: Response<LeaderboardQuery.Data>) {
                val me = response.data()?.accounts()?.me()
                val rank = me?.rank()
                val total_score = me?.score()
                val percentile = me?.percentile()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

                val scores: List<Pair<Date, Int>>? = me?.broadcasts()?.items()?.map {
                    val join = it.joinedTimeStamps()?.map { x -> Pair(x.id(), x.time())  }?.toMutableList()
                    val left = it.leftTimeStamps()?.map { x -> Pair(x.id(), x.time())  }?.toMutableList()
                    val date = dateFormat.parse(it.activity() as String)
                    if (join != null && left != null){
                        Pair(date, calculateScoreFromViewTime(join, left))
                    }
                    else{
                        return
                    }
                }

                activity?.runOnUiThread {
                    setFields(rank, total_score, percentile, scores)
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.d("LEADERBOARD", "Something went wrong while fetching leaderboard: $e")
            }
        })

        return v
    }

    fun setFields(rank: Int?, total_score: Int?, percentile: Double?, scores: List<Pair<Date, Int>>?) {
        view?.findViewById<TextView>(R.id.rank)?.text = rank.toString()
        view?.findViewById<TextView>(R.id.total_score)?.text = total_score.toString()
        view?.findViewById<TextView>(R.id.percentile)?.text = percentile.toString()

        val chart = view?.findViewById<View>(R.id.leaderboard_chart) as LineChart
        val entries = scores?.map { Entry(it.first.time.toFloat(), it.second.toFloat()) }

        if (percentile != 0.0) {
            val data = LineDataSet(entries, "Score for the last 30 broadcasts")

            chart.axisLeft.axisMinimum = 0f
            chart.axisRight.setDrawGridLines(false)
            chart.axisRight.setDrawLabels(false)
            chart.data = LineData(data)
            chart.invalidate()
        }

    }



}