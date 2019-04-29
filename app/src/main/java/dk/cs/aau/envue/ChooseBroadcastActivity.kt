package dk.cs.aau.envue

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.TextView
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import dk.cs.aau.envue.shared.GatewayClient
import dk.cs.aau.envue.utility.ActiveBroadcast
import dk.cs.aau.envue.workers.ChooseBroadcastListAdapter

class ChooseBroadcastActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_broadcast)
        loadBroadcasts()
    }

    private fun loadBroadcasts() {

        val broadcastsQuery = BroadcastsQuery.builder().build()
        GatewayClient.query(broadcastsQuery).enqueue(object: ApolloCall.Callback<BroadcastsQuery.Data>() {
            override fun onResponse(response: Response<BroadcastsQuery.Data>) {
                val data = response.data()
                if (data == null) {
                    Log.e("LoadBroadcast", "Could not load active broadcasts.")
                }

                val broadcasts = data?.broadcasts()?.active()?.items()?.toTypedArray()
                if (broadcasts != null) {
                    initializeRecyclerView(broadcasts)
                }
            }


            override fun onFailure(e: ApolloException) {
                Log.d("LOAD_FAILURE", e.toString())
            }
        })
    }

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
