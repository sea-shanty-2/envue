package dk.cs.aau.envue

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.view.View
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.facebook.login.LoginManager
import com.google.gson.GsonBuilder
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.utility.EmojiIcon
import kotlinx.android.synthetic.main.activity_profile.*
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.utils.Utils
import dk.cs.aau.envue.shared.FormatDate
import dk.cs.aau.envue.type.AccountUpdateInputType
import dk.cs.aau.envue.utility.BarChartMarker
import java.util.*


class ProfileFragment : Fragment() {
    companion object {
        internal const val SET_INTERESTS_REQUEST = 0
        internal val TAG = ProfileFragment::class.java.simpleName ?: "ProfileFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Register swipe to refresh
        view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)?.setOnRefreshListener {
            fetchLeaderboard()
        }

        // Register button listeners
        view.findViewById<ImageView>(R.id.logOutButton)?.setOnClickListener { this.logOut() }
        view.findViewById<TextView>(R.id.profileNameView)?.setOnClickListener { this.openDialog() }
        view.findViewById<TextView>(R.id.interestsButton)?.setOnClickListener { this.onChangeInterests() }

        return view
    }

    override fun onStart() {
        super.onStart()

        fetchProfile()
        fetchLeaderboard()
    }

    fun setLeaderboardFields(rank: Int?, total_score: Int?, percentile: Double?, scores: List<Pair<Date, Int>>?) {
        val rankView = view?.findViewById<TextView>(R.id.rank)
        val scoreView = view?.findViewById<TextView>(R.id.total_score)
        val percentileView = view?.findViewById<TextView>(R.id.percentile)

        rankView?.text = rank?.let { "#$it" } ?: getString(R.string.dots)
        scoreView?.text = total_score?.let {
            if (it < 1000) "$it" else "${String.format("%.1f", it / 1000f)}k"
        } ?: getString(R.string.dots)
        percentileView?.text = percentile?.let {"${String.format("%.1f", it)}%"} ?: getString(R.string.dots)

        // If null or empty keep chart unchanged
        if (scores?.isEmpty() != false) return

        var index = 0f
        val chart = view?.findViewById<View>(R.id.leaderboard_chart) as LineChart

        val entries = scores.sortedByDescending { it.first }.map {
            val e = Entry(index, it.second.toFloat())
            index += 1
            e
        }

        val dataSet = LineDataSet(entries, "Score for the last 10 broadcasts").apply {
            setDrawIcons(false)

            // Draw dashed line
            enableDashedLine(10f, 0f, 0f)

            // Black lines and points
            color = Color.BLACK
            setCircleColor(Color.BLACK)

            // Line thickness and point size
            lineWidth = 1f
            circleRadius = 3f

            // Draw points as solid circles
            setDrawCircleHole(false)

            // Customize legend entry
            formLineWidth = 1f
            formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            formSize = 15f

            setDrawValues(false)

            mode = LineDataSet.Mode.CUBIC_BEZIER

            // Draw selection line as dashed
            enableDashedHighlightLine(10f, 5f, 0f)

            // Set the filled area
            setDrawFilled(true)
            fillFormatter = IFillFormatter { _, _ -> chart.axisLeft.axisMinimum }
            setDrawCircles(false)
            setDrawCircleHole(false)
        }


        // Set color of filled area
        if (Utils.getSDKInt() >= 18) {
            // Drawables only supported on api level 18 and above
            val drawable = ContextCompat.getDrawable(context!!, R.drawable.fade_blue)
            dataSet.fillDrawable = drawable
        } else {
            dataSet.fillColor = Color.BLACK
        }

        // Get max
        val yMax = scores.maxBy { it.second }?.second ?: 0
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)

            // create marker to display box when values are selected
            val mv = BarChartMarker(context, R.layout.marker_barchart)

            // Set the marker to the chart
            mv.chartView = chart
            marker = mv

            // enable scaling and dragging
            isDragEnabled = true
            setScaleEnabled(true)

            // force pinch zoom along both axis
            setPinchZoom(true)

            // Axis settings
            xAxis.isEnabled = false

            axisLeft.apply {
                // Horizontal grid lines
                enableAxisLineDashedLine(10f, 10f, 0f)

                axisMinimum = 0f
                axisMaximum = yMax + 1f
                labelCount = Math.min(yMax, 5)
            }

            axisRight.apply {
                setDrawGridLines(false)
                setDrawLabels(false)
                isEnabled = false
            }

        }

        chart.data = LineData(dataSet)

        // Get the legend (only possible after setting data)
        val legend = chart.legend

        // Draw legend entries as lines
        legend.form = Legend.LegendForm.LINE

        chart.background = resources.getDrawable(android.R.color.transparent)
        chart.invalidate()
    }

    private fun fetchLeaderboard() {
        val query: LeaderboardQuery = LeaderboardQuery.builder().build()
        GatewayClient.query(query).enqueue(object: ApolloCall.Callback<LeaderboardQuery.Data>() {
            override fun onResponse(response: Response<LeaderboardQuery.Data>) {
                Log.d("LEADERBOARD", "Response ")
                val me = response.data()?.accounts()?.me() ?: return

                val rank = me.rank() ?: 0
                val total = me.score()
                val percentile = me.percentile() ?: 0.0

                val scores: List<Pair<Date, Int>>? = me.broadcasts()?.items()?.map {
                    Pair(FormatDate(it.activity() as String), it.score())
                }

                activity?.runOnUiThread {
                    setLeaderboardFields(rank, total, percentile, scores)
                    view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)?.isRefreshing = false
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.d("LEADERBOARD", "Something went wrong while fetching leaderboard: $e")
                view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)?.isRefreshing = false
            }
        })
    }

    private fun fetchProfile() {
        val profileQuery = ProfileQuery.builder().build()

        GatewayClient.query(profileQuery).enqueue(object : ApolloCall.Callback<ProfileQuery.Data>() {
            override fun onResponse(response: Response<ProfileQuery.Data>) {
                response.data()?.accounts()?.me()?.let { onProfileFetch(it) }
            }

            override fun onFailure(e: ApolloException) = onProfileFetchFailure(e)
        })
    }

    private fun onProfileFetch(profile: ProfileQuery.Me) {
        activity?.runOnUiThread {
            profile.categories()?.let { oneHotVectorToEmoji(it) }

            view?.findViewById<TextView>(R.id.profileNameView)?.text = profile.displayName()
        }
    }

    private fun onProfileFetchFailure(e: ApolloException) {
        activity?.run {
            runOnUiThread {
                AlertDialog
                    .Builder(this)
                    .setTitle(e.message)
                    .setMessage(
                        "There was an issue fetching your profile data." +
                                "Check your internet connection or you can try relogging.")
                    .setNegativeButton("log out") { _, _ ->  logOut() }
                    .setPositiveButton("return") { _, _ -> activity?.finish() }
                    .create()
                    .show()
            }
        }
    }

    private fun logOut() {
        LoginManager.getInstance().logOut()
        startActivity(Intent(this.activity, LoginActivity::class.java))
    }

    private fun onChangeInterests() {
        val curInt: CharSequence = view?.findViewById<TextView>(R.id.interests)?.text ?: ""
        val intent = Intent(this.activity, InterestsActivity::class.java)
        intent.putExtra(resources.getString(R.string.current_interests_key), curInt)
        startActivityForResult(intent, SET_INTERESTS_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SET_INTERESTS_REQUEST ->
                if (resultCode == Activity.RESULT_OK) {
                    view?.findViewById<TextView>(R.id.interests)?.text = data?.getStringExtra(resources.getString(R.string.interests_response_key))
                }
        }
    }

    private fun oneHotVectorToEmoji(categories: List<Double>) {
        var allEmojis = ArrayList<EmojiIcon>()
        allEmojis = allEmojis.plus(
            GsonBuilder().create().fromJson(
                resources.openRawResource(R.raw.limited_emojis).bufferedReader(),
                Array<EmojiIcon>::class.java
            )
        ) as ArrayList


        var concatenated = ""
        for (i in categories.indices) {
            if (categories[i] == 1.0) {
                concatenated += allEmojis[i].char
            }
        }

        view?.findViewById<TextView>(R.id.interests)?.text = if (concatenated.isEmpty()) getString(R.string.none) else concatenated
    }

    private fun openDialog() {
        val input = EditText(this.activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(profileNameView?.text?.toString())
        }

        context?.let {
            AlertDialog.Builder(it).apply {
                setTitle("Change display name")
                setView(input)
                setPositiveButton("OK") { _, _ -> acceptDialog(input)}
                setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

                show()
            }
        }
    }

    private fun acceptDialog(input : EditText) {
        val temp = AccountUpdateInputType.builder().displayName(input.text.toString()).build()
        val changeDisplayName = ProfileUpdateMutation.builder().account(temp).build()

        GatewayClient.mutate(changeDisplayName).enqueue(object: ApolloCall.Callback<ProfileUpdateMutation.Data>(){
            override fun onFailure(e: ApolloException) {
            }

            override fun onResponse(response: Response<ProfileUpdateMutation.Data>) {
                activity?.runOnUiThread{
                    view?.findViewById<TextView>(R.id.profileNameView)?.text = input.text.toString()
                }
            }
        })
    }

}