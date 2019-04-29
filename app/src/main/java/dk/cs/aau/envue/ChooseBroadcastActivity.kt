package dk.cs.aau.envue

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.Button
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.workers.ChooseBroadcastListAdapter

class ChooseBroadcastActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_broadcast)
        loadBroadcasts()

        findViewById<Button>(R.id.watch_broadcast_btn).setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }
    }

    private fun loadBroadcasts() {

        // Launch the query
        // TODO: We should have an EventsQuery that fetches broadcast clusters, not the broadcasts themselves
        val broadcastsQuery = BroadcastsQuery.builder().build()
        GatewayClient.query(broadcastsQuery).enqueue(object: ApolloCall.Callback<BroadcastsQuery.Data>() {

            // Load the broadcasts into the recycler view when received
            override fun onResponse(response: Response<BroadcastsQuery.Data>) {
                val data = response.data()
                if (data == null) {
                    Log.e("LoadBroadcast", "Could not load active broadcasts.")
                }

                val broadcasts = data?.broadcasts()?.active()?.items()?.toTypedArray()
                if (broadcasts != null) {
                    // Populate the recycler view with the broadcasts (should be events)
                    initializeRecyclerView(broadcasts)
                }
            }

            override fun onFailure(e: ApolloException) {
                Log.d("LOAD_FAILURE", e.toString())
            }
        })
    }

    /**
     * Populates the RecyclerView with the broadcasts (should be events, see the to-do) **/
    private fun initializeRecyclerView(items: Array<BroadcastsQuery.Item>) {
        runOnUiThread {
            viewManager = LinearLayoutManager(this)
            viewAdapter = ChooseBroadcastListAdapter(items)
            recyclerView = findViewById<RecyclerView>(R.id.choose_broadcast_list_view).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
        }
    }
}
